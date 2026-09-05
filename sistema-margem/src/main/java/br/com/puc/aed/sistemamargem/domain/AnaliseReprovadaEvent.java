package br.com.puc.aed.sistemamargem.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AnaliseReprovadaEvent {

    private final String cpf;
    private final String solicitacaoId;
    private final String motivo;

    @JsonCreator
    public AnaliseReprovadaEvent(
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
        return "AnaliseReprovadaEvent{cpf='" + cpf + "', solicitacaoId='" + solicitacaoId
                + "', motivo='" + motivo + "'}";
    }
}
