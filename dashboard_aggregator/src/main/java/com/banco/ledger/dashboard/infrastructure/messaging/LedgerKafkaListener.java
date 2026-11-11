package com.banco.ledger.dashboard.infrastructure.messaging;

import com.banco.ledger.dashboard.application.ProyeccionDashboard;
import com.banco.ledger.dashboard.domain.contract.FraudAlertRecord;
import com.banco.ledger.dashboard.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada con dos suscripciones (transactions_log y fraud_alerts).
 * Cada arranque usa un group-id nuevo y lee desde el inicio: la vista en memoria se
 * reconstruye reproduciendo el log de eventos (Event Sourcing).
 */
@Component
public class LedgerKafkaListener {

    private final ProyeccionDashboard proyeccion;
    private final ObjectMapper objectMapper;

    public LedgerKafkaListener(ProyeccionDashboard proyeccion, ObjectMapper objectMapper) {
        this.proyeccion = proyeccion;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ledger.kafka.transactions-topic}", groupId = "dashboard-tx-${random.uuid}")
    public void alRecibirTransaccion(String payload) throws JsonProcessingException {
        proyeccion.alRecibirTransaccion(objectMapper.readValue(payload, TransactionEventRecord.class));
    }

    @KafkaListener(topics = "${ledger.kafka.fraud-alerts-topic}", groupId = "dashboard-fraud-${random.uuid}")
    public void alRecibirAlerta(String payload) throws JsonProcessingException {
        proyeccion.alRecibirAlerta(objectMapper.readValue(payload, FraudAlertRecord.class));
    }
}
