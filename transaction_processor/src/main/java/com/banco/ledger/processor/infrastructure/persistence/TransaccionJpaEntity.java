package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de persistencia (tabla) separado del modelo de dominio.
 * @Immutable: el histórico del ledger es de solo inserción.
 */
@Entity
@Immutable
@Table(name = "ledger_transacciones")
public class TransaccionJpaEntity {

    @Id
    @Column(name = "tx_id", length = 64)
    private String txId;

    @Column(name = "from_user", nullable = false, length = 100)
    private String fromUser;

    @Column(name = "to_user", nullable = false, length = 100)
    private String toUser;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason", length = 100)
    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected TransaccionJpaEntity() {
        // requerido por JPA
    }

    private TransaccionJpaEntity(TransactionEventRecord evento) {
        this.txId = evento.txId();
        this.fromUser = evento.fromUser();
        this.toUser = evento.toUser();
        this.amount = evento.amount();
        this.currency = evento.currency();
        this.status = evento.status();
        this.reason = evento.reason();
        this.processedAt = evento.timestamp();
    }

    public static TransaccionJpaEntity desdeEvento(TransactionEventRecord evento) {
        return new TransaccionJpaEntity(evento);
    }
}
