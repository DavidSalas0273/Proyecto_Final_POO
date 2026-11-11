package com.banco.ledger.processor.domain.model;

import java.util.Objects;

/**
 * Entidad rica: encapsula el saldo y protege la invariante "el saldo nunca es negativo".
 * No expone setters: el saldo solo cambia mediante debitar() y acreditar().
 */
public class CuentaBancaria {

    private final String titular;
    private MontoDinero saldo;

    private CuentaBancaria(String titular, MontoDinero saldo) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("El titular de la cuenta no puede ser vacío");
        }
        this.titular = titular.trim();
        this.saldo = Objects.requireNonNull(saldo, "El saldo no puede ser nulo");
    }

    /** Abre una cuenta nueva con un saldo inicial. */
    public static CuentaBancaria abrir(String titular, MontoDinero saldoInicial) {
        return new CuentaBancaria(titular, saldoInicial);
    }

    /** Reconstruye una cuenta existente desde la persistencia. */
    public static CuentaBancaria reconstituir(String titular, MontoDinero saldo) {
        return new CuentaBancaria(titular, saldo);
    }

    public boolean tieneFondosPara(MontoDinero monto) {
        return saldo.esMayorOIgualQue(monto);
    }

    public void debitar(MontoDinero monto) {
        exigirPositivo(monto);
        if (!tieneFondosPara(monto)) {
            throw new IllegalStateException("Fondos insuficientes en la cuenta de " + titular);
        }
        saldo = saldo.restar(monto);
    }

    public void acreditar(MontoDinero monto) {
        exigirPositivo(monto);
        saldo = saldo.sumar(monto);
    }

    public boolean perteneceA(String usuario) {
        return titular.equals(usuario);
    }

    public String titular() {
        return titular;
    }

    /** Lectura del saldo como Value Object inmutable (no permite mutación externa). */
    public MontoDinero saldo() {
        return saldo;
    }

    private static void exigirPositivo(MontoDinero monto) {
        if (monto == null || !monto.esPositivo()) {
            throw new IllegalArgumentException("El monto de la operación debe ser mayor que cero");
        }
    }
}
