package com.banco.ledger.processor.domain.model;

import java.util.Objects;

/**
 * Política de dominio: los usuarios nuevos reciben una cuenta con un saldo inicial
 * (simula el onboarding para la demo P2P).
 */
public class PoliticaAperturaCuentas {

    private final MontoDinero saldoInicial;

    public PoliticaAperturaCuentas(MontoDinero saldoInicial) {
        this.saldoInicial = Objects.requireNonNull(saldoInicial, "El saldo inicial es obligatorio");
    }

    public CuentaBancaria abrirCuentaPara(String titular) {
        return CuentaBancaria.abrir(titular, saldoInicial);
    }
}
