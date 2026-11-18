package com.banco.ledger.ads.application.port;

import com.banco.ledger.ads.domain.contract.EmailCommandRecord;

public interface PublicadorAnuncios {
    void publicar(EmailCommandRecord anuncio);
}
