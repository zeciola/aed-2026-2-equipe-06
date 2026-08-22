package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.EventoProcessado;
import org.springframework.data.repository.CrudRepository;

public interface EventoProcessadoRepository extends CrudRepository<EventoProcessado, String> {
}
