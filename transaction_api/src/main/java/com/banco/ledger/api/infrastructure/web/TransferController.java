package com.banco.ledger.api.infrastructure.web;

import com.banco.ledger.api.application.TransferService;
import com.banco.ledger.api.domain.DepositCommandRecord;
import com.banco.ledger.api.domain.TransferCommandRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** Envía dinero de un usuario a otro. */
    @PostMapping("/transfer")
    public ResponseEntity<TransferAcceptedResponse> transfer(@RequestBody TransferRequest request) {
        TransferCommandRecord comando = transferService.solicitarTransferencia(
                request.fromUser(), request.toUser(), request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }

    /** Recarga el saldo de un usuario. */
    @PostMapping("/deposit")
    public ResponseEntity<TransferAcceptedResponse> deposit(@RequestBody DepositRequest request) {
        DepositCommandRecord comando = transferService.solicitarRecarga(
                request.user(), request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }
}
