package br.com.puc.aed.sistemamargem.controller;

import br.com.puc.aed.sistemamargem.domain.AnaliseReprovadaEvent;
import br.com.puc.aed.sistemamargem.service.MargemService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MargemCompensacaoListener {

    private static final String CABECALHO_ID = "ce_id";
    private static final Logger log = LoggerFactory.getLogger(MargemCompensacaoListener.class);

    private final MargemService margemService;

    public MargemCompensacaoListener(MargemService margemService) {
        this.margemService = margemService;
    }

    @KafkaListener(
            topics = "${sistema-margem.topico.analise-reprovada}",
            groupId = "sistema-margem-compensacao",
            containerFactory = "analiseReprovadaContainerFactory"
    )
    public void compensarMargem(ConsumerRecord<String, AnaliseReprovadaEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        if (eventoId == null || eventoId.isBlank()) {
            log.error("Registro de reprovação sem {} descartado: particao={} offset={}",
                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
            ack.acknowledge();
            return;
        }

        AnaliseReprovadaEvent evento = consumerRecord.value();
        if (evento != null) {
            margemService.processarCompensacao(eventoId, evento);
        }
        ack.acknowledge();
    }

    private String obterId(ConsumerRecord<String, AnaliseReprovadaEvent> registro) {
        Header cabecalho = registro.headers().lastHeader(CABECALHO_ID);
        if (cabecalho == null) {
            log.error("Cabecalho {} nao encontrado no registro de reprovacao {}", CABECALHO_ID, registro);
            return UUID.randomUUID().toString();
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }
}
