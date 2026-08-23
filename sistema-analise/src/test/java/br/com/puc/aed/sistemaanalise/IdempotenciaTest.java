package br.com.puc.aed.sistemaanalise;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = "margem.reservada.v1")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:aed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "logging.level.br.com.puc.aed=INFO"
})
class IdempotenciaTest {

    private static final Duration PRAZO = Duration.ofSeconds(20);
    private static final Duration JANELA_DE_OBSERVACAO = Duration.ofSeconds(3);

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${spring.embedded.kafka.brokers}")
    private String servidores;
    @Value("${sistema-analise.topico.margem-reservada}")
    private String topico;

    private TestPublisher publicador;

    @BeforeEach
    void prepararEstado() {
        jdbc.update("DELETE FROM analise");
        jdbc.update("DELETE FROM evento_processado_analise");
        publicador = new TestPublisher(servidores, topico);
    }

    @Test
    @DisplayName("evento novo gera uma analise")
    void eventoNovoGeraUmaAnalise() {
        publicador.publicar(UUID.randomUUID().toString(), "22222222222", "sol-1");

        aguardarQuantidadeDeAnalises(1);
        assertThat(contarEventosProcessados()).isEqualTo(1L);
    }

    @Test
    @DisplayName("o MESMO evento entregue tres vezes gera uma analise so")
    void reentregaNaoDuplicaOEfeito() {
        String eventoId = UUID.randomUUID().toString();

        publicador.publicar(eventoId, "22222222222", "sol-2");
        aguardarQuantidadeDeAnalises(1);

        publicador.publicar(eventoId, "22222222222", "sol-2");
        publicador.publicar(eventoId, "22222222222", "sol-2");

        confirmarQueQuantidadeNaoMuda(1);
        assertThat(contarEventosProcessados()).isEqualTo(1L);
    }

    private long contarAnalises() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM analise", Long.class);
        return total == null ? 0L : total.longValue();
    }

    private long contarEventosProcessados() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM evento_processado_analise", Long.class);
        return total == null ? 0L : total.longValue();
    }

    private void aguardarQuantidadeDeAnalises(long quantidadeEsperada) {
        Awaitility.await()
                .atMost(PRAZO)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(contarAnalises()).isEqualTo(quantidadeEsperada));
    }

    private void confirmarQueQuantidadeNaoMuda(long quantidadeEsperada) {
        Awaitility.await()
                .during(JANELA_DE_OBSERVACAO)
                .atMost(JANELA_DE_OBSERVACAO.plusSeconds(5))
                .untilAsserted(() -> assertThat(contarAnalises()).isEqualTo(quantidadeEsperada));
    }
}
