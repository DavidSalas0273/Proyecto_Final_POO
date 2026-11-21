package com.banco.ledger.api.repository;

import com.banco.ledger.api.model.Usuario;

import java.util.Optional;

public interface RepositorioUsuarios {
    boolean existeUsuario(String usuario);

    boolean existeEmail(String email);

    boolean existeDocumento(String documento);

    Optional<Usuario> buscarPorUsuarioOEmail(String identificador);

    void guardar(Usuario usuario);
}
