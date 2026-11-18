package com.banco.ledger.email.application.port;

import com.banco.ledger.email.domain.contract.EmailCommandRecord;

public interface DespachadorEmail {
    void despachar(EmailCommandRecord email);
}
