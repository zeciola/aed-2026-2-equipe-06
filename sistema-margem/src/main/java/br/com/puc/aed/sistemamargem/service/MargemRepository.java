package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.Margem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class MargemRepository implements br.com.puc.aed.sistemamargem.domain.MargemRepository {

    private final JdbcTemplate jdbc;

    public MargemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void salvar(Margem margem) {
        jdbc.update(
                "INSERT INTO margem (id, cpf, valor, codigo_verba, criado_em, tipo) VALUES (?, ?, ?, ?, ?, ?)",
                margem.getId(),
                margem.getCpf(),
                margem.getValor(),
                margem.getCodigoVerba(),
                Timestamp.from(margem.getCriadoEm()),
                margem.getTipo().name()
        );
    }

    @Override
    public Optional<BigDecimal> margemAtual(String cpf) {
        BigDecimal total = jdbc.queryForObject(
                "SELECT SUM(valor) FROM margem WHERE cpf = ?", BigDecimal.class, cpf);
        return Optional.ofNullable(total);
    }
}
