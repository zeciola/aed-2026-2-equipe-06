package br.com.puc.aed.sistemaemprestimo.service;

import br.com.puc.aed.sistemaemprestimo.domain.EventoProcessadoRepository;
import br.com.puc.aed.sistemaemprestimo.domain.MargemRecusadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MargemRecusadaService {

    private static final Logger log = LoggerFactory.getLogger(MargemRecusadaService.class);

    private final EventoProcessadoRepository eventoProcessadoRepository;

    public MargemRecusadaService(EventoProcessadoRepository eventoProcessadoRepository) {
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void processarMargemRecusada(String eventoId, MargemRecusadaEvent event) {
        boolean primeiraVez = eventoProcessadoRepository.registrarSeNovo(eventoId);
        if (!primeiraVez) {
            log.warn("Evento de margem recusada {} já processado, descartando em silêncio", eventoId);
            return;
        }

        log.info("Margem recusada: evento={} emprestimo={} cliente={} motivo={} - Notificação enviada para o cliente",
                eventoId, event.solicitacaoId(), event.cpf(), event.motivo());
    }
}
