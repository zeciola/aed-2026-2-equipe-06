package br.com.puc.aed.sistemaanalise.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class MargemReservadaEvent {

    private final String cpf;
    private final String emprestimoId;

    @JsonCreator
    public MargemReservadaEvent(@JsonProperty("cpf") String cpf,
                                @JsonProperty("emprestimoId") String emprestimoId) {
        this.cpf = cpf;
        this.emprestimoId = emprestimoId;
    }

    @JsonProperty("cpf")
    public String cpf() {
        return cpf;
    }

    @JsonProperty("emprestimoId")
    public String emprestimoId() {
        return emprestimoId;
    }

    @Override
    public String toString() {
        return "MargemReservadaEvent{cpf='" + cpf + "', emprestimoId='" + emprestimoId + "'}";
    }
}
