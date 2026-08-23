package br.com.puc.aed.sistemaanalise.controller;

import br.com.puc.aed.sistemaanalise.domain.MargemReservadaEvent;
import br.com.puc.aed.sistemaanalise.service.AnaliseService;
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
public class AnaliseListener {

    private static final String CABECALHO_ID = "ce-id";
    private static final Logger log = LoggerFactory.getLogger(AnaliseListener.class);

    private final AnaliseService analiseService;

    public AnaliseListener(AnaliseService analiseService) {
        this.analiseService = analiseService;
    }

    @KafkaListener(topics = "${sistema-analise.topico.margem-reservada}", groupId = "sistema-analise")
    public void avaliarCredito(ConsumerRecord<String, MargemReservadaEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        analiseService.avaliar(eventoId, consumerRecord.value());
        ack.acknowledge();
    }

    private @Nullable String obterId(ConsumerRecord<String, MargemReservadaEvent> registro) {
        Header cabecalho = registro.headers().lastHeader(CABECALHO_ID);
        if (cabecalho == null) {
            log.error("Cabecalho {} nao encontrado no registro {}", CABECALHO_ID, registro);
            return null;
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }

}
