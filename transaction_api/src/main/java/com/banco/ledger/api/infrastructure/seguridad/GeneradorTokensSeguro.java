package com.banco.ledger.api.infrastructure.seguridad;

import com.banco.ledger.api.application.port.GeneradorTokens;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class GeneradorTokensSeguro implements GeneradorTokens {
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String nuevoToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
