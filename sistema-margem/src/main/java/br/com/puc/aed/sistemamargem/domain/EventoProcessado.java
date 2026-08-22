package br.com.puc.aed.sistemamargem.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("evento_processado")
public class EventoProcessado {

    @Id
    private String eventoId;

    private Instant processadoEm;

    public EventoProcessado(String eventoId) {
        this.eventoId = eventoId;
        this.processadoEm = Instant.now();
    }

    public String getEventoId() {
        return eventoId;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
