package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
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

    private final MargemRepository margemRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;
    private final KafkaTemplate<String, MargemReservadaEvent> margemRecusadaEventTemplate;
    private final KafkaTemplate<String, MargemAprovadaEvent> margemAprovadaEventTemplate;

    @Value("${sistema-margem.topico.margem-recusada}")
    private String margemRecusadaTopic;
    @Value("${sistema-margem.topico.margem-reservada}")
    private String margemReservadaTopic;

    public MargemService(MargemRepository margemRepository, EventoProcessadoRepository eventoProcessadoRepository,
                         JdbcAggregateTemplate jdbcAggregateTemplate,
                         KafkaTemplate<String, MargemReservadaEvent> margemRecusadaEventTemplate,
                         KafkaTemplate<String, MargemAprovadaEvent> margemAprovadaEventTemplate) {
        this.margemRepository = margemRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
        this.margemRecusadaEventTemplate = margemRecusadaEventTemplate;
        this.margemAprovadaEventTemplate = margemAprovadaEventTemplate;
    }

    @Transactional
    public void processarSolicitacaoEmprestimo(String eventoId, EmprestimoSolicitadoEvent event) {
        eventoProcessadoRepository.findById(eventoId)
                .ifPresentOrElse(
                        eventoProcessado -> log.warn("Evento {} já processado em {}", eventoId, eventoProcessado.getProcessadoEm()),
                        () -> {
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
                                gerarMargemRecusadaEvent(eventoId, event.cpf());
                                return;
                            }

                            jdbcAggregateTemplate.insert(margem);
                            jdbcAggregateTemplate.insert(new EventoProcessado(eventoId));
                            gerarMargemReservadaEvent(eventoId, event.cpf());
                        }
                );
    }

    private void gerarMargemRecusadaEvent(String eventoId, String cpf) {
        var time = Instant.now();
        var event = new MargemReservadaEvent(
                cpf,
                eventoId,
                "Margem insuficiente"
        );

        ProducerRecord<String, MargemReservadaEvent> recusadaEventProducerRecord = new ProducerRecord<>(
                margemRecusadaTopic,
                cpf,
                event
        );

        recusadaEventProducerRecord.headers().add("ce-specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-time", time.toString().getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-type", "margem.recusada.v1".getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-id", eventoId.getBytes(StandardCharsets.UTF_8));

        margemRecusadaEventTemplate.send(recusadaEventProducerRecord);
    }

    private void gerarMargemReservadaEvent(String eventoId, String cpf) {
        var time = Instant.now();
        var event = new MargemAprovadaEvent(cpf, eventoId);


        ProducerRecord<String, MargemAprovadaEvent> recusadaEventProducerRecord = new ProducerRecord<>(
                margemReservadaTopic,
                cpf,
                event
        );

        recusadaEventProducerRecord.headers().add("ce-specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-time", time.toString().getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-type", "margem.aprovada.v1".getBytes(StandardCharsets.UTF_8));
        recusadaEventProducerRecord.headers().add("ce-id", eventoId.getBytes(StandardCharsets.UTF_8));

        margemAprovadaEventTemplate.send(recusadaEventProducerRecord);
    }


}
