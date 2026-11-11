package com.banco.ledger.processor.domain.estado;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoTransaccionTest {

    @Test
    void transicionesPermitidas() {
        assertThat(EstadoRecibida.INSTANCIA.iniciarProcesamiento()).isSameAs(EstadoProcesando.INSTANCIA);
        assertThat(EstadoProcesando.INSTANCIA.completar()).isSameAs(EstadoCompletada.INSTANCIA);
        assertThat(EstadoProcesando.INSTANCIA.rechazar()).isSameAs(EstadoRechazada.INSTANCIA);
        assertThat(EstadoCompletada.INSTANCIA.marcarSospechosa()).isSameAs(EstadoSospechosa.INSTANCIA);
    }

    @Test
    void recibidaNoPuedeCompletarseNiRechazarse() {
        assertThatThrownBy(EstadoRecibida.INSTANCIA::completar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(EstadoRecibida.INSTANCIA::rechazar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void estadosFinalesBloqueanTodasLasTransiciones() {
        for (EstadoTransaccion estadoFinal : new EstadoTransaccion[]{EstadoRechazada.INSTANCIA, EstadoSospechosa.INSTANCIA}) {
            assertThatThrownBy(estadoFinal::iniciarProcesamiento).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(estadoFinal::completar).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(estadoFinal::rechazar).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(estadoFinal::marcarSospechosa).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void completadaNoPuedeRechazarse() {
        assertThatThrownBy(EstadoCompletada.INSTANCIA::rechazar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
    }
}
