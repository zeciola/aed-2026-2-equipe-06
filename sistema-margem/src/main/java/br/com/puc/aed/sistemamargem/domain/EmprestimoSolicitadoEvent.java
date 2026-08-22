package br.com.puc.aed.sistemamargem.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true) // tolerante a mudanças FORWARD
public record EmprestimoSolicitadoEvent(
        @JsonProperty("cpf")
        String cpf,

        @JsonProperty("valorParcela")
        BigDecimal valorParcela,

        @JsonProperty("codigoVerba")
        Integer codigoVerba
) {}
