package com.banco.ledger.notification.infrastructure.config;

import com.banco.ledger.notification.domain.PlantillaNotificaciones;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class NotificationConfig {

    @Bean
    public PlantillaNotificaciones plantillaNotificaciones(@Value("${ledger.email.dominio}") String dominio) {
        return new PlantillaNotificaciones(dominio);
    }

    @Bean
    public Queue emailQueue(@Value("${ledger.rabbit.email-queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public NewTopic transactionsLogTopic(@Value("${ledger.kafka.transactions-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
