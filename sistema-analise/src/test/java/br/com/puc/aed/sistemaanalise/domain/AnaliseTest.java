package br.com.puc.aed.sistemaanalise.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnaliseTest {

    @Test
    void cpfComUltimoDigitoParAprova() {
        Analise analise = Analise.decidir("22222222222", "sol-1");

        assertThat(analise.isAprovada()).isTrue();
        assertThat(analise.getCpf()).isEqualTo("22222222222");
        assertThat(analise.getSolicitacaoId()).isEqualTo("sol-1");
        assertThat(analise.getId()).isNotNull();
        assertThat(analise.getCriadoEm()).isNotNull();
    }

    @Test
    void cpfComUltimoDigitoImparReprova() {
        Analise analise = Analise.decidir("11111111111", "sol-2");

        assertThat(analise.isAprovada()).isFalse();
    }
}
