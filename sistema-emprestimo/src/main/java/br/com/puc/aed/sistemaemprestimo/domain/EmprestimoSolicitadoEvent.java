package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public final class EmprestimoSolicitadoEvent {

    private final String emprestimoId;
    private final String cpf;
    private final BigDecimal valorParcela;
    private final BigDecimal valorTotal;
    private final Integer codigoVerba;

    public EmprestimoSolicitadoEvent(String emprestimoId, String cpf, BigDecimal valorParcela, BigDecimal valorTotal, Integer codigoVerba) {
        this.emprestimoId = emprestimoId;
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.valorTotal = valorTotal;
        this.codigoVerba = codigoVerba;
    }

    @JsonProperty("emprestimoId")
    public String emprestimoId() {
        return emprestimoId;
    }

    @JsonProperty("cpf")
    public String cpf() {
        return cpf;
    }

    @JsonProperty("valorParcela")
    public BigDecimal valorParcela() {
        return valorParcela;
    }

    @JsonProperty("valorTotal")
    public BigDecimal valorTotal() {
        return valorTotal;
    }

    @JsonProperty("codigoVerba")
    public Integer codigoVerba() {
        return codigoVerba;
    }

    @Override
    public String toString() {
        return "EmprestimoSolicitadoEvent{emprestimoId='" + emprestimoId + "', cpf='" + cpf
                + "', valorParcela=" + valorParcela + ", valorTotal=" + valorTotal
                + ", codigoVerba=" + codigoVerba + "}";
    }
}
