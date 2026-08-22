package br.com.puc.aed.sistemaanalise.service;

import br.com.puc.aed.sistemaanalise.domain.Analise;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class AnaliseRepository implements br.com.puc.aed.sistemaanalise.domain.AnaliseRepository {

    private final JdbcTemplate jdbc;

    public AnaliseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void salvar(Analise analise) {
        jdbc.update(
                "INSERT INTO analise (id, cpf, solicitacao_id, aprovada, criado_em) VALUES (?, ?, ?, ?, ?)",
                analise.getId(),
                analise.getCpf(),
                analise.getSolicitacaoId(),
                analise.isAprovada(),
                Timestamp.from(analise.getCriadoEm())
        );
    }
}
