package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.Margem;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface MargemRepository extends CrudRepository<Margem, UUID> {

    @Query("SELECT SUM(valor) FROM margem WHERE cpf = :cpf")
    Optional<BigDecimal> margemAtual(@Param("cpf") String cpf);
}
