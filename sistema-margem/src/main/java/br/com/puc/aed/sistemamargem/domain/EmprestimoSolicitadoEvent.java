package br.com.puc.aed.sistemamargem.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true) // tolerante a mudanças FORWARD
public final class EmprestimoSolicitadoEvent {

    private final String emprestimoId;
    private final String cpf;
    private final BigDecimal valorParcela;
    private final Integer codigoVerba;
    // valorTotal não é declarado de propósito: este consumidor não utiliza o campo.

    @JsonCreator
    public EmprestimoSolicitadoEvent(
            @JsonProperty("emprestimoId") String emprestimoId,
            @JsonProperty("cpf") String cpf,
            @JsonProperty("valorParcela") BigDecimal valorParcela,
            @JsonProperty("codigoVerba") Integer codigoVerba) {
        this.emprestimoId = emprestimoId;
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.codigoVerba = codigoVerba;
    }

    public String emprestimoId() {
        return emprestimoId;
    }

    public String cpf() {
        return cpf;
    }

    public BigDecimal valorParcela() {
        return valorParcela;
    }

    public Integer codigoVerba() {
        return codigoVerba;
    }

    @Override
    public String toString() {
        return "EmprestimoSolicitadoEvent{emprestimoId='" + emprestimoId + "', cpf='" + cpf
                + "', valorParcela=" + valorParcela + ", codigoVerba=" + codigoVerba + "}";
    }
}
