package com.banco.ledger.processor.model;

import java.util.Objects;

public class PoliticaAperturaCuentas {
    private final MontoDinero saldoInicial;

    public PoliticaAperturaCuentas(MontoDinero saldoInicial) {
        this.saldoInicial = Objects.requireNonNull(saldoInicial, "El saldo inicial es obligatorio");
    }

    public CuentaBancaria abrirCuentaPara(String titular) {
        return CuentaBancaria.abrir(titular, saldoInicial);
    }
}
