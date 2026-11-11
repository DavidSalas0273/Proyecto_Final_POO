package com.banco.ledger.email.infrastructure;

import com.banco.ledger.email.domain.contract.EmailCommandRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsolaDespachadorEmailTest {

    @Test
    void imprimeElFormatoEsperado() {
        // Arrange
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ConsolaDespachadorEmail despachador =
                new ConsolaDespachadorEmail(new PrintStream(buffer, true, StandardCharsets.UTF_8), 0);

        // Act
        despachador.despachar(new EmailCommandRecord("user_a@email.com", "Envió $50 a B"));

        // Assert
        assertThat(buffer.toString(StandardCharsets.UTF_8).trim())
                .isEqualTo("ENVIANDO EMAIL a user_a@email.com: Envió $50 a B");
    }

    @Test
    void comandoInvalidoFallaRapido() {
        assertThatThrownBy(() -> new EmailCommandRecord(null, "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
