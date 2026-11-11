package com.banco.ledger.ads.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoPromocionesTest {

    @Test
    void rotaLasPromocionesDeFormaCircular() {
        // Arrange
        CatalogoPromociones catalogo = new CatalogoPromociones(List.of("P1", "P2"));

        // Act & Assert
        assertThat(catalogo.siguientePromocion()).isEqualTo("P1");
        assertThat(catalogo.siguientePromocion()).isEqualTo("P2");
        assertThat(catalogo.siguientePromocion()).isEqualTo("P1");
    }

    @Test
    void catalogoVacioFallaRapido() {
        assertThatThrownBy(() -> new CatalogoPromociones(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CatalogoPromociones(List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
