package com.banco.ledger.fraud.infrastructure.messaging;

import com.banco.ledger.fraud.application.DetectorFraude;
import com.banco.ledger.fraud.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: consume el ledger transactions_log. */
@Component
public class TransactionsLogListener {

    private final DetectorFraude detector;
    private final ObjectMapper objectMapper;

    public TransactionsLogListener(DetectorFraude detector, ObjectMapper objectMapper) {
        this.detector = detector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ledger.kafka.transactions-topic}")
    public void alRecibirEvento(String payload) throws JsonProcessingException {
        detector.analizar(objectMapper.readValue(payload, TransactionEventRecord.class));
    }
}
