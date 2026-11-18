package com.banco.ledger.api.infrastructure.persistence;

import com.banco.ledger.api.application.port.RepositorioSesiones;
import com.banco.ledger.api.domain.usuario.Sesion;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RepositorioSesionesAdapter implements RepositorioSesiones {
    private final SpringDataSesionRepository jpa;

    public RepositorioSesionesAdapter(SpringDataSesionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Sesion sesion) {
        jpa.save(SesionJpaEntity.desdeDominio(sesion));
    }

    @Override
    public Optional<Sesion> buscar(String token) {
        return jpa.findById(token).map(SesionJpaEntity::aDominio);
    }

    @Override
    public void eliminar(String token) {
        jpa.deleteById(token);
    }
}
