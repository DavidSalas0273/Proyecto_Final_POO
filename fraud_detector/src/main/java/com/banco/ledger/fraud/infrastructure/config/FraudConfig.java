package com.banco.ledger.fraud.infrastructure.config;

import com.banco.ledger.fraud.domain.model.HistorialTransacciones;
import com.banco.ledger.fraud.domain.regla.FrecuenciaAnomalaRegla;
import com.banco.ledger.fraud.domain.regla.PoliticaDeteccionFraude;
import com.banco.ledger.fraud.domain.regla.TransaccionAltoValorRegla;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Composición: aquí se eligen y configuran las estrategias de detección. */
@Configuration
public class FraudConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public HistorialTransacciones historialTransacciones(
            @Value("${ledger.fraude.frecuencia.ventana-segundos}") long ventanaSegundos) {
        return new HistorialTransacciones(Duration.ofSeconds(ventanaSegundos));
    }

    @Bean
    public PoliticaDeteccionFraude politicaDeteccionFraude(
            @Value("${ledger.fraude.umbral-alto-valor}") BigDecimal umbral,
            @Value("${ledger.fraude.frecuencia.max-transacciones}") int maxTransacciones,
            @Value("${ledger.fraude.frecuencia.ventana-segundos}") long ventanaSegundos) {
        return new PoliticaDeteccionFraude(List.of(
                new TransaccionAltoValorRegla(umbral),
                new FrecuenciaAnomalaRegla(maxTransacciones, Duration.ofSeconds(ventanaSegundos))));
    }

    @Bean
    public NewTopic fraudAlertsTopic(@Value("${ledger.kafka.fraud-alerts-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionsLogTopic(@Value("${ledger.kafka.transactions-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
