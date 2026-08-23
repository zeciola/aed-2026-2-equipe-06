package br.com.puc.aed.sistemamargem.domain;

public interface EventoProcessadoRepository {

    /**
     * Registra o evento como processado, se ainda nao tiver sido.
     *
     * @return true se e a primeira vez (efeito deve ser aplicado); false se e reentrega.
     */
    boolean registrarSeNovo(String eventoId);
}
