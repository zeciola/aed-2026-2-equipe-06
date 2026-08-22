package br.com.puc.aed.sistemamargem.domain;

public record MargemAprovadaEvent(
        String cpf,
        String solicitacaoId
) {}
