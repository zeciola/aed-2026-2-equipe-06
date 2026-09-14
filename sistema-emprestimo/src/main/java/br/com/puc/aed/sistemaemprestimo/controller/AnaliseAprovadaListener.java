package br.com.puc.aed.sistemaemprestimo.controller;

import br.com.puc.aed.sistemaemprestimo.domain.AnaliseAprovadaEvent;
import br.com.puc.aed.sistemaemprestimo.service.AnaliseAprovadaService;
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
public class AnaliseAprovadaListener {

    private static final String CABECALHO_ID = "ce_id";
    private static final Logger log = LoggerFactory.getLogger(AnaliseAprovadaListener.class);

    private final AnaliseAprovadaService analiseAprovadaService;

    public AnaliseAprovadaListener(AnaliseAprovadaService analiseAprovadaService) {
        this.analiseAprovadaService = analiseAprovadaService;
    }

    @KafkaListener(
            topics = "${sistema-emprestimo.topicos.analise-aprovada}",
            groupId = "sistema-emprestimo-analise-aprovada",
            containerFactory = "analiseAprovadaContainerFactory"
    )
    public void registrarAnaliseAprovada(ConsumerRecord<String, AnaliseAprovadaEvent> consumerRecord, Acknowledgment ack) {
        var eventoId = obterId(consumerRecord);
        if (eventoId == null || eventoId.isBlank()) {
            log.error("Registro de análise aprovada sem {} descartado: particao={} offset={}",
                    CABECALHO_ID, consumerRecord.partition(), consumerRecord.offset());
            ack.acknowledge();
            return;
        }

        AnaliseAprovadaEvent evento = consumerRecord.value();
        if (evento != null) {
            analiseAprovadaService.processarAnaliseAprovada(eventoId, evento);
        }
        ack.acknowledge();
    }

    private @Nullable String obterId(ConsumerRecord<String, AnaliseAprovadaEvent> registro) {
        Header cabecalho = registro.headers().lastHeader(CABECALHO_ID);
        if (cabecalho == null) {
            log.error("Cabecalho {} nao encontrado no registro de análise aprovada {}", CABECALHO_ID, registro);
            return null;
        }
        return new String(cabecalho.value(), StandardCharsets.UTF_8);
    }
}
