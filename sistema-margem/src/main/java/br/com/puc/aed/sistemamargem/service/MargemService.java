package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MargemService {

    private static final Logger log = LoggerFactory.getLogger(MargemService.class);
    private static final List<Integer> VERBAS_DEBITO = List.of(600, 700, 800, 900);
    private static final String VERSAO_CLOUDEVENTS = "1.0";
    private static final String ORIGEM = "sistema-margem";

    private final MargemRepository margemRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final KafkaTemplate<String, MargemRecusadaEvent> margemRecusadaEventTemplate;
    private final KafkaTemplate<String, MargemReservadaEvent> margemReservadaEventTemplate;

    @Value("${sistema-margem.topico.margem-recusada}")
    private String margemRecusadaTopic;
    @Value("${sistema-margem.topico.margem-reservada}")
    private String margemReservadaTopic;

    public MargemService(MargemRepository margemRepository, EventoProcessadoRepository eventoProcessadoRepository,
                         KafkaTemplate<String, MargemRecusadaEvent> margemRecusadaEventTemplate,
                         KafkaTemplate<String, MargemReservadaEvent> margemReservadaEventTemplate) {
        this.margemRepository = margemRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.margemRecusadaEventTemplate = margemRecusadaEventTemplate;
        this.margemReservadaEventTemplate = margemReservadaEventTemplate;
    }

    @Transactional
    public void processarSolicitacaoEmprestimo(String eventoId, EmprestimoSolicitadoEvent event) {
        boolean primeiraVez = eventoProcessadoRepository.registrarSeNovo(eventoId);
        if (!primeiraVez) {
            log.warn("Evento {} já processado, descartando em silêncio", eventoId);
            return;
        }

        log.info("Processando evento={} para o cliente={}", eventoId, event.cpf());
        var tipo = VERBAS_DEBITO.contains(event.codigoVerba()) ? Margem.Tipo.DEBITO : Margem.Tipo.CREDITO;
        var margem = new Margem(
                UUID.randomUUID(),
                event.cpf(),
                event.valorParcela(),
                event.codigoVerba(),
                Instant.now(),
                tipo
        );

        var valorMargemAtual = margemRepository.margemAtual(event.cpf()).orElse(BigDecimal.ZERO);
        var margemRestante = valorMargemAtual.add(margem.getValor());

        if (margemRestante.compareTo(BigDecimal.ZERO) < 0) {
            gerarMargemRecusadaEvent(event.cpf(), event.emprestimoId());
            return;
        }

        margemRepository.salvar(margem);
        gerarMargemReservadaEvent(event.cpf(), event.emprestimoId());
    }

    private void gerarMargemRecusadaEvent(String cpf, String emprestimoId) {
        var time = Instant.now();
        var novoEventoId = UUID.randomUUID().toString();
        var event = new MargemRecusadaEvent(
                cpf,
                emprestimoId,
                "Margem insuficiente"
        );

        ProducerRecord<String, MargemRecusadaEvent> recusadaEventProducerRecord = new ProducerRecord<>(
                margemRecusadaTopic,
                cpf,
                event
        );

        recusadaEventProducerRecord.headers().add("ce_specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce_source", ORIGEM.getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce_time", time.toString().getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce_type", "margem.recusada.v1".getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce_id", novoEventoId.getBytes(StandardCharsets.UTF_8));

        publicar(margemRecusadaEventTemplate, recusadaEventProducerRecord, "margem.recusada.v1");
    }

    private void gerarMargemReservadaEvent(String cpf, String emprestimoId) {
        var time = Instant.now();
        var novoEventoId = UUID.randomUUID().toString();
        var event = new MargemReservadaEvent(cpf, emprestimoId);

        ProducerRecord<String, MargemReservadaEvent> reservadaEventProducerRecord = new ProducerRecord<>(
                margemReservadaTopic,
                cpf,
                event
        );

        reservadaEventProducerRecord.headers().add("ce_specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        reservadaEventProducerRecord.headers().add("ce_source", ORIGEM.getBytes(StandardCharsets.UTF_8));
        reservadaEventProducerRecord.headers().add("ce_time", time.toString().getBytes(StandardCharsets.UTF_8));
        reservadaEventProducerRecord.headers().add("ce_type", "margem.reservada.v1".getBytes(StandardCharsets.UTF_8));
        reservadaEventProducerRecord.headers().add("ce_id", novoEventoId.getBytes(StandardCharsets.UTF_8));

        publicar(margemReservadaEventTemplate, reservadaEventProducerRecord, "margem.reservada.v1");
    }



    private <T> void publicar(KafkaTemplate<String, T> template,
                              ProducerRecord<String, T> registro,
                              String tipo) {
        template.send(registro).whenComplete((resultado, erro) -> {
            if (erro != null) {
                log.error("Falha ao publicar {}: {}", tipo, erro.getMessage(), erro);
                return;
            }
            var meta = resultado.getRecordMetadata();
            log.info("{} publicado em {}-{}@{}", tipo, meta.topic(), meta.partition(), meta.offset());
        });
    }

}
