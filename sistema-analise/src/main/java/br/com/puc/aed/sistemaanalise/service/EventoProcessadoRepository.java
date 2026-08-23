package br.com.puc.aed.sistemaanalise.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventoProcessadoRepository implements br.com.puc.aed.sistemaanalise.domain.EventoProcessadoRepository {

    private final JdbcTemplate jdbc;

    public EventoProcessadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarSeNovo(String eventoId) {
        int linhas = jdbc.update(
                "INSERT INTO evento_processado_analise (evento_id) VALUES (?) ON CONFLICT DO NOTHING", eventoId);
        return linhas == 1;
    }
}
