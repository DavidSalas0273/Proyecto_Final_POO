package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.model.CuentaBancaria;

import java.util.Optional;

public interface CuentaRepository {
    Optional<CuentaBancaria> buscarPorTitular(String titular);

    void guardar(CuentaBancaria cuenta);
}
