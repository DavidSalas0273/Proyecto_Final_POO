package com.banco.ledger.processor.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Value Object inmutable que representa una cantidad de dinero en una divisa.
 * Nunca puede ser negativo: las operaciones devuelven nuevas instancias.
 */
public record MontoDinero(BigDecimal valor, String divisa) {

    public static final Set<String> DIVISAS_ADMITIDAS = Set.of("USD", "EUR", "COP");

    public MontoDinero {
        if (valor == null) {
            throw new IllegalArgumentException("El valor del monto no puede ser nulo");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo: " + valor);
        }
        if (divisa == null || divisa.isBlank()) {
            throw new IllegalArgumentException("La divisa no puede ser nula ni vacía");
        }
        divisa = divisa.trim().toUpperCase(Locale.ROOT);
        if (!DIVISAS_ADMITIDAS.contains(divisa)) {
            throw new IllegalArgumentException("Divisa no admitida: " + divisa);
        }
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static MontoDinero de(String valor, String divisa) {
        return new MontoDinero(new BigDecimal(valor), divisa);
    }

    public static MontoDinero cero(String divisa) {
        return new MontoDinero(BigDecimal.ZERO, divisa);
    }

    public MontoDinero sumar(MontoDinero otro) {
        exigirMismaDivisa(otro);
        return new MontoDinero(valor.add(otro.valor), divisa);
    }

    public MontoDinero restar(MontoDinero otro) {
        exigirMismaDivisa(otro);
        if (valor.compareTo(otro.valor) < 0) {
            throw new IllegalStateException("La resta produciría un monto negativo");
        }
        return new MontoDinero(valor.subtract(otro.valor), divisa);
    }

    public boolean esPositivo() {
        return valor.signum() > 0;
    }

    public boolean esMayorQue(MontoDinero otro) {
        exigirMismaDivisa(otro);
        return valor.compareTo(otro.valor) > 0;
    }

    public boolean esMayorOIgualQue(MontoDinero otro) {
        exigirMismaDivisa(otro);
        return valor.compareTo(otro.valor) >= 0;
    }

    public boolean mismaDivisaQue(MontoDinero otro) {
        return divisa.equals(otro.divisa);
    }

    private void exigirMismaDivisa(MontoDinero otro) {
        if (otro == null || !mismaDivisaQue(otro)) {
            throw new IllegalArgumentException("Operación entre divisas distintas no permitida");
        }
    }
}
