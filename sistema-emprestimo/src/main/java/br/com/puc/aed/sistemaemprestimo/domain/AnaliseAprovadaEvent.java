package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AnaliseAprovadaEvent {

    private final String cpf;
    private final String solicitacaoId;

    @JsonCreator
    public AnaliseAprovadaEvent(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("solicitacaoId") String solicitacaoId) {
        this.cpf = cpf;
        this.solicitacaoId = solicitacaoId;
    }

    @JsonProperty("cpf")
    public String cpf() {
        return cpf;
    }

    @JsonProperty("solicitacaoId")
    public String solicitacaoId() {
        return solicitacaoId;
    }

    @Override
    public String toString() {
        return "AnaliseAprovadaEvent{cpf='" + cpf + "', solicitacaoId='" + solicitacaoId + "'}";
    }
}
