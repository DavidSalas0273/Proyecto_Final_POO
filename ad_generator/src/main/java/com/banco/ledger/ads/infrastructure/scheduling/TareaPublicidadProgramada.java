package com.banco.ledger.ads.infrastructure.scheduling;

import com.banco.ledger.ads.application.GeneradorAnuncios;
import com.banco.ledger.ads.domain.contract.EmailCommandRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Adaptador de entrada temporal: dispara la campaña de marketing periódicamente. */
@Component
public class TareaPublicidadProgramada {

    private static final Logger log = LoggerFactory.getLogger(TareaPublicidadProgramada.class);

    private final GeneradorAnuncios generador;

    public TareaPublicidadProgramada(GeneradorAnuncios generador) {
        this.generador = generador;
    }

    @Scheduled(fixedRateString = "${ledger.ads.intervalo-ms}", initialDelayString = "${ledger.ads.retraso-inicial-ms}")
    public void ejecutar() {
        EmailCommandRecord anuncio = generador.generarAnuncio();
        log.info("Anuncio encolado para {}: {}", anuncio.to(), anuncio.body());
    }
}
