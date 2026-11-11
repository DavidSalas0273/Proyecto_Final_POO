package com.banco.ledger.dashboard.infrastructure.web;

import com.banco.ledger.dashboard.domain.model.SaldoCuenta;
import com.banco.ledger.dashboard.domain.model.TransaccionVista;
import com.banco.ledger.dashboard.domain.model.VistaMaterializada;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas (lado Query de CQRS) de la vista materializada. */
@RestController
@CrossOrigin(origins = "*")
public class TransaccionesController {

    private final VistaMaterializada vista;

    public TransaccionesController(VistaMaterializada vista) {
        this.vista = vista;
    }

    @GetMapping("/api/transactions")
    public List<TransaccionVista> transacciones() {
        return vista.instantanea();
    }

    @GetMapping("/api/accounts")
    public List<SaldoCuenta> cuentas() {
        return vista.cuentas();
    }
}
