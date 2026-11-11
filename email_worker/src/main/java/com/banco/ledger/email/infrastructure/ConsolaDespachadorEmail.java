package com.banco.ledger.email.infrastructure;

import com.banco.ledger.email.application.port.DespachadorEmail;
import com.banco.ledger.email.domain.contract.EmailCommandRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.PrintStream;

/** Adaptador que simula el envío imprimiendo en consola. */
@Component
public class ConsolaDespachadorEmail implements DespachadorEmail {

    private final PrintStream salida;
    private final long latenciaSimuladaMs;

    @Autowired
    public ConsolaDespachadorEmail(@Value("${ledger.email.latencia-simulada-ms}") long latenciaSimuladaMs) {
        this(System.out, latenciaSimuladaMs);
    }

    ConsolaDespachadorEmail(PrintStream salida, long latenciaSimuladaMs) {
        this.salida = salida;
        this.latenciaSimuladaMs = latenciaSimuladaMs;
    }

    @Override
    public void despachar(EmailCommandRecord email) {
        simularLatencia();
        salida.println("ENVIANDO EMAIL a " + email.to() + ": " + email.body());
    }

    private void simularLatencia() {
        try {
            Thread.sleep(latenciaSimuladaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
