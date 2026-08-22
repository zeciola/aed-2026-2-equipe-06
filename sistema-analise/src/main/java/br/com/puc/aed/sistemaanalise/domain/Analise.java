package br.com.puc.aed.sistemaanalise.domain;

import java.time.Instant;
import java.util.UUID;

public class Analise {

    private final UUID id;
    private final String cpf;
    private final String solicitacaoId;
    private final boolean aprovada;
    private final Instant criadoEm;

    public Analise(UUID id, String cpf, String solicitacaoId, boolean aprovada, Instant criadoEm) {
        this.id = id;
        this.cpf = cpf;
        this.solicitacaoId = solicitacaoId;
        this.aprovada = aprovada;
        this.criadoEm = criadoEm;
    }

    public static Analise decidir(String cpf, String solicitacaoId) {
        int ultimoDigito = Character.getNumericValue(cpf.charAt(cpf.length() - 1));
        boolean aprovada = ultimoDigito % 2 == 0;
        return new Analise(UUID.randomUUID(), cpf, solicitacaoId, aprovada, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public String getSolicitacaoId() {
        return solicitacaoId;
    }

    public boolean isAprovada() {
        return aprovada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
