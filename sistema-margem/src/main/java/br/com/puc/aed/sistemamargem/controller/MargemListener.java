package br.com.puc.aed.sistemamargem.controller;

import br.com.puc.aed.sistemamargem.domain.EmprestimoSolicitadoEvent;
import br.com.puc.aed.sistemamargem.service.MargemService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MargemListener {

    private static final String CABECALHO_ID = "ce-id";
    private static final Logger log = LoggerFactory.getLogger(MargemListener.class);

    private final MargemService margemService;

    public MargemListener(MargemService margemService) {
        this.margemService = margemService;
    }

    @KafkaListener(topics = "${sistema-margem.topico.empresitmo-solicitado}", groupId = "sistema-margem")
    public void verificarMargem(ConsumerRecord<String, EmprestimoSolicitadoEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        margemService.processarSolicitacaoEmprestimo(eventoId, consumerRecord.value());
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
