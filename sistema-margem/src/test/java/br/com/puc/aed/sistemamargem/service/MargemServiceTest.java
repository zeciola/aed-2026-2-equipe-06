package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.EmprestimoSolicitadoEvent;
import br.com.puc.aed.sistemamargem.domain.MargemRecusadaEvent;
import br.com.puc.aed.sistemamargem.domain.MargemReservadaEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MargemServiceTest {

    @Mock
    private MargemRepository margemRepository;
    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;
    @Mock
    private KafkaTemplate<String, MargemRecusadaEvent> margemRecusadaEventTemplate;
    @Mock
    private KafkaTemplate<String, MargemReservadaEvent> margemReservadaEventTemplate;

    private MargemService service;

    @BeforeEach
    void montarService() {
        service = new MargemService(margemRepository, eventoProcessadoRepository,
                margemRecusadaEventTemplate, margemReservadaEventTemplate);
        ReflectionTestUtils.setField(service, "margemRecusadaTopic", "margem.recusada.v1");
        ReflectionTestUtils.setField(service, "margemReservadaTopic", "margem.reservada.v1");
        lenient().when(margemRecusadaEventTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, MargemRecusadaEvent>>any()))
                .thenReturn(envioConcluido());
        lenient().when(margemReservadaEventTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, MargemReservadaEvent>>any()))
                .thenReturn(envioConcluido());
    }

    @Test
    void publicaReservaComNovoEventoIdEPreservaEmprestimoId() {
        var eventoRecebidoId = UUID.randomUUID().toString();
        var event = new EmprestimoSolicitadoEvent(
                "emprestimo-1", "00000000000", new BigDecimal("100.00"), 500);
        when(eventoProcessadoRepository.registrarSeNovo(eventoRecebidoId)).thenReturn(true);
        when(margemRepository.margemAtual(event.cpf())).thenReturn(Optional.of(BigDecimal.ZERO));

        service.processarSolicitacaoEmprestimo(eventoRecebidoId, event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, MargemReservadaEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(margemReservadaEventTemplate).send(captor.capture());

        var record = captor.getValue();
        var novoEventoId = new String(record.headers().lastHeader("ce_id").value(), StandardCharsets.UTF_8);
        assertThat(UUID.fromString(novoEventoId)).isNotNull();
        assertThat(novoEventoId).isNotEqualTo(eventoRecebidoId);
        assertThat(record.value().solicitacaoId()).isEqualTo("emprestimo-1");
        assertThat(record.key()).isEqualTo(event.cpf());
    }

    private static <T> CompletableFuture<SendResult<String, T>> envioConcluido() {
        var metadata = new RecordMetadata(new TopicPartition("topico", 0), 0L, 0, 0L, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(null, metadata));
    }
}
