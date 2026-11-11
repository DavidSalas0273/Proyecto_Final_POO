package com.banco.ledger.api.infrastructure.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue transferCommandsQueue(@Value("${ledger.rabbit.transfer-queue}") String queue) {
        return new Queue(queue, true);
    }
}
