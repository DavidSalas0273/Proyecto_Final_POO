package com.banco.ledger.email.application.port;

import com.banco.ledger.email.domain.contract.EmailCommandRecord;

/** Puerto de salida: proveedor de correo (simulado en consola). */
public interface DespachadorEmail {

    void despachar(EmailCommandRecord email);
}
