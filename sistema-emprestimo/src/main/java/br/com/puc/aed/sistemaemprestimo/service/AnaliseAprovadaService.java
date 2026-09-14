package br.com.puc.aed.sistemaemprestimo.service;

import br.com.puc.aed.sistemaemprestimo.domain.AnaliseAprovadaEvent;
import br.com.puc.aed.sistemaemprestimo.domain.EventoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnaliseAprovadaService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseAprovadaService.class);

    private final EventoProcessadoRepository eventoProcessadoRepository;

    public AnaliseAprovadaService(EventoProcessadoRepository eventoProcessadoRepository) {
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void processarAnaliseAprovada(String eventoId, AnaliseAprovadaEvent event) {
        boolean primeiraVez = eventoProcessadoRepository.registrarSeNovo(eventoId);
        if (!primeiraVez) {
            log.warn("Evento de análise aprovada {} já processado, descartando em silêncio", eventoId);
            return;
        }

        log.info("Análise aprovada: evento={} emprestimo={} cliente={} - Emprestimo disponibilizado para o cliente",
                eventoId, event.solicitacaoId(), event.cpf());
    }
}
