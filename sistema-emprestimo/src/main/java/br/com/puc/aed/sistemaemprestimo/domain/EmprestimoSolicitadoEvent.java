package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public final class EmprestimoSolicitadoEvent {

    private final String cpf;
    private final BigDecimal valorParcela;
    private final Integer codigoVerba;

    public EmprestimoSolicitadoEvent(String cpf, BigDecimal valorParcela, Integer codigoVerba) {
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.codigoVerba = codigoVerba;
    }

    @JsonProperty("cpf")
    public String cpf() {
        return cpf;
    }

    @JsonProperty("valorParcela")
    public BigDecimal valorParcela() {
        return valorParcela;
    }

    @JsonProperty("codigoVerba")
    public Integer codigoVerba() {
        return codigoVerba;
    }

    @Override
    public String toString() {
        return "EmprestimoSolicitadoEvent{cpf='" + cpf + "', valorParcela=" + valorParcela
                + ", codigoVerba=" + codigoVerba + "}";
    }
}
