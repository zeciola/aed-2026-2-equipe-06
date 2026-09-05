package br.com.puc.aed.sistemamargem.service;

import br.com.puc.aed.sistemamargem.domain.MargemAgregadaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MargemAgregadorService {

    private static final Logger log = LoggerFactory.getLogger(MargemAgregadorService.class);

    private final long tamanhoJanelaSegundos;
    private final Map<Instant, JanelaAcumuladora> janelas = new ConcurrentHashMap<>();

    public MargemAgregadorService(@Value("${sistema-margem.agregador.janela-segundos:3600}") long tamanhoJanelaSegundos) {
        this.tamanhoJanelaSegundos = tamanhoJanelaSegundos > 0 ? tamanhoJanelaSegundos : 3600;
    }

    public void registrarReserva(String eventoId, String cpf, String solicitacaoId, Instant timestampEvento) {
        Instant inicioJanela = calcularInicioJanela(timestampEvento);
        Instant fimJanela = inicioJanela.plusSeconds(tamanhoJanelaSegundos);

        JanelaAcumuladora acumuladora = janelas.computeIfAbsent(inicioJanela, k -> new JanelaAcumuladora(inicioJanela, fimJanela));
        acumuladora.adicionar(cpf);

        log.info("Agregador [eventoId={}]: reserva computada na janela [{} a {}] para o CPF={}, total na janela={}",
                eventoId, inicioJanela, fimJanela, cpf, acumuladora.quantidadeTotal.get());
    }

    public List<MargemAgregadaVO> listarRelatorio() {
        return janelas.values().stream()
                .map(JanelaAcumuladora::toVO)
                .sorted(Comparator.comparing(MargemAgregadaVO::getInicioJanela))
                .toList();
    }

    public MargemAgregadaVO obterJanela(Instant timestamp) {
        Instant inicio = calcularInicioJanela(timestamp);
        JanelaAcumuladora acumuladora = janelas.get(inicio);
        if (acumuladora == null) {
            return new MargemAgregadaVO(inicio, inicio.plusSeconds(tamanhoJanelaSegundos), 0, Map.of());
        }
        return acumuladora.toVO();
    }

    private Instant calcularInicioJanela(Instant instante) {
        long epoch = instante.getEpochSecond();
        long inicioEpoch = (epoch / tamanhoJanelaSegundos) * tamanhoJanelaSegundos;
        return Instant.ofEpochSecond(inicioEpoch);
    }

    private static final class JanelaAcumuladora {
        private final Instant inicio;
        private final Instant fim;
        private final AtomicLong quantidadeTotal = new AtomicLong(0);
        private final Map<String, AtomicLong> reservasPorCpf = new ConcurrentHashMap<>();

        private JanelaAcumuladora(Instant inicio, Instant fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        private void adicionar(String cpf) {
            quantidadeTotal.incrementAndGet();
            reservasPorCpf.computeIfAbsent(cpf, k -> new AtomicLong(0)).incrementAndGet();
        }

        private MargemAgregadaVO toVO() {
            Map<String, Long> mapa = new ConcurrentHashMap<>();
            reservasPorCpf.forEach((k, v) -> mapa.put(k, v.get()));
            return new MargemAgregadaVO(inicio, fim, quantidadeTotal.get(), mapa);
        }
    }
}
