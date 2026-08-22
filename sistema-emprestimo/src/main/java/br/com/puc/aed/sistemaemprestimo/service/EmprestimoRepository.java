package br.com.puc.aed.sistemaemprestimo.service;

import br.com.puc.aed.sistemaemprestimo.domain.Emprestimo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class EmprestimoRepository implements br.com.puc.aed.sistemaemprestimo.domain.EmprestimoRepository {

    private final JdbcTemplate jdbc;

    public EmprestimoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void salvar(Emprestimo emprestimo) {
        jdbc.update(
                "INSERT INTO emprestimo (id, cpf, valor_parcela, valor_total, codigo_verba, data_emprestimo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                emprestimo.getId(),
                emprestimo.getCpf(),
                emprestimo.getValorParcela(),
                emprestimo.getValorTotal(),
                emprestimo.getCodigoVerba(),
                Timestamp.from(emprestimo.getDataEmprestimo())
        );
    }
}
