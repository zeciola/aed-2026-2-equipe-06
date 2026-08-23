package br.com.puc.aed.sistemaanalise;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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

}
