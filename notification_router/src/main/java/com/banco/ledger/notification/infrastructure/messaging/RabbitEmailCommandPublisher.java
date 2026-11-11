package com.banco.ledger.notification.infrastructure.messaging;

import com.banco.ledger.notification.application.port.PublicadorComandosEmail;
import com.banco.ledger.notification.domain.contract.EmailCommandRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adaptador RabbitMQ: publica el comando de email en la Work Queue email_queue. */
@Component
public class RabbitEmailCommandPublisher implements PublicadorComandosEmail {

    private static final Logger log = LoggerFactory.getLogger(RabbitEmailCommandPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queue;

    public RabbitEmailCommandPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                       @Value("${ledger.rabbit.email-queue}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    @Override
    public void publicar(EmailCommandRecord comando) {
        try {
            rabbitTemplate.convertAndSend("", queue, objectMapper.writeValueAsString(comando));
            log.info("Notificación encolada para {}", comando.to());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el comando de email", e);
        }
    }
}
