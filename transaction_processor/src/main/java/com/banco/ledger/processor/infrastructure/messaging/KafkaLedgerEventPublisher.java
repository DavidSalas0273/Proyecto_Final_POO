package com.banco.ledger.processor.infrastructure.messaging;

import com.banco.ledger.processor.application.port.PublicadorEventosLedger;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Adaptador Kafka: publica el evento en transactions_log usando tx_id como key. */
@Component
public class KafkaLedgerEventPublisher implements PublicadorEventosLedger {

    private static final Logger log = LoggerFactory.getLogger(KafkaLedgerEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaLedgerEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                     @Value("${ledger.kafka.transactions-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publicar(TransactionEventRecord evento) {
        try {
            String json = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, evento.txId(), json).get(10, TimeUnit.SECONDS);
            log.info("Evento {} [{}] publicado en {}", evento.txId(), evento.status(), topic);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + evento.txId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicación interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("No se pudo publicar el evento en Kafka", e);
        }
    }
}
