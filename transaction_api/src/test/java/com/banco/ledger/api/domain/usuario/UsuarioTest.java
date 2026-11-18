package com.banco.ledger.api.domain.usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {
    private static final Instant T0 = Instant.parse("2026-11-17T09:00:00Z");

    private static final Usuario.VerificadorContrasena VERIFICADOR = (plana, hash) -> ("hash:" + plana).equals(hash);

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        DatosRegistro datos = new DatosRegistro("Ana Pérez", "1020345678", "ana@correo.com", "ana", "clave2026");
        usuario = Usuario.registrar(datos, "hash:clave2026", T0);
    }

    private void fallar(int veces, Instant momento) {
        for (int i = 0; i < veces; i++) {
            assertThatThrownBy(() -> usuario.autenticar("incorrecta1", VERIFICADOR, momento))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
    }

    @Test
    void contrasenaCorrectaAutentica() {
        usuario.autenticar("clave2026", VERIFICADOR, T0);

        assertThat(usuario.intentosFallidos()).isZero();
    }

    @Test
    void cadaFalloSumaUnIntento() {
        fallar(2, T0);

        assertThat(usuario.intentosFallidos()).isEqualTo(2);
        assertThat(usuario.bloqueoVigente(T0)).isEmpty();
    }

    @Test
    void elQuintoFalloBloqueaQuinceMinutosAunqueLuegoAcierte() {
        fallar(Usuario.MAX_INTENTOS, T0);

        assertThat(usuario.bloqueoVigente(T0)).contains(T0.plus(Usuario.DURACION_BLOQUEO));
        assertThatThrownBy(() -> usuario.autenticar("clave2026", VERIFICADOR, T0.plusSeconds(60)))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    void elBloqueoTerminaCuandoPasaElTiempo() {
        fallar(Usuario.MAX_INTENTOS, T0);
        Instant despues = T0.plus(Usuario.DURACION_BLOQUEO).plusSeconds(1);

        usuario.autenticar("clave2026", VERIFICADOR, despues);

        assertThat(usuario.bloqueoVigente(despues)).isEmpty();
        assertThat(usuario.bloqueadoHasta()).isEmpty();
    }

    @Test
    void unAciertoReiniciaElContador() {
        fallar(3, T0);

        usuario.autenticar("clave2026", VERIFICADOR, T0);

        assertThat(usuario.intentosFallidos()).isZero();
    }

    @Test
    void sesionSabeSiEstaVigente() {
        Sesion sesion = new Sesion("x".repeat(43), "ana", "Ana Pérez", T0.plusSeconds(1800));

        assertThat(sesion.estaVigente(T0)).isTrue();
        assertThat(sesion.estaVigente(T0.plusSeconds(1800))).isFalse();
        assertThatThrownBy(() -> new Sesion("corto", "ana", "Ana", T0)).isInstanceOf(IllegalArgumentException.class);
    }
}
