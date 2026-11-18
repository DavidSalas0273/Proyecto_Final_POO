package com.banco.ledger.notification.application.port;

import com.banco.ledger.notification.domain.contract.EmailCommandRecord;

public interface PublicadorComandosEmail {
    void publicar(EmailCommandRecord comando);
}
