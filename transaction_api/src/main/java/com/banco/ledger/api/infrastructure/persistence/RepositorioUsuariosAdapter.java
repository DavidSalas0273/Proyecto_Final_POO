package com.banco.ledger.api.infrastructure.persistence;

import com.banco.ledger.api.application.port.RepositorioUsuarios;
import com.banco.ledger.api.domain.usuario.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class RepositorioUsuariosAdapter implements RepositorioUsuarios {
    private final SpringDataUsuarioRepository jpa;

    public RepositorioUsuariosAdapter(SpringDataUsuarioRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existeUsuario(String usuario) {
        return jpa.existsByUsuarioIgnoreCase(usuario);
    }

    @Override
    public boolean existeEmail(String email) {
        return jpa.existsByEmail(email.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean existeDocumento(String documento) {
        return jpa.existsByDocumento(documento);
    }

    @Override
    public Optional<Usuario> buscarPorUsuarioOEmail(String identificador) {
        return jpa.findFirstByUsuarioIgnoreCaseOrEmail(identificador, identificador.toLowerCase(Locale.ROOT))
                .map(UsuarioJpaEntity::aDominio);
    }

    @Override
    public void guardar(Usuario usuario) {
        jpa.findById(usuario.usuario()).ifPresentOrElse(
                existente -> existente.sincronizarCon(usuario),
                () -> jpa.save(UsuarioJpaEntity.desdeDominio(usuario)));
    }
}
