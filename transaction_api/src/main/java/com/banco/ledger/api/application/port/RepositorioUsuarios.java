package com.banco.ledger.api.application.port;

import com.banco.ledger.api.domain.usuario.Usuario;

import java.util.Optional;

/** Puerto de persistencia de los clientes de la banca en línea. */
public interface RepositorioUsuarios {

    boolean existeUsuario(String usuario);

    boolean existeEmail(String email);

    boolean existeDocumento(String documento);

    /** Busca por nombre de usuario (sin distinguir mayúsculas) o por correo. */
    Optional<Usuario> buscarPorUsuarioOEmail(String identificador);

    void guardar(Usuario usuario);
}
