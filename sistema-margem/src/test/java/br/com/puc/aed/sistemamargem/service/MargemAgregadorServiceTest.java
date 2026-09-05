package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.MargemAgregadaVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MargemAgregadorServiceTest {

    private MargemAgregadorService service;

    @BeforeEach
    void setUp() {
        // Janela de 3600 segundos (1 hora)
        service = new MargemAgregadorService(3600);
    }

    @Test
    @DisplayName("Agrupa eventos ocorridos na mesma hora dentro da mesma janela temporal")
    void agrupaEventosNaMesmaJanela() {
        Instant t1 = Instant.parse("2026-09-05T10:15:00Z");
        Instant t2 = Instant.parse("2026-09-05T10:45:00Z");

        service.registrarReserva("ev-1", "11111111111", "sol-1", t1);
        service.registrarReserva("ev-2", "22222222222", "sol-2", t2);
        service.registrarReserva("ev-3", "11111111111", "sol-3", t2);

        MargemAgregadaVO janela = service.obterJanela(t1);

        assertThat(janela.getQuantidadeReservas()).isEqualTo(3L);
        assertThat(janela.getReservasPorCpf().get("11111111111")).isEqualTo(2L);
        assertThat(janela.getReservasPorCpf().get("22222222222")).isEqualTo(1L);
        assertThat(janela.getInicioJanela()).isEqualTo(Instant.parse("2026-09-05T10:00:00Z"));
        assertThat(janela.getFimJanela()).isEqualTo(Instant.parse("2026-09-05T11:00:00Z"));
    }

    @Test
    @DisplayName("Cria janelas distintas para eventos ocorridos em horas diferentes")
    void separaEventosEmJanelasDistintas() {
        Instant hora10 = Instant.parse("2026-09-05T10:10:00Z");
        Instant hora11 = Instant.parse("2026-09-05T11:20:00Z");

        service.registrarReserva("ev-1", "11111111111", "sol-1", hora10);
        service.registrarReserva("ev-2", "22222222222", "sol-2", hora11);

        List<MargemAgregadaVO> relatorio = service.listarRelatorio();

        assertThat(relatorio).hasSize(2);
        assertThat(relatorio.get(0).getInicioJanela()).isEqualTo(Instant.parse("2026-09-05T10:00:00Z"));
        assertThat(relatorio.get(0).getQuantidadeReservas()).isEqualTo(1L);
        assertThat(relatorio.get(1).getInicioJanela()).isEqualTo(Instant.parse("2026-09-05T11:00:00Z"));
        assertThat(relatorio.get(1).getQuantidadeReservas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Admite evento retardatario computando na janela retroativa correta")
    void admiteEventoRetardatario() {
        Instant hora11 = Instant.parse("2026-09-05T11:30:00Z");
        Instant hora10Atrasado = Instant.parse("2026-09-05T10:50:00Z");

        // Evento da janela das 11h chega primeiro
        service.registrarReserva("ev-recente", "22222222222", "sol-2", hora11);

        // Evento da janela das 10h chega depois (retardatário)
        service.registrarReserva("ev-atrasado", "11111111111", "sol-1", hora10Atrasado);

        MargemAgregadaVO janelaDas10 = service.obterJanela(hora10Atrasado);
        assertThat(janelaDas10.getQuantidadeReservas()).isEqualTo(1L);
        assertThat(janelaDas10.getInicioJanela()).isEqualTo(Instant.parse("2026-09-05T10:00:00Z"));
    }
}
