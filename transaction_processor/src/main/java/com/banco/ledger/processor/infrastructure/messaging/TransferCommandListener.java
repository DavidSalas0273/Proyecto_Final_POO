package com.banco.ledger.processor.infrastructure.messaging;

import com.banco.ledger.processor.application.ProcesadorTransacciones;
import com.banco.ledger.processor.domain.contract.TransferCommandRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: consumidor competitivo de la Work Queue transfer_commands. */
@Component
public class TransferCommandListener {

    private static final Logger log = LoggerFactory.getLogger(TransferCommandListener.class);

    private final ProcesadorTransacciones procesador;
    private final ObjectMapper objectMapper;

    public TransferCommandListener(ProcesadorTransacciones procesador, ObjectMapper objectMapper) {
        this.procesador = procesador;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${ledger.rabbit.transfer-queue}")
    public void alRecibirComando(String payload) throws JsonProcessingException {
        TransferCommandRecord comando = objectMapper.readValue(payload, TransferCommandRecord.class);
        log.info("Comando recibido: {} ({} -> {}, {} {})", comando.txId(), comando.fromUser(),
                comando.toUser(), comando.amount(), comando.currency());
        procesador.procesar(comando).ifPresent(evento ->
                log.info("Transacción {} finalizada con estado {}", evento.txId(), evento.status()));
    }
}
