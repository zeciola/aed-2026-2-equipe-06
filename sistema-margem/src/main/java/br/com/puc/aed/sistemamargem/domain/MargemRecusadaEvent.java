package br.com.puc.aed.sistemamargem.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class MargemRecusadaEvent {

    private final String cpf;
    private final String solicitacaoId;
    private final String motivo;

    public MargemRecusadaEvent(String cpf, String solicitacaoId, String motivo) {
        this.cpf = cpf;
        this.solicitacaoId = solicitacaoId;
        this.motivo = motivo;
    }

    @JsonProperty("cpf")
    public String cpf() {
        return cpf;
    }

    @JsonProperty("solicitacaoId")
    public String solicitacaoId() {
        return solicitacaoId;
    }

    @JsonProperty("motivo")
    public String motivo() {
        return motivo;
    }

    @Override
    public String toString() {
        return "MargemRecusadaEvent{cpf='" + cpf + "', solicitacaoId='" + solicitacaoId
                + "', motivo='" + motivo + "'}";
    }
}
