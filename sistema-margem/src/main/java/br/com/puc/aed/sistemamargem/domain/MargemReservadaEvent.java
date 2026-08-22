package br.com.puc.aed.sistemamargem.domain;

public record MargemReservadaEvent(
        String cpf,
        String solicitacaoId,
        String motivo
) {}
