package com.banco.ledger.api.security;

import java.util.Optional;

public final class TokenBearer {
    private static final String PREFIJO = "Bearer ";

    private TokenBearer() {
    }

    public static Optional<String> extraer(String cabecera) {
        return Optional.ofNullable(cabecera)
                .filter(c -> c.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length()))
                .map(c -> c.substring(PREFIJO.length()).trim())
                .filter(token -> !token.isEmpty());
    }
}
