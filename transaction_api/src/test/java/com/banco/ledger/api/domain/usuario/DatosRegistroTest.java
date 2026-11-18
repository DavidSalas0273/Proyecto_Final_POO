package com.banco.ledger.api.domain.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatosRegistroTest {
    private static DatosRegistro datos(String nombre, String documento, String email, String usuario,
                                       String contrasena) {
        return new DatosRegistro(nombre, documento, email, usuario, contrasena);
    }

    @Test
    void normalizaLosDatosValidos() {
        DatosRegistro d = datos("  Ana   María Pérez ", "1.020.345-678", "Ana@Correo.COM ", "ana_p", "clave2026");

        assertThat(d.nombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(d.documento()).isEqualTo("1020345678");
        assertThat(d.email()).isEqualTo("ana@correo.com");
        assertThat(d.usuario()).isEqualTo("ana_p");
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "soloLetras", "12345678"})
    void rechazaContrasenasDebiles(String contrasena) {
        assertThatThrownBy(() -> datos("Ana Pérez", "1020345678", "ana@correo.com", "ana", contrasena))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "con espacio", "usuario-con-guion", "unusuariodemasiadolargo1"})
    void rechazaUsuariosInvalidos(String usuario) {
        assertThatThrownBy(() -> datos("Ana Pérez", "1020345678", "ana@correo.com", usuario, "clave2026"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCorreoYDocumentoInvalidos() {
        assertThatThrownBy(() -> datos("Ana Pérez", "1020345678", "ana@correo", "ana", "clave2026"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("correo");
        assertThatThrownBy(() -> datos("Ana Pérez", "12AB", "ana@correo.com", "ana", "clave2026"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("documento");
    }

    @Test
    void rechazaCamposVacios() {
        assertThatThrownBy(() -> datos(" ", "1020345678", "ana@correo.com", "ana", "clave2026"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> datos("Ana Pérez", "1020345678", "ana@correo.com", "ana", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noExponeLaContrasenaEnToString() {
        DatosRegistro d = datos("Ana Pérez", "1020345678", "ana@correo.com", "ana", "clave2026");

        assertThat(d.toString()).doesNotContain("clave2026");
    }
}
