package com.banco.ledger.email.infrastructure;

import com.banco.ledger.email.application.ServicioEnvioEmail;
import com.banco.ledger.email.domain.contract.EmailCommandRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailQueueListener {
    private final ServicioEnvioEmail servicio;
    private final ObjectMapper objectMapper;

    public EmailQueueListener(ServicioEnvioEmail servicio, ObjectMapper objectMapper) {
        this.servicio = servicio;
        this.objectMapper = objectMapper;
    }

    // RabbitMQ: declara la cola durable email_queue
    @Bean
    public static Queue emailQueue(@Value("${ledger.rabbit.email-queue}") String queue) {
        return new Queue(queue, true);
    }

    // RabbitMQ: consume la cola email_queue; con varias réplicas, cada mensaje lo toma una sola (prefetch 1)
    @RabbitListener(queues = "${ledger.rabbit.email-queue}")
    public void alRecibirEmail(String payload) throws JsonProcessingException {
        servicio.procesar(objectMapper.readValue(payload, EmailCommandRecord.class));
    }
}
