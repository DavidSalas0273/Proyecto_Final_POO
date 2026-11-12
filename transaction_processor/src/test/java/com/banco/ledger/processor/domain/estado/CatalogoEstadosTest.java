package com.banco.ledger.processor.domain.estado;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoEstadosTest {

    @Test
    void traduceCadaCodigoASuEstado() {
        assertThat(CatalogoEstados.desdeCodigo("RECEIVED")).isSameAs(EstadoRecibida.INSTANCIA);
        assertThat(CatalogoEstados.desdeCodigo("PROCESSING")).isSameAs(EstadoProcesando.INSTANCIA);
        assertThat(CatalogoEstados.desdeCodigo("COMPLETED")).isSameAs(EstadoCompletada.INSTANCIA);
        assertThat(CatalogoEstados.desdeCodigo("REJECTED")).isSameAs(EstadoRechazada.INSTANCIA);
        assertThat(CatalogoEstados.desdeCodigo("SUSPICIOUS")).isSameAs(EstadoSospechosa.INSTANCIA);
    }

    @Test
    void codigoDesconocidoOFaltanteFallaRapido() {
        assertThatThrownBy(() -> CatalogoEstados.desdeCodigo("BORRADA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogoEstados.desdeCodigo(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void soloLaCompletadaAdmiteSospecha() {
        assertThat(EstadoCompletada.INSTANCIA.admiteSospecha()).isTrue();
        assertThat(EstadoRecibida.INSTANCIA.admiteSospecha()).isFalse();
        assertThat(EstadoProcesando.INSTANCIA.admiteSospecha()).isFalse();
        assertThat(EstadoRechazada.INSTANCIA.admiteSospecha()).isFalse();
        assertThat(EstadoSospechosa.INSTANCIA.admiteSospecha()).isFalse();
    }
}
