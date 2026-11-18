package com.banco.ledger.dashboard.infrastructure.config;

import com.banco.ledger.dashboard.domain.model.VistaMaterializada;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class DashboardConfig {
    @Bean
    public VistaMaterializada vistaMaterializada(@Value("${ledger.dashboard.capacidad}") int capacidad) {
        return new VistaMaterializada(capacidad);
    }

    // Kafka: crea el topic transactions_log (3 particiones) si no existe
    @Bean
    public NewTopic transactionsLogTopic(@Value("${ledger.kafka.transactions-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    // Kafka: crea el topic fraud_alerts (3 particiones) si no existe
    @Bean
    public NewTopic fraudAlertsTopic(@Value("${ledger.kafka.fraud-alerts-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
