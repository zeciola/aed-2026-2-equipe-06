package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record EmprestimoSolicitadoEvent (
        @JsonProperty("cpf")
        String cpf,

        @JsonProperty("valorParcela")
        BigDecimal valorParcela,

        @JsonProperty("codigoVerba")
        Integer codigoVerba
) {}
