package com.banco.ledger.email.application;

import com.banco.ledger.email.application.port.DespachadorEmail;
import com.banco.ledger.email.domain.contract.EmailCommandRecord;
import org.springframework.stereotype.Service;

/** Caso de uso: procesar un comando de email de forma desacoplada del proveedor. */
@Service
public class ServicioEnvioEmail {

    private final DespachadorEmail despachador;

    public ServicioEnvioEmail(DespachadorEmail despachador) {
        this.despachador = despachador;
    }

    public void procesar(EmailCommandRecord email) {
        despachador.despachar(email);
    }
}
