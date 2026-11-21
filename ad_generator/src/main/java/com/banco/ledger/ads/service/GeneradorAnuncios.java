package com.banco.ledger.ads.service;

import com.banco.ledger.ads.dto.EmailCommandRecord;
import com.banco.ledger.ads.model.CatalogoPromociones;
import com.banco.ledger.ads.rabbit.PublicadorAnuncios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
