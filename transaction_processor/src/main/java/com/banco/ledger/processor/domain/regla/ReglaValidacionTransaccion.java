package com.banco.ledger.processor.domain.regla;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.Transaccion;

import java.util.Optional;

@FunctionalInterface
public interface ReglaValidacionTransaccion {
    Optional<String> evaluar(Transaccion transaccion, Optional<CuentaBancaria> origen, CuentaBancaria destino);
}
