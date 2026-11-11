package com.banco.ledger.api.application;

import com.banco.ledger.api.application.port.TransferCommandPublisher;
import com.banco.ledger.api.application.port.TxIdGenerator;
import com.banco.ledger.api.domain.TransferCommandRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Caso de uso: solicitar una transferencia P2P.
 * Todas las dependencias se inyectan por constructor y son interfaces (DIP).
 */
@Service
public class TransferService {

    private final TransferCommandPublisher publisher;
    private final TxIdGenerator txIdGenerator;
    private final Clock clock;

    public TransferService(TransferCommandPublisher publisher, TxIdGenerator txIdGenerator, Clock clock) {
        this.publisher = publisher;
        this.txIdGenerator = txIdGenerator;
        this.clock = clock;
    }

    public TransferCommandRecord solicitarTransferencia(String fromUser, String toUser,
                                                        BigDecimal amount, String currency) {
        TransferCommandRecord comando = new TransferCommandRecord(
                txIdGenerator.nuevoId(), fromUser, toUser, amount, currency, clock.instant());
        publisher.publicar(comando);
        return comando;
    }
}
