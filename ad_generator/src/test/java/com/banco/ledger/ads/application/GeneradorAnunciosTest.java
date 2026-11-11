package com.banco.ledger.ads.application;

import com.banco.ledger.ads.application.port.PublicadorAnuncios;
import com.banco.ledger.ads.domain.CatalogoPromociones;
import com.banco.ledger.ads.domain.contract.EmailCommandRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneradorAnunciosTest {

    @Mock
    private CatalogoPromociones catalogo;
    @Mock
    private PublicadorAnuncios publicador;

    @Test
    void publicaUnAnuncioParaTodaLaAudiencia() {
        // Arrange
        when(catalogo.siguientePromocion()).thenReturn("¡Obtenga nuestra nueva Tarjeta de Crédito!");
        GeneradorAnuncios generador = new GeneradorAnuncios(catalogo, publicador, "all_users@banco.com");

        // Act
        EmailCommandRecord anuncio = generador.generarAnuncio();

        // Assert
        verify(publicador, times(1)).publicar(anuncio);
        assertThat(anuncio.to()).isEqualTo("all_users@banco.com");
        assertThat(anuncio.body()).contains("Tarjeta de Crédito");
    }
}
