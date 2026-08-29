package br.com.puc.aed.sistemaanalise.service;

import br.com.puc.aed.sistemaanalise.domain.Analise;
import br.com.puc.aed.sistemaanalise.domain.AnaliseAprovadaEvent;
import br.com.puc.aed.sistemaanalise.domain.AnaliseReprovadaEvent;
import br.com.puc.aed.sistemaanalise.domain.MargemReservadaEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);
    private static final String VERSAO_CLOUDEVENTS = "1.0";
    private static final String ORIGEM = "sistema-analise";

    private final AnaliseRepository analiseRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final KafkaTemplate<String, AnaliseAprovadaEvent> analiseAprovadaEventTemplate;
    private final KafkaTemplate<String, AnaliseReprovadaEvent> analiseReprovadaEventTemplate;

    @Value("${sistema-analise.topico.analise-aprovada}")
    private String analiseAprovadaTopic;
    @Value("${sistema-analise.topico.analise-reprovada}")
    private String analiseReprovadaTopic;

    public AnaliseService(AnaliseRepository analiseRepository,
                          EventoProcessadoRepository eventoProcessadoRepository,
                          KafkaTemplate<String, AnaliseAprovadaEvent> analiseAprovadaEventTemplate,
                          KafkaTemplate<String, AnaliseReprovadaEvent> analiseReprovadaEventTemplate) {
        this.analiseRepository = analiseRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.analiseAprovadaEventTemplate = analiseAprovadaEventTemplate;
        this.analiseReprovadaEventTemplate = analiseReprovadaEventTemplate;
    }

    @Transactional
    public void avaliar(String eventoId, MargemReservadaEvent event) {
        boolean primeiraVez = eventoProcessadoRepository.registrarSeNovo(eventoId);
        if (!primeiraVez) {
            log.warn("Evento {} já processado, descartando em silêncio", eventoId);
            return;
        }

        log.info("Avaliando crédito para evento={} cliente={}", eventoId, event.cpf());
        var analise = Analise.decidir(event.cpf(), event.solicitacaoId());
        analiseRepository.salvar(analise);

        if (analise.isAprovada()) {
            publicarAprovada(event.cpf(), event.solicitacaoId());
        } else {
            publicarReprovada(event.cpf(), event.solicitacaoId());
        }
    }

    private void publicarAprovada(String cpf, String solicitacaoId) {
        var time = Instant.now();
        var novoEventoId = UUID.randomUUID().toString();
        var event = new AnaliseAprovadaEvent(cpf, solicitacaoId);

        ProducerRecord<String, AnaliseAprovadaEvent> producerRecord =
                new ProducerRecord<>(analiseAprovadaTopic, cpf, event);

        producerRecord.headers().add("ce-specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-source", ORIGEM.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-time", time.toString().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-type", "analise.aprovada.v1".getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce_id", novoEventoId.getBytes(StandardCharsets.UTF_8));

        analiseAprovadaEventTemplate.send(producerRecord);
    }

    private void publicarReprovada(String cpf, String solicitacaoId) {
        var time = Instant.now();
        var novoEventoId = UUID.randomUUID().toString();
        var event = new AnaliseReprovadaEvent(cpf, solicitacaoId, "Reprovado na análise de crédito");

        ProducerRecord<String, AnaliseReprovadaEvent> producerRecord =
                new ProducerRecord<>(analiseReprovadaTopic, cpf, event);

        producerRecord.headers().add("ce-specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-source", ORIGEM.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-time", time.toString().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-type", "analise.reprovada.v1".getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce_id", novoEventoId.getBytes(StandardCharsets.UTF_8));

        analiseReprovadaEventTemplate.send(producerRecord);
    }
}
