package com.banco.ledger.api.repository;

import com.banco.ledger.api.model.Sesion;

import java.util.Optional;

public interface RepositorioSesiones {
    void guardar(Sesion sesion);

    Optional<Sesion> buscar(String token);

    void eliminar(String token);
}
