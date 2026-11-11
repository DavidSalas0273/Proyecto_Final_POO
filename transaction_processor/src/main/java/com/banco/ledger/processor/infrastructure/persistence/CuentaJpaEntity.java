package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.domain.model.CuentaBancaria;
import com.banco.ledger.processor.domain.model.MontoDinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/** Modelo de persistencia de la cuenta, con bloqueo optimista (@Version). */
@Entity
@Table(name = "cuentas_bancarias")
public class CuentaJpaEntity {

    @Id
    @Column(name = "titular", length = 100)
    private String titular;

    @Column(name = "saldo", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(name = "divisa", nullable = false, length = 3)
    private String divisa;

    @Version
    @Column(name = "version")
    private Long version;

    protected CuentaJpaEntity() {
        // requerido por JPA
    }

    private CuentaJpaEntity(CuentaBancaria cuenta) {
        this.titular = cuenta.titular();
        sincronizarCon(cuenta);
    }

    public static CuentaJpaEntity desdeDominio(CuentaBancaria cuenta) {
        return new CuentaJpaEntity(cuenta);
    }

    public void sincronizarCon(CuentaBancaria cuenta) {
        this.saldo = cuenta.saldo().valor();
        this.divisa = cuenta.saldo().divisa();
    }

    public CuentaBancaria aDominio() {
        return CuentaBancaria.reconstituir(titular, new MontoDinero(saldo, divisa));
    }
}
