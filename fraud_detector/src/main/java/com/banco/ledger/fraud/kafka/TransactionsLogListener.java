package com.banco.ledger.fraud.kafka;

import com.banco.ledger.fraud.dto.TransactionEventRecord;
import com.banco.ledger.fraud.service.DetectorFraude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionsLogListener {
    private final DetectorFraude detector;
    private final ObjectMapper objectMapper;

    public TransactionsLogListener(DetectorFraude detector, ObjectMapper objectMapper) {
        this.detector = detector;
        this.objectMapper = objectMapper;
    }

    // Kafka: consume el topic transactions_log (grupo fraud-detector)
    @KafkaListener(topics = "${ledger.kafka.transactions-topic}")
    public void alRecibirEvento(String payload) throws JsonProcessingException {
        detector.analizar(objectMapper.readValue(payload, TransactionEventRecord.class));
    }
}
