package br.com.puc.aed.sistemamargem.controller;

import br.com.puc.aed.sistemamargem.domain.EmprestimoSolicitadoEvent;
import br.com.puc.aed.sistemamargem.service.MargemService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MargemListener {

    private static final String CABECALHO_ID = "ce_id";
    private static final Logger log = LoggerFactory.getLogger(MargemListener.class);

    private final MargemService margemService;
    private final String cpfBancoIndisponivel;

    public MargemListener(MargemService margemService,
                          @Value("${sistema-margem.simulacao.cpf-banco-indisponivel:}") String cpfBancoIndisponivel) {
        this.margemService = margemService;
        this.cpfBancoIndisponivel = cpfBancoIndisponivel;
    }

    @KafkaListener(topics = "${sistema-margem.topico.emprestimo-solicitado}", groupId = "sistema-margem")
    public void verificarMargem(ConsumerRecord<String, EmprestimoSolicitadoEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        if (eventoId == null || eventoId.isBlank()) {
            log.error("Registro sem {} descartado: particao={} offset={}",
                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
            ack.acknowledge();
            return;
        }
        // Simulacao de banco fora do ar: o erro e transitorio, entao passa por todas as
        // retentativas do error handler e, esgotadas, o registro vai para a DLQ.
        EmprestimoSolicitadoEvent evento = consumerRecord.value();
        if (evento != null && !cpfBancoIndisponivel.isBlank() && cpfBancoIndisponivel.equals(evento.cpf())) {
            log.warn("Simulando banco indisponivel: evento={} cliente={} particao={} offset={}",
                    eventoId, evento.cpf(), consumerRecord.partition(), consumerRecord.offset());
            throw new TransientDataAccessResourceException("Simulacao: banco de dados indisponivel");
        }

        margemService.processarSolicitacaoEmprestimo(eventoId, evento);
        ack.acknowledge();
    }

    private @Nullable String obterId(ConsumerRecord<String, EmprestimoSolicitadoEvent> registro) {
        Header cabecalho = registro.headers().lastHeader(CABECALHO_ID);
        if (cabecalho == null) {
            log.error("Cabecalho {} nao encontrado no registro {}", CABECALHO_ID, registro);
            return null;
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }

}
