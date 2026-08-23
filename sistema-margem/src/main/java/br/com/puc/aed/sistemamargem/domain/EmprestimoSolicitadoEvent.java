package br.com.puc.aed.sistemamargem.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true) // tolerante a mudanças FORWARD
public final class EmprestimoSolicitadoEvent {

    private final String cpf;
    private final BigDecimal valorParcela;
    private final Integer codigoVerba;

    @JsonCreator
    public EmprestimoSolicitadoEvent(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("valorParcela") BigDecimal valorParcela,
            @JsonProperty("codigoVerba") Integer codigoVerba) {
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.codigoVerba = codigoVerba;
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
        return "EmprestimoSolicitadoEvent{cpf='" + cpf + "', valorParcela=" + valorParcela
                + ", codigoVerba=" + codigoVerba + "}";
    }
}
