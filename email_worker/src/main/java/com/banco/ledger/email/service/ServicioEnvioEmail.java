package com.banco.ledger.email.service;

import com.banco.ledger.email.dto.EmailCommandRecord;
import com.banco.ledger.email.mail.DespachadorEmail;

import org.springframework.stereotype.Service;

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
