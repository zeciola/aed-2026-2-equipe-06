package br.com.puc.aed.sistemaemprestimo.service;

import br.com.puc.aed.sistemaemprestimo.domain.Emprestimo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmprestimoRepository extends CrudRepository<Emprestimo, UUID> {
}
