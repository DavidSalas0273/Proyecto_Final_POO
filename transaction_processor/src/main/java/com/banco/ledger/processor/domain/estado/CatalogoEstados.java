package com.banco.ledger.processor.domain.estado;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Traduce el código guardado en la base de datos (COMPLETED, SUSPICIOUS, ...) al objeto
 * de estado correspondiente, para reconstituir una transacción. Usa una tabla de
 * búsqueda en lugar de un switch.
 */
public final class CatalogoEstados {

    private static final Map<String, EstadoTransaccion> POR_CODIGO = Stream.of(
                    EstadoRecibida.INSTANCIA, EstadoProcesando.INSTANCIA, EstadoCompletada.INSTANCIA,
                    EstadoRechazada.INSTANCIA, EstadoSospechosa.INSTANCIA)
            .collect(Collectors.toUnmodifiableMap(EstadoTransaccion::codigo, Function.identity()));

    private CatalogoEstados() {
    }

    public static EstadoTransaccion desdeCodigo(String codigo) {
        return Optional.ofNullable(codigo)
                .map(POR_CODIGO::get)
                .orElseThrow(() -> new IllegalArgumentException("Estado de transacción desconocido: " + codigo));
    }
}
