package com.banco.ledger.api.repository;

import com.banco.ledger.api.entity.SesionJpaEntity;
import com.banco.ledger.api.model.Sesion;

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
