package com.banco.ledger.dashboard.controller;

import com.banco.ledger.dashboard.model.SaldoCuenta;
import com.banco.ledger.dashboard.model.TransaccionVista;
import com.banco.ledger.dashboard.model.VistaMaterializada;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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
