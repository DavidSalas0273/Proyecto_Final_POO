package com.banco.ledger.api.infrastructure.web;

import com.banco.ledger.api.domain.usuario.Sesion;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

final class AuthDtos {
    private AuthDtos() {
    }

    record RegistroRequest(@JsonProperty("full_name") String fullName,
                           @JsonProperty("document") String document,
                           @JsonProperty("email") String email,
                           @JsonProperty("username") String username,
                           @JsonProperty("password") String password) {
        @Override
        public String toString() {
            return "RegistroRequest[username=" + username + "]";
        }
    }

    record LoginRequest(@JsonProperty("username") String username,
                        @JsonProperty("password") String password) {
        @Override
        public String toString() {
            return "LoginRequest[username=" + username + "]";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SesionResponse(@JsonProperty("token") String token,
                          @JsonProperty("username") String username,
                          @JsonProperty("full_name") String fullName,
                          @JsonProperty("expires_at") Instant expiresAt) {
        static SesionResponse con(Sesion sesion) {
            return new SesionResponse(sesion.token(), sesion.usuario(), sesion.nombreCompleto(), sesion.expiraEn());
        }

        static SesionResponse sinToken(Sesion sesion) {
            return new SesionResponse(null, sesion.usuario(), sesion.nombreCompleto(), sesion.expiraEn());
        }
    }
}
