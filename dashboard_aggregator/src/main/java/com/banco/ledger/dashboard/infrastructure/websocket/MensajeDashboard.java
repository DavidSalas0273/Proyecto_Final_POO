package com.banco.ledger.dashboard.infrastructure.websocket;

import com.banco.ledger.dashboard.domain.model.SaldoCuenta;
import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Mensaje enviado al cliente:
 * SNAPSHOT (todos los movimientos + saldos) o UPSERT (un movimiento + saldos vigentes).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MensajeDashboard(String type, List<TransaccionVista> transactions, TransaccionVista transaction,
                               List<SaldoCuenta> accounts) {

    public static MensajeDashboard instantanea(List<TransaccionVista> movimientos, List<SaldoCuenta> cuentas) {
        return new MensajeDashboard("SNAPSHOT", movimientos, null, cuentas);
    }

    public static MensajeDashboard actualizacion(TransaccionVista movimiento, List<SaldoCuenta> cuentas) {
        return new MensajeDashboard("UPSERT", null, movimiento, cuentas);
    }
}
