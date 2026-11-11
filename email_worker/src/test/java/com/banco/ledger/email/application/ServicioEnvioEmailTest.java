package com.banco.ledger.email.application;

import com.banco.ledger.email.application.port.DespachadorEmail;
import com.banco.ledger.email.domain.contract.EmailCommandRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServicioEnvioEmailTest {

    @Mock
    private DespachadorEmail despachador;
    @InjectMocks
    private ServicioEnvioEmail servicio;

    @Test
    void cadaComandoSeDespachaExactamenteUnaVez() {
        // Arrange
        EmailCommandRecord email = new EmailCommandRecord("user_a@email.com", "Envió $50 a B");

        // Act
        servicio.procesar(email);

        // Assert
        verify(despachador, times(1)).despachar(email);
    }
}
