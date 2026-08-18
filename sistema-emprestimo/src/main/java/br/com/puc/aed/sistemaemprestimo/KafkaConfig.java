package br.com.puc.aed.sistemaemprestimo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topic(@Value("${sistema-emprestimo.topicos.emprestimo-solicitado}")  String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
