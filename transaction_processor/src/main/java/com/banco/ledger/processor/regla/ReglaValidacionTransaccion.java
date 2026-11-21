package com.banco.ledger.processor.regla;

import com.banco.ledger.processor.model.CuentaBancaria;
import com.banco.ledger.processor.model.Transaccion;

import java.util.Optional;

@FunctionalInterface
public interface ReglaValidacionTransaccion {
    Optional<String> evaluar(Transaccion transaccion, Optional<CuentaBancaria> origen, CuentaBancaria destino);
}
