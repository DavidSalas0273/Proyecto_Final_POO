package com.banco.ledger.ads.domain;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Catálogo de promociones que rota de forma circular y segura entre hilos. */
public class CatalogoPromociones {

    private final List<String> promociones;
    private final AtomicInteger siguiente = new AtomicInteger();

    public CatalogoPromociones(List<String> promociones) {
        if (promociones == null || promociones.isEmpty()) {
            throw new IllegalArgumentException("El catálogo debe tener al menos una promoción");
        }
        if (promociones.stream().anyMatch(p -> p == null || p.isBlank())) {
            throw new IllegalArgumentException("Las promociones no pueden ser vacías");
        }
        this.promociones = List.copyOf(promociones);
    }

    public String siguientePromocion() {
        return promociones.get(Math.floorMod(siguiente.getAndIncrement(), promociones.size()));
    }
}
