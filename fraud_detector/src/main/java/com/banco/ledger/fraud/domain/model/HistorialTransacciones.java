package com.banco.ledger.fraud.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Estado en memoria del consumidor stateful: ventana deslizante de transacciones
 * por usuario. Purga automáticamente lo que queda fuera del periodo de retención.
 */
public class HistorialTransacciones {

    private final Duration retencion;
    private final Map<String, Deque<Instant>> porUsuario = new HashMap<>();

    public HistorialTransacciones(Duration retencion) {
        this.retencion = Objects.requireNonNull(retencion, "La retención es obligatoria");
        if (retencion.isNegative() || retencion.isZero()) {
            throw new IllegalArgumentException("La retención debe ser positiva");
        }
    }

    public synchronized void registrar(String usuario, Instant momento) {
        Deque<Instant> marcas = porUsuario.computeIfAbsent(usuario, clave -> new ArrayDeque<>());
        marcas.addLast(momento);
        Instant limite = momento.minus(retencion);
        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(limite)) {
            marcas.pollFirst();
        }
    }

    public synchronized long contarDesde(String usuario, Instant desde) {
        return porUsuario.getOrDefault(usuario, new ArrayDeque<>()).stream()
                .filter(momento -> !momento.isBefore(desde))
                .count();
    }
}
