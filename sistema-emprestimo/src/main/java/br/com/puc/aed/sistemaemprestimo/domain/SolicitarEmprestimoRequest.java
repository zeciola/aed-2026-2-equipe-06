package br.com.puc.aed.sistemaemprestimo.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitarEmprestimoRequest(
        String cpf,
        BigDecimal valorParcela,
        BigDecimal valorTotal,
        Integer codigoVerba
) {

    public SolicitarEmprestimoRequest {
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
    }

    public Emprestimo toEmprestimo() {
        return new Emprestimo(
                UUID.randomUUID(),
                this.cpf,
                this.valorParcela,
                this.valorTotal,
                this.codigoVerba,
                java.time.Instant.now()
        );
    }

}
