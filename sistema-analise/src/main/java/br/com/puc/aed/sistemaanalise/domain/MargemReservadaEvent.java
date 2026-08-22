package br.com.puc.aed.sistemaanalise.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class MargemReservadaEvent {

    private final String cpf;
    private final String solicitacaoId;

    @JsonCreator
    public MargemReservadaEvent(@JsonProperty("cpf") String cpf,
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
        return "MargemReservadaEvent{cpf='" + cpf + "', solicitacaoId='" + solicitacaoId + "'}";
    }
}
