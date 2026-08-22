package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.EmprestimoSolicitadoEvent;
import br.com.puc.aed.sistemamargem.domain.EventoProcessado;
import br.com.puc.aed.sistemamargem.domain.Margem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MargemService {

    private static final Logger log = LoggerFactory.getLogger(MargemService.class);
    private static final List<Integer> VERBAS_DEBITO = List.of(600, 700, 800, 900);

    private final MargemRepository margemRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    public MargemService(MargemRepository margemRepository,
                         EventoProcessadoRepository eventoProcessadoRepository,
                         JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.margemRepository = margemRepository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Transactional
    public void processarSolicitacaoEmprestimo(String eventoId, EmprestimoSolicitadoEvent event) {
        eventoProcessadoRepository.findById(eventoId)
                .ifPresentOrElse(
                        eventoProcessado -> log.warn("Evento {} já processado em {}", eventoId, eventoProcessado.getProcessadoEm()),
                        () -> {
                            log.info("Processando evento={} para o cliente={}", eventoId, event.cpf());
                            var tipo = VERBAS_DEBITO.contains(event.codigoVerba()) ? Margem.Tipo.DEBITO : Margem.Tipo.CREDITO;
                            var margem = new Margem(
                                    UUID.randomUUID(),
                                    event.cpf(),
                                    event.valorParcela(),
                                    event.codigoVerba(),
                                    Instant.now(),
                                    tipo
                            );

                            var valorMargemAtual = margemRepository.margemAtual(event.cpf()).orElse(BigDecimal.ZERO);

                            var margemRestante = valorMargemAtual.add(margem.getValor());

                            if (margemRestante.compareTo(BigDecimal.ZERO) < 0) {
                                throw new IllegalStateException("Margem Insuficiente");
                            }

                            jdbcAggregateTemplate.insert(margem);
                            jdbcAggregateTemplate.insert(new EventoProcessado(eventoId));
                        }
                );
    }


}
