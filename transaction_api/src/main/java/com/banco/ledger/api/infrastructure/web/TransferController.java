package com.banco.ledger.api.infrastructure.web;

import com.banco.ledger.api.application.ServicioAutenticacion;
import com.banco.ledger.api.application.TransferService;
import com.banco.ledger.api.domain.DepositCommandRecord;
import com.banco.ledger.api.domain.TransferCommandRecord;
import com.banco.ledger.api.domain.usuario.SesionInvalidaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Envíos y recargas.
 * <ul>
 *   <li>Con sesión ("Authorization: Bearer ..."): el titular de la sesión es siempre quien
 *       envía o recarga, sin importar lo que diga el cuerpo. Una sesión vencida da 401.</li>
 *   <li>Sin sesión: se respeta el contrato del enunciado (from_user en el cuerpo), que usan
 *       los flujos de prueba y los escenarios de demostración.</li>
 * </ul>
 */
@RestController
public class TransferController {

    private final TransferService transferService;
    private final ServicioAutenticacion autenticacion;

    public TransferController(TransferService transferService, ServicioAutenticacion autenticacion) {
        this.transferService = transferService;
        this.autenticacion = autenticacion;
    }

    /** Envía dinero de un usuario a otro. */
    @PostMapping("/transfer")
    public ResponseEntity<TransferAcceptedResponse> transfer(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion,
            @RequestBody TransferRequest request) {
        String origen = titularOr(autorizacion, request.fromUser());
        TransferCommandRecord comando = transferService.solicitarTransferencia(
                origen, request.toUser(), request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }

    /** Recarga el saldo de un usuario. */
    @PostMapping("/deposit")
    public ResponseEntity<TransferAcceptedResponse> deposit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String autorizacion,
            @RequestBody DepositRequest request) {
        String titular = titularOr(autorizacion, request.user());
        DepositCommandRecord comando = transferService.solicitarRecarga(titular, request.amount(), request.currency());
        return ResponseEntity.accepted().body(new TransferAcceptedResponse(comando.txId(), "QUEUED"));
    }

    /**
     * Sin cabecera Authorization se usa el usuario del cuerpo. Si la cabecera llega pero está
     * vacía, mal formada o con un token vencido, se rechaza (401): nunca se "degrada" a
     * operar sin sesión.
     */
    private String titularOr(String autorizacion, String usuarioDelCuerpo) {
        if (autorizacion == null || autorizacion.isBlank()) {
            return usuarioDelCuerpo;
        }
        return autenticacion.titularDe(TokenBearer.extraer(autorizacion).orElseThrow(SesionInvalidaException::new));
    }
}
