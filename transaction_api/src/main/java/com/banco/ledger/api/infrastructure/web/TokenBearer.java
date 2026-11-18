package com.banco.ledger.api.infrastructure.web;

import java.util.Optional;

/** Extrae el token de la cabecera "Authorization: Bearer &lt;token&gt;". */
final class TokenBearer {

    private static final String PREFIJO = "Bearer ";

    private TokenBearer() {
    }

    static Optional<String> extraer(String cabecera) {
        return Optional.ofNullable(cabecera)
                .filter(c -> c.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length()))
                .map(c -> c.substring(PREFIJO.length()).trim())
                .filter(token -> !token.isEmpty());
    }
}
