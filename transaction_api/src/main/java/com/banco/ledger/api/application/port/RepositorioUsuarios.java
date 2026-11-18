package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.usuario.Usuario;

import java.util.Optional;

public interface RepositorioUsuarios {
    boolean existeUsuario(String usuario);

    boolean existeEmail(String email);

    boolean existeDocumento(String documento);

    Optional<Usuario> buscarPorUsuarioOEmail(String identificador);

    void guardar(Usuario usuario);
}
