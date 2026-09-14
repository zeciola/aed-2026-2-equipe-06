package br.com.puc.aed.sistemamargem;

import br.com.puc.aed.sistemamargem.domain.AnaliseReprovadaEvent;
import br.com.puc.aed.sistemamargem.domain.MargemReservadaEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.LinkedHashMap;
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MargemReservadaEvent> margemReservadaContainerFactory(
            KafkaProperties kafkaProperties, CommonErrorHandler errorHandler) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // O deserializer abaixo e configurado via setters; as props globais do yaml conflitam com isso.
        props.keySet().removeIf(chave -> chave.startsWith("spring.json.") || chave.startsWith("spring.deserializer."));

        JacksonJsonDeserializer<MargemReservadaEvent> jsonDeserializer = new JacksonJsonDeserializer<>(MargemReservadaEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ConsumerFactory<String, MargemReservadaEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));

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
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // O deserializer abaixo e configurado via setters; as props globais do yaml conflitam com isso.
        props.keySet().removeIf(chave -> chave.startsWith("spring.json.") || chave.startsWith("spring.deserializer."));

        JacksonJsonDeserializer<AnaliseReprovadaEvent> jsonDeserializer = new JacksonJsonDeserializer<>(AnaliseReprovadaEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ConsumerFactory<String, AnaliseReprovadaEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));

        ConcurrentKafkaListenerContainerFactory<String, AnaliseReprovadaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
