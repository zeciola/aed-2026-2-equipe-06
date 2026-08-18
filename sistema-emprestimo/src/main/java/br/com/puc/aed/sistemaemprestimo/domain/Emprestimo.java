package br.com.puc.aed.sistemaemprestimo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("emprestimo")
public class Emprestimo {

    @Id
    private final UUID id;
    private final String cpf;
    private final BigDecimal valorParcela;
    private final BigDecimal valorTotal;
    private final Integer codigoVerba;
    private final Instant dataEmprestimo;

    public Emprestimo(UUID id, String cpf, BigDecimal valorParcela, BigDecimal valorTotal,
                      Integer codigoVerba, Instant dataEmprestimo) {
        this.id = id;
        this.cpf = cpf;
        this.valorParcela = valorParcela;
        this.valorTotal = valorTotal;
        this.codigoVerba = codigoVerba;
        this.dataEmprestimo = dataEmprestimo;
    }

    public UUID getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public Integer getCodigoVerba() {
        return codigoVerba;
    }

    public Instant getDataEmprestimo() {
        return dataEmprestimo;
    }
}
