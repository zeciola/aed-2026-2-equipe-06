package br.com.puc.aed.sistemaanalise;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class TestPublisher {

    private final KafkaTemplate<String, String> template;
    private final String topico;

    public TestPublisher(String servidores, String topico) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servidores);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ProducerFactory<String, String> fabrica = new DefaultKafkaProducerFactory<>(config);
        this.template = new KafkaTemplate<>(fabrica);
        this.topico = topico;
    }

    public void publicar(String eventoId, String cpf, String solicitacaoId) {
        String json = "{\"cpf\":\"" + cpf + "\",\"solicitacaoId\":\"" + solicitacaoId + "\"}";

        ProducerRecord<String, String> registro = new ProducerRecord<>(topico, cpf, json);

        registro.headers().add("ce-specversion", "1.0".getBytes(UTF_8));
        registro.headers().add("ce-id", eventoId.getBytes(UTF_8));
        registro.headers().add("ce-source", "sistema-margem".getBytes(UTF_8));
        registro.headers().add("ce-type", "margem.reservada.v1".getBytes(UTF_8));
        registro.headers().add("ce-time", Instant.now().toString().getBytes(UTF_8));

        template.send(registro);
        template.flush();
    }
}
