package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MargemRecusadaEvent {

    private final String cpf;
    private final String solicitacaoId;
    private final String motivo;

    @JsonCreator
    public MargemRecusadaEvent(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("solicitacaoId") String solicitacaoId,
            @JsonProperty("motivo") String motivo) {
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
