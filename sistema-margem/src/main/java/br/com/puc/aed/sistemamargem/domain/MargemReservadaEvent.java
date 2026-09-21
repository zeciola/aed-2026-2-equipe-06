package br.com.puc.aed.sistemamargem.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class MargemReservadaEvent {

    private final String cpf;
    private final String emprestimoId;

    public MargemReservadaEvent(String cpf, String emprestimoId) {
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
