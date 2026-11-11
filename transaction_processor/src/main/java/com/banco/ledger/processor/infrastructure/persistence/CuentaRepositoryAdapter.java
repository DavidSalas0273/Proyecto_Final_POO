package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.application.port.CuentaRepository;
import com.banco.ledger.processor.domain.model.CuentaBancaria;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador de persistencia de cuentas (dominio <-> JPA). */
@Repository
public class CuentaRepositoryAdapter implements CuentaRepository {

    private final SpringDataCuentaRepository jpa;

    public CuentaRepositoryAdapter(SpringDataCuentaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CuentaBancaria> buscarPorTitular(String titular) {
        return jpa.findById(titular).map(CuentaJpaEntity::aDominio);
    }

    @Override
    public void guardar(CuentaBancaria cuenta) {
        jpa.findById(cuenta.titular()).ifPresentOrElse(
                existente -> existente.sincronizarCon(cuenta),
                () -> jpa.save(CuentaJpaEntity.desdeDominio(cuenta)));
    }
}
