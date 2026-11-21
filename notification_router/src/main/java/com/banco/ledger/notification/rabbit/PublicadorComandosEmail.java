package com.banco.ledger.notification.rabbit;

import com.banco.ledger.notification.dto.EmailCommandRecord;

public interface PublicadorComandosEmail {
    void publicar(EmailCommandRecord comando);
}
