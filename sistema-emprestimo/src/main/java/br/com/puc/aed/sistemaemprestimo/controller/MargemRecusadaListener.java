package br.com.puc.aed.sistemaemprestimo.controller;

import br.com.puc.aed.sistemaemprestimo.domain.MargemRecusadaEvent;
import br.com.puc.aed.sistemaemprestimo.service.MargemRecusadaService;
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
public class MargemRecusadaListener {

    private static final String CABECALHO_ID = "ce_id";
    private static final Logger log = LoggerFactory.getLogger(MargemRecusadaListener.class);

    private final MargemRecusadaService margemRecusadaService;

    public MargemRecusadaListener(MargemRecusadaService margemRecusadaService) {
        this.margemRecusadaService = margemRecusadaService;
    }

    @KafkaListener(topics = "${sistema-emprestimo.topicos.margem-recusada}", groupId = "sistema-emprestimo")
    public void registrarMargemRecusada(ConsumerRecord<String, MargemRecusadaEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        if (eventoId == null || eventoId.isBlank()) {
            log.error("Registro de margem recusada sem {} descartado: particao={} offset={}",
                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
            ack.acknowledge();
            return;
        }

        MargemRecusadaEvent evento = consumerRecord.value();
        if (evento != null) {
            margemRecusadaService.processarMargemRecusada(eventoId, evento);
        }
        ack.acknowledge();
    }

    private @Nullable String obterId(ConsumerRecord<String, MargemRecusadaEvent> registro) {
        Header cabecalho = registro.headers().lastHeader(CABECALHO_ID);
        if (cabecalho == null) {
            log.error("Cabecalho {} nao encontrado no registro de margem recusada {}", CABECALHO_ID, registro);
            return null;
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }
}
