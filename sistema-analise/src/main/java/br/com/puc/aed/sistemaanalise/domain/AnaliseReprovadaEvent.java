package br.com.puc.aed.sistemaanalise.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class AnaliseReprovadaEvent {

    private final String cpf;
    private final String solicitacaoId;
    private final String motivo;

    public AnaliseReprovadaEvent(String cpf, String solicitacaoId, String motivo) {
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
        return "AnaliseReprovadaEvent{cpf='" + cpf + "', solicitacaoId='" + solicitacaoId
                + "', motivo='" + motivo + "'}";
    }
}
