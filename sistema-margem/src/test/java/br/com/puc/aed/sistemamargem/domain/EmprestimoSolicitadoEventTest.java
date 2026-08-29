package br.com.puc.aed.sistemamargem.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmprestimoSolicitadoEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ignoraValorTotalQueNaoFazParteDoContratoConsumido() throws Exception {
        var json = """
                {
                  "emprestimoId": "emprestimo-1",
                  "cpf": "00000000000",
                  "valorParcela": 100.00,
                  "valorTotal": 1200.00,
                  "codigoVerba": 600
                }
                """;

        var event = objectMapper.readValue(json, EmprestimoSolicitadoEvent.class);

        assertThat(event.emprestimoId()).isEqualTo("emprestimo-1");
        assertThat(event.cpf()).isEqualTo("00000000000");
        assertThat(event.valorParcela()).isEqualByComparingTo("100.00");
        assertThat(event.codigoVerba()).isEqualTo(600);
    }
}
