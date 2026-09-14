package br.com.puc.aed.sistemaanalise;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic analiseAprovadaTopic(@Value("${sistema-analise.topico.analise-aprovada}") String topic) {
        return TopicBuilder.name(topic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic analiseReprovadaTopic(@Value("${sistema-analise.topico.analise-reprovada}") String topic) {
        return TopicBuilder.name(topic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic margemReservadaDlqTopic() {
        return TopicBuilder.name("margem.reservada.v1.dlq")
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaOperations<Object, Object> kafkaOperations,
                                                 KafkaProperties kafkaProperties) {
        // Registro que nao desserializou chega na DLQ como os bytes originais; o template JSON
        // padrao os transformaria em base64. Os demais registros seguem pelo template JSON.
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties();
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaOperations<Object, Object> bytesTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, kafkaOperations);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));

        // 1s, 2s, 4s, 8s, 16s e depois 30s ate 8 retentativas: ~2min de espera somada ao
        // connection-timeout do Hikari (5s por tentativa) antes de desistir e ir para a DLQ.
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(8);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                org.apache.kafka.common.errors.SerializationException.class
        );
        return errorHandler;
    }
}
