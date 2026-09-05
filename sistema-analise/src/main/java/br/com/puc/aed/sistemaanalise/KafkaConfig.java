package br.com.puc.aed.sistemaanalise;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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
    public CommonErrorHandler commonErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                org.apache.kafka.common.errors.SerializationException.class
        );
        return errorHandler;
    }
}
