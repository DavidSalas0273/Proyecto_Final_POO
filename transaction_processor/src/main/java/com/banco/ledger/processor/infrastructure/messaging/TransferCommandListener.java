package com.banco.ledger.processor.infrastructure.messaging;

import com.banco.ledger.processor.application.ProcesadorTransacciones;
import com.banco.ledger.processor.domain.contract.ComandoLedger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransferCommandListener {
    private static final Logger log = LoggerFactory.getLogger(TransferCommandListener.class);

    private final ProcesadorTransacciones procesador;
    private final ObjectMapper objectMapper;

    public TransferCommandListener(ProcesadorTransacciones procesador, ObjectMapper objectMapper) {
        this.procesador = procesador;
        this.objectMapper = objectMapper;
    }

    // RabbitMQ: consume la cola transfer_commands (prefetch 1, un consumidor para conservar el orden)
    @RabbitListener(queues = "${ledger.rabbit.transfer-queue}")
    public void alRecibirComando(String payload) throws JsonProcessingException {
        ComandoLedger comando = objectMapper.readValue(payload, ComandoLedger.class);
        log.info("Comando recibido: {}", comando);
        procesador.procesar(comando).ifPresent(evento ->
                log.info("Movimiento {} [{}] finalizado con estado {}", evento.txId(), evento.type(),
                        evento.status()));
    }
}
