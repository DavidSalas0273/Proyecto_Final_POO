package com.banco.ledger.fraud.regla;

import com.banco.ledger.fraud.dto.TransactionEventRecord;
import com.banco.ledger.fraud.model.HistorialTransacciones;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class FrecuenciaAnomalaRegla implements ReglaDeteccionFraude {
    public static final String MOTIVO = "ANOMALOUS_FREQUENCY";

    private final int maxTransacciones;
    private final Duration ventana;

    public FrecuenciaAnomalaRegla(int maxTransacciones, Duration ventana) {
        if (maxTransacciones <= 0) {
            throw new IllegalArgumentException("El máximo de transacciones debe ser positivo");
        }
        this.maxTransacciones = maxTransacciones;
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria");
    }

    @Override
    public Optional<String> evaluar(TransactionEventRecord evento, HistorialTransacciones historial) {
        long recientes = historial.contarDesde(evento.fromUser(), evento.timestamp().minus(ventana));
        return Optional.of(MOTIVO).filter(motivo -> recientes > maxTransacciones);
    }
}
