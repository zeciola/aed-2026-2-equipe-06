package br.com.puc.aed.sistemamargem.domain;

import java.math.BigDecimal;
import java.util.Optional;

public interface MargemRepository {

    void salvar(Margem margem);

    Optional<BigDecimal> margemAtual(String cpf);
}
