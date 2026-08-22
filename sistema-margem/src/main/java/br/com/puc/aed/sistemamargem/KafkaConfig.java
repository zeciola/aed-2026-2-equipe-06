package br.com.puc.aed.sistemamargem;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic margemRecusadaTopic(@Value("${sistema-margem.topico.margem-recusada}") String topic) {
        return TopicBuilder.name(topic)
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic margemReservadaTopic(@Value("${sistema-margem.topico.margem-reservada}") String topic) {
        return TopicBuilder.name(topic)
                .replicas(1)
                .partitions(3)
                .build();
    }

}
