package com.banco.ledger.fraud.infrastructure.messaging;

import com.banco.ledger.fraud.application.port.PublicadorAlertasFraude;
import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Adaptador Kafka: publica alertas en fraud_alerts usando tx_id como key. */
@Component
public class KafkaFraudAlertPublisher implements PublicadorAlertasFraude {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaFraudAlertPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                    @Value("${ledger.kafka.fraud-alerts-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publicar(FraudAlertRecord alerta) {
        try {
            kafkaTemplate.send(topic, alerta.txId(), objectMapper.writeValueAsString(alerta));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la alerta " + alerta.txId(), e);
        }
    }
}
