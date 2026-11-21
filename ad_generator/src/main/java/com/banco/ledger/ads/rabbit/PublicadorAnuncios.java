package com.banco.ledger.ads.rabbit;

import com.banco.ledger.ads.dto.EmailCommandRecord;

public interface PublicadorAnuncios {
    void publicar(EmailCommandRecord anuncio);
}
