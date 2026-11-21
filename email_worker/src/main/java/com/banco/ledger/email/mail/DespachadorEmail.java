package com.banco.ledger.email.mail;

import com.banco.ledger.email.dto.EmailCommandRecord;

public interface DespachadorEmail {
    void despachar(EmailCommandRecord email);
}
