package br.com.puc.aed.sistemamargem.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Margem {

    private UUID id;
    private String cpf;
    private BigDecimal valor;
    private Integer codigoVerba;
    private Instant criadoEm;
    private Tipo tipo;

    public Margem(UUID id, String cpf, BigDecimal valor, Integer codigoVerba, Instant criadoEm, Tipo tipo) {
        this.id = id;
        this.cpf = cpf;
        this.valor = valor.multiply(Tipo.DEBITO.equals(tipo) ? BigDecimal.valueOf(-1) : BigDecimal.ONE);
        this.codigoVerba = codigoVerba;
        this.criadoEm = criadoEm;
        this.tipo = tipo;
    }

    public UUID getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Integer getCodigoVerba() {
        return codigoVerba;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public enum Tipo {
        DEBITO, CREDITO
    }
}
