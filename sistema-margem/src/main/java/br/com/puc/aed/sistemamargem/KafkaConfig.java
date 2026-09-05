package br.com.puc.aed.sistemamargem;

import br.com.puc.aed.sistemamargem.domain.AnaliseReprovadaEvent;
import br.com.puc.aed.sistemamargem.domain.MargemReservadaEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${sistema-margem.topico.margem-recusada}")
    private String margemRecusadaTopic;

    @Value("${sistema-margem.topico.margem-reservada}")
    private String margemReservadaTopic;

    @Value("${sistema-margem.topico.margem-liberada:margem.liberada.v1}")
    private String margemLiberadaTopic;

    @Bean
    public NewTopic margemRecusadaTopic() {
        return TopicBuilder.name(margemRecusadaTopic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic margemReservadaTopic() {
        return TopicBuilder.name(margemReservadaTopic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic margemLiberadaTopic() {
        return TopicBuilder.name(margemLiberadaTopic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic emprestimoSolicitadoDlqTopic() {
        return TopicBuilder.name("emprestimo.solicitado.v1.dlq")
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MargemReservadaEvent> margemReservadaContainerFactory(
            KafkaProperties kafkaProperties, CommonErrorHandler errorHandler) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<MargemReservadaEvent> jsonDeserializer = new JsonDeserializer<>(MargemReservadaEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ConsumerFactory<String, MargemReservadaEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, MargemReservadaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnaliseReprovadaEvent> analiseReprovadaContainerFactory(
            KafkaProperties kafkaProperties, CommonErrorHandler errorHandler) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<AnaliseReprovadaEvent> jsonDeserializer = new JsonDeserializer<>(AnaliseReprovadaEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ConsumerFactory<String, AnaliseReprovadaEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, AnaliseReprovadaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
