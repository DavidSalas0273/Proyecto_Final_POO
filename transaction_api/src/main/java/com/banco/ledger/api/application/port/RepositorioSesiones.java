package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.usuario.Sesion;

import java.util.Optional;

public interface RepositorioSesiones {
    void guardar(Sesion sesion);

    Optional<Sesion> buscar(String token);

    void eliminar(String token);
}
