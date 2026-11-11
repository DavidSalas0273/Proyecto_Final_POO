package com.banco.ledger.notification.infrastructure.messaging;

import com.banco.ledger.notification.application.EnrutadorNotificaciones;
import com.banco.ledger.notification.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: consume transactions_log (grupo notification-router). */
@Component
public class TransactionsLogListener {

    private final EnrutadorNotificaciones enrutador;
    private final ObjectMapper objectMapper;

    public TransactionsLogListener(EnrutadorNotificaciones enrutador, ObjectMapper objectMapper) {
        this.enrutador = enrutador;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ledger.kafka.transactions-topic}")
    public void alRecibirEvento(String payload) throws JsonProcessingException {
        enrutador.enrutar(objectMapper.readValue(payload, TransactionEventRecord.class));
    }
}
