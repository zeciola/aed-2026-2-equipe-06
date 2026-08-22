package br.com.puc.aed.sistemaemprestimo.service;

import br.com.puc.aed.sistemaemprestimo.domain.EmprestimoSolicitadoEvent;
import br.com.puc.aed.sistemaemprestimo.domain.SolicitarEmprestimoRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class EmprestimoService {

    private static final Logger log = LoggerFactory.getLogger(EmprestimoService.class);
    private static final String TIPO_DO_EVENTO = "emprestimo.solicitado.v1";
    private static final String ORIGEM = "/emprestimos/solicitar";
    private static final String VERSAO_CLOUDEVENTS = "1.0";

    private final EmprestimoRepository emprestimoRepository;
    private final KafkaTemplate<String, EmprestimoSolicitadoEvent> kafkaTemplate;
    private final String topico;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             KafkaTemplate<String, EmprestimoSolicitadoEvent> kafkaTemplate,
                             @Value("${sistema-emprestimo.topicos.emprestimo-solicitado}") String topico) {
        this.emprestimoRepository = emprestimoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topico = topico;
    }

    @Transactional
    public void solictar(SolicitarEmprestimoRequest request) {
        var emprestimo = emprestimoRepository.save(request.toEmprestimo());

        var event = new EmprestimoSolicitadoEvent(
                emprestimo.getCpf(),
                emprestimo.getValorParcela(),
                emprestimo.getCodigoVerba()
        );

        ProducerRecord<String, EmprestimoSolicitadoEvent> producerRecord = new ProducerRecord<>(
                topico,
                emprestimo.getCpf(),
                event
        );

        producerRecord.headers().add("ce-specversion", VERSAO_CLOUDEVENTS.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-source", ORIGEM.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-time", emprestimo.getDataEmprestimo().toString().getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-type", TIPO_DO_EVENTO.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("ce-id", emprestimo.getId().toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(producerRecord)
                .whenComplete((records, throwable) -> {
                    if (throwable != null) {
                        log.error("Erro ao publicar evento de empréstimo solicitado: {}", throwable.getMessage(), throwable);
                    }

                    log.info("Evento de empréstimo solicitado publicado com sucesso: {}", event);
                });
    }

}
