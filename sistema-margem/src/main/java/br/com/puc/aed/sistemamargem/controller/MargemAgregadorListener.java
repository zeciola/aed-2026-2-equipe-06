package br.com.puc.aed.sistemamargem.controller;

import br.com.puc.aed.sistemamargem.domain.MargemReservadaEvent;
import br.com.puc.aed.sistemamargem.service.MargemAgregadorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@Component
public class MargemAgregadorListener {

    private static final Logger log = LoggerFactory.getLogger(MargemAgregadorListener.class);
    private static final String CABECALHO_ID = "ce_id";
    private static final String CABECALHO_TIME = "ce_time";

    private final MargemAgregadorService margemAgregadorService;

    public MargemAgregadorListener(MargemAgregadorService margemAgregadorService) {
        this.margemAgregadorService = margemAgregadorService;
    }

    @KafkaListener(
            topics = "${sistema-margem.topico.margem-reservada}",
            groupId = "sistema-margem-agregador",
            containerFactory = "margemReservadaContainerFactory"
    )
    public void agregarReserva(ConsumerRecord<String, MargemReservadaEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterCabecalho(consumerRecord, CABECALHO_ID);
        var timeStr = obterCabecalho(consumerRecord, CABECALHO_TIME);

        Instant timestampEvento;
        if (timeStr != null) {
            try {
                timestampEvento = Instant.parse(timeStr);
            } catch (DateTimeParseException e) {
                log.warn("Formato invalido de ce_time: {}, usando timestamp do registro Kafka", timeStr);
                timestampEvento = Instant.ofEpochMilli(consumerRecord.timestamp());
            }
        } else {
            timestampEvento = Instant.ofEpochMilli(consumerRecord.timestamp());
        }

        MargemReservadaEvent evento = consumerRecord.value();
        if (evento != null) {
            margemAgregadorService.registrarReserva(
                    eventoId != null ? eventoId : "desconhecido",
                    evento.cpf(),
                    evento.solicitacaoId(),
                    timestampEvento
            );
        }

        ack.acknowledge();
    }

    private String obterCabecalho(ConsumerRecord<String, ?> registro, String chave) {
        Header cabecalho = registro.headers().lastHeader(chave);
        if (cabecalho == null) {
            return null;
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }
}
