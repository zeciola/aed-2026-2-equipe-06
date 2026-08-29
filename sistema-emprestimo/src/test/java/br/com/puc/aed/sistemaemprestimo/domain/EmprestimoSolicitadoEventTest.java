package br.com.puc.aed.sistemaemprestimo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EmprestimoSolicitadoEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializaIdentidadeDoEmprestimoEValorTotal() throws Exception {
        var event = new EmprestimoSolicitadoEvent(
                "emprestimo-1",
                "00000000000",
                new BigDecimal("100.00"),
                new BigDecimal("1200.00"),
                600
        );

        var json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(json.get("emprestimoId").asText()).isEqualTo("emprestimo-1");
        assertThat(json.get("valorTotal").decimalValue()).isEqualByComparingTo("1200.00");
    }
}
