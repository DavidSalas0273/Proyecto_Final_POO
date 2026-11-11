package com.banco.ledger.ads.infrastructure.messaging;

import com.banco.ledger.ads.application.port.PublicadorAnuncios;
import com.banco.ledger.ads.domain.contract.EmailCommandRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adaptador RabbitMQ: publica el anuncio en email_queue. */
@Component
public class RabbitAnuncioPublisher implements PublicadorAnuncios {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queue;

    public RabbitAnuncioPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                  @Value("${ledger.rabbit.email-queue}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.queue = queue;
    }

    @Override
    public void publicar(EmailCommandRecord anuncio) {
        try {
            rabbitTemplate.convertAndSend("", queue, objectMapper.writeValueAsString(anuncio));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el anuncio", e);
        }
    }
}
