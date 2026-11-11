package com.banco.ledger.api.infrastructure.messaging;

import com.banco.ledger.api.application.port.TransferCommandPublisher;
import com.banco.ledger.api.domain.TransferCommandRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adaptador: publica el comando como JSON en la Work Queue transfer_commands. */
@Component
public class RabbitTransferCommandPublisher implements TransferCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitTransferCommandPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queue;

    public RabbitTransferCommandPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                          @Value("${ledger.rabbit.transfer-queue}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    @Override
    public void publicar(TransferCommandRecord comando) {
        try {
            rabbitTemplate.convertAndSend("", queue, objectMapper.writeValueAsString(comando));
            log.info("Comando {} publicado en la cola {}", comando.txId(), queue);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el comando " + comando.txId(), e);
        }
    }
}
