package com.banco.ledger.processor.infrastructure.messaging;

import com.banco.ledger.processor.application.MarcadorDeSospechas;
import com.banco.ledger.processor.domain.contract.FraudAlertRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: consume fraud_alerts para cerrar el ciclo de vida en SOSPECHOSA. */
@Component
public class FraudAlertListener {

    private final MarcadorDeSospechas marcador;
    private final ObjectMapper objectMapper;

    public FraudAlertListener(MarcadorDeSospechas marcador, ObjectMapper objectMapper) {
        this.marcador = marcador;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ledger.kafka.fraud-alerts-topic}")
    public void alRecibirAlerta(String payload) throws JsonProcessingException {
        marcador.marcar(objectMapper.readValue(payload, FraudAlertRecord.class));
    }
}
