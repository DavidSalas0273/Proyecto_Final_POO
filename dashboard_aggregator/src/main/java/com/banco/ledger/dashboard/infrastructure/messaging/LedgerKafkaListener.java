package com.banco.ledger.dashboard.infrastructure.messaging;

import com.banco.ledger.dashboard.application.ProyeccionDashboard;
import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LedgerKafkaListener {
    private final ProyeccionDashboard proyeccion;
    private final ObjectMapper objectMapper;

    public LedgerKafkaListener(ProyeccionDashboard proyeccion, ObjectMapper objectMapper) {
        this.proyeccion = proyeccion;
        this.objectMapper = objectMapper;
    }

    // Kafka: consume transactions_log; el grupo es nuevo en cada arranque para releer el ledger desde el inicio
    @KafkaListener(topics = "${ledger.kafka.transactions-topic}", groupId = "dashboard-tx-${random.uuid}")
    public void alRecibirTransaccion(String payload) throws JsonProcessingException {
        proyeccion.alRecibirTransaccion(objectMapper.readValue(payload, TransactionEventRecord.class));
    }

    // Kafka: consume fraud_alerts para marcar las transacciones sospechosas
    @KafkaListener(topics = "${ledger.kafka.fraud-alerts-topic}", groupId = "dashboard-fraud-${random.uuid}")
    public void alRecibirAlerta(String payload) throws JsonProcessingException {
        proyeccion.alRecibirAlerta(objectMapper.readValue(payload, FraudAlertRecord.class));
    }
}
