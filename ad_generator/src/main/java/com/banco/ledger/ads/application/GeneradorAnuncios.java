package com.banco.ledger.ads.application;

import com.banco.ledger.ads.application.port.PublicadorAnuncios;
import com.banco.ledger.ads.domain.CatalogoPromociones;
import com.banco.ledger.ads.domain.contract.EmailCommandRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Caso de uso: generar la siguiente campaña y encolarla para todos los usuarios. */
@Service
public class GeneradorAnuncios {

    private final CatalogoPromociones catalogo;
    private final PublicadorAnuncios publicador;
    private final String audiencia;

    public GeneradorAnuncios(CatalogoPromociones catalogo, PublicadorAnuncios publicador,
                             @Value("${ledger.ads.audiencia}") String audiencia) {
        this.catalogo = catalogo;
        this.publicador = publicador;
        this.audiencia = audiencia;
    }

    public EmailCommandRecord generarAnuncio() {
        EmailCommandRecord anuncio = new EmailCommandRecord(audiencia, catalogo.siguientePromocion());
        publicador.publicar(anuncio);
        return anuncio;
    }
}
