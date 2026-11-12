package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Historial de estados de cada transacción (tabla historial_estados).
 * Solo inserción: el estado vigente es la fila más reciente de cada tx_id.
 */
@Entity
@Immutable
@Table(name = "historial_estados", indexes = @Index(name = "idx_historial_tx", columnList = "tx_id"))
public class EstadoTransaccionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_id", nullable = false, length = 64)
    private String txId;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "motivo", length = 100)
    private String motivo;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;

    protected EstadoTransaccionJpaEntity() {
        // requerido por JPA
    }

    private EstadoTransaccionJpaEntity(TransactionEventRecord asiento) {
        this.txId = asiento.txId();
        this.estado = asiento.status();
        this.motivo = asiento.reason();
        this.registradoEn = asiento.timestamp();
    }

    public static EstadoTransaccionJpaEntity desdeAsiento(TransactionEventRecord asiento) {
        return new EstadoTransaccionJpaEntity(asiento);
    }

    String estado() {
        return estado;
    }

    String motivo() {
        return motivo;
    }

    Instant registradoEn() {
        return registradoEn;
    }
}
