package br.com.puc.aed.sistemamargem.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public final class MargemAgregadaVO {

    private final Instant inicioJanela;
    private final Instant fimJanela;
    private final long quantidadeReservas;
    private final Map<String, Long> reservasPorCpf;

    public MargemAgregadaVO(Instant inicioJanela, Instant fimJanela, long quantidadeReservas, Map<String, Long> reservasPorCpf) {
        this.inicioJanela = inicioJanela;
        this.fimJanela = fimJanela;
        this.quantidadeReservas = quantidadeReservas;
        this.reservasPorCpf = reservasPorCpf != null ? Map.copyOf(reservasPorCpf) : Collections.emptyMap();
    }

    public Instant getInicioJanela() {
        return inicioJanela;
    }

    public Instant getFimJanela() {
        return fimJanela;
    }

    public long getQuantidadeReservas() {
        return quantidadeReservas;
    }

    public Map<String, Long> getReservasPorCpf() {
        return reservasPorCpf;
    }

    @Override
    public String toString() {
        return "MargemAgregadaVO{" +
                "inicioJanela=" + inicioJanela +
                ", fimJanela=" + fimJanela +
                ", quantidadeReservas=" + quantidadeReservas +
                ", reservasPorCpf=" + reservasPorCpf +
                '}';
    }
}
