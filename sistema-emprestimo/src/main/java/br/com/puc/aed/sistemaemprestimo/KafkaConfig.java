package br.com.puc.aed.sistemaemprestimo;

import br.com.puc.aed.sistemaemprestimo.domain.AnaliseAprovadaEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topic(@Value("${sistema-emprestimo.topicos.emprestimo-solicitado}")  String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnaliseAprovadaEvent> analiseAprovadaContainerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        // O deserializer abaixo e configurado via setters; as props spring.json.* globais do yaml conflitam com isso.
        props.keySet().removeIf(chave -> chave.startsWith("spring.json."));

        JacksonJsonDeserializer<AnaliseAprovadaEvent> jsonDeserializer = new JacksonJsonDeserializer<>(AnaliseAprovadaEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ConsumerFactory<String, AnaliseAprovadaEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, AnaliseAprovadaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
