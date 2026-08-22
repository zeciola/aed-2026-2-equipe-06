package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SolicitarEmprestimoVO {

    private final String cpf;
    private final BigDecimal valorParcela;
    private final BigDecimal valorTotal;
    private final Integer codigoVerba;

    @JsonCreator
    public SolicitarEmprestimoVO(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("valorParcela") BigDecimal valorParcela,
            @JsonProperty("valorTotal") BigDecimal valorTotal,
            @JsonProperty("codigoVerba") Integer codigoVerba) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }
        if (valorParcela == null || valorParcela.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da parcela deve ser maior que zero");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total deve ser maior que zero");
        }
        if (codigoVerba == null || codigoVerba <= 0) {
            throw new IllegalArgumentException("Código da verba deve ser maior que zero");
        }
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.valorTotal = valorTotal;
        this.codigoVerba = codigoVerba;
    }

    public String cpf() {
        return cpf;
    }

    public BigDecimal valorParcela() {
        return valorParcela;
    }

    public BigDecimal valorTotal() {
        return valorTotal;
    }

    public Integer codigoVerba() {
        return codigoVerba;
    }

    public Emprestimo toEmprestimo() {
        return new Emprestimo(
                UUID.randomUUID(),
                this.cpf,
                this.valorParcela,
                this.valorTotal,
                this.codigoVerba,
                Instant.now()
        );
    }

}
