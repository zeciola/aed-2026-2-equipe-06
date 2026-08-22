package br.com.puc.aed.sistemaanalise.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.puc.aed.sistemaanalise.domain.AnaliseAprovadaEvent;
import br.com.puc.aed.sistemaanalise.domain.AnaliseReprovadaEvent;
import br.com.puc.aed.sistemaanalise.domain.MargemReservadaEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnaliseServiceTest {

    @Mock
    private AnaliseRepository analiseRepository;
    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;
    @Mock
    private KafkaTemplate<String, AnaliseAprovadaEvent> analiseAprovadaEventTemplate;
    @Mock
    private KafkaTemplate<String, AnaliseReprovadaEvent> analiseReprovadaEventTemplate;

    private AnaliseService service;

    @BeforeEach
    void montarService() {
        service = new AnaliseService(analiseRepository, eventoProcessadoRepository,
                analiseAprovadaEventTemplate, analiseReprovadaEventTemplate);
        ReflectionTestUtils.setField(service, "analiseAprovadaTopic", "analise.aprovada.v1");
        ReflectionTestUtils.setField(service, "analiseReprovadaTopic", "analise.reprovada.v1");
    }

    @Test
    void reentregaNaoProcessaDeNovo() {
        when(eventoProcessadoRepository.registrarSeNovo("evt-1")).thenReturn(false);

        service.avaliar("evt-1", new MargemReservadaEvent("22222222222", "sol-1"));

        verifyNoInteractions(analiseRepository, analiseAprovadaEventTemplate, analiseReprovadaEventTemplate);
    }

    @Test
    void cpfComUltimoDigitoParPublicaAprovada() {
        when(eventoProcessadoRepository.registrarSeNovo("evt-2")).thenReturn(true);

        service.avaliar("evt-2", new MargemReservadaEvent("22222222222", "sol-2"));

        verify(analiseRepository).salvar(any());
        verify(analiseAprovadaEventTemplate).send(any(ProducerRecord.class));
        verifyNoInteractions(analiseReprovadaEventTemplate);
    }

    @Test
    void cpfComUltimoDigitoImparPublicaReprovada() {
        when(eventoProcessadoRepository.registrarSeNovo("evt-3")).thenReturn(true);

        service.avaliar("evt-3", new MargemReservadaEvent("11111111111", "sol-3"));

        verify(analiseRepository).salvar(any());
        verify(analiseReprovadaEventTemplate).send(any(ProducerRecord.class));
        verifyNoInteractions(analiseAprovadaEventTemplate);
    }
}
