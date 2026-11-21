package com.banco.ledger.api.service;

import com.banco.ledger.api.dto.DepositCommandRecord;
import com.banco.ledger.api.dto.TransferCommandRecord;
import com.banco.ledger.api.rabbit.TransferCommandPublisher;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Clock;

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

    public DepositCommandRecord solicitarRecarga(String user, BigDecimal amount, String currency) {
        DepositCommandRecord comando = new DepositCommandRecord(
                txIdGenerator.nuevoId(), user, amount, currency, clock.instant());
        publisher.publicar(comando);
        return comando;
    }
}
