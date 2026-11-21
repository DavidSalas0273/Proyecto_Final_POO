package com.banco.ledger.api.controller;

import com.banco.ledger.api.dto.DepositCommandRecord;
import com.banco.ledger.api.dto.DepositRequest;
import com.banco.ledger.api.dto.TransferAcceptedResponse;
import com.banco.ledger.api.dto.TransferCommandRecord;
import com.banco.ledger.api.dto.TransferRequest;
import com.banco.ledger.api.exception.SesionInvalidaException;
import com.banco.ledger.api.security.TokenBearer;
import com.banco.ledger.api.service.ServicioAutenticacion;
import com.banco.ledger.api.service.TransferService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {
    private final TransferService transferService;
    private final ServicioAutenticacion autenticacion;

    public TransferController(TransferService transferService, ServicioAutenticacion autenticacion) {
        this.transferService = transferService;
        this.autenticacion = autenticacion;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferAcceptedResponse> transfer(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion,
            @RequestBody TransferRequest request) {
        String origen = titularOr(autorizacion, request.fromUser());
        TransferCommandRecord comando = transferService.solicitarTransferencia(
                origen, request.toUser(), request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransferAcceptedResponse> deposit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion,
            @RequestBody DepositRequest request) {
        String titular = titularOr(autorizacion, request.user());
        DepositCommandRecord comando = transferService.solicitarRecarga(titular, request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }

    private String titularOr(String autorizacion, String usuarioDelCuerpo) {
        if (autorizacion == null || autorizacion.isBlank()) {
            return usuarioDelCuerpo;
        }
        return autenticacion.titularDe(TokenBearer.extraer(autorizacion).orElseThrow(SesionInvalidaException::new));
    }
}
