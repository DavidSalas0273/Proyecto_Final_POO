package com.banco.ledger.processor.application.port;

import com.banco.ledger.processor.domain.model.CuentaBancaria;

import java.util.Optional;

public interface CuentaRepository {
    Optional<CuentaBancaria> buscarPorTitular(String titular);

    void guardar(CuentaBancaria cuenta);
}
