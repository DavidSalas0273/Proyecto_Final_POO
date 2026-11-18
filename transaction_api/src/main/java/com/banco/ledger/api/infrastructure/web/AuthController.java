package com.banco.ledger.api.infrastructure.web;

import com.banco.ledger.api.application.ServicioAutenticacion;
import com.banco.ledger.api.domain.usuario.DatosRegistro;
import com.banco.ledger.api.domain.usuario.SesionInvalidaException;
import com.banco.ledger.api.infrastructure.web.AuthDtos.LoginRequest;
import com.banco.ledger.api.infrastructure.web.AuthDtos.RegistroRequest;
import com.banco.ledger.api.infrastructure.web.AuthDtos.SesionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final ServicioAutenticacion autenticacion;

    public AuthController(ServicioAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/register")
    public ResponseEntity<SesionResponse> registrar(@RequestBody RegistroRequest request) {
        DatosRegistro datos = new DatosRegistro(request.fullName(), request.document(), request.email(),
                request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(SesionResponse.con(autenticacion.registrar(datos)));
    }

    @PostMapping("/login")
    public SesionResponse ingresar(@RequestBody LoginRequest request) {
        return SesionResponse.con(autenticacion.iniciarSesion(request.username(), request.password()));
    }

    @GetMapping("/me")
    public SesionResponse sesionActual(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                       String autorizacion) {
        return TokenBearer.extraer(autorizacion)
                .flatMap(autenticacion::sesionVigente)
                .map(SesionResponse::sinToken)
                .orElseThrow(SesionInvalidaException::new);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> salir(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                      String autorizacion) {
        TokenBearer.extraer(autorizacion).ifPresent(autenticacion::cerrarSesion);
        return ResponseEntity.noContent().build();
    }
}
