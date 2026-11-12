package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.estado.CatalogoEstados;
import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.TipoMovimiento;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Modelo de persistencia (tabla) separado del modelo de dominio.
 * @Immutable: el histórico del ledger es de solo inserción.
 */
@Entity
@Immutable
@Table(name = "ledger_movimientos")
public class TransaccionJpaEntity {

    @Id
    @Column(name = "tx_id", length = 64)
    private String txId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "from_user", length = 100)
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

    @Column(name = "from_balance", precision = 19, scale = 2)
    private BigDecimal fromBalance;

    @Column(name = "to_balance", precision = 19, scale = 2)
    private BigDecimal toBalance;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected TransaccionJpaEntity() {
        // requerido por JPA
    }

    private TransaccionJpaEntity(TransactionEventRecord evento) {
        this.txId = evento.txId();
        this.type = evento.type();
        this.fromUser = evento.fromUser();
        this.toUser = evento.toUser();
        this.amount = evento.amount();
        this.currency = evento.currency();
        this.status = evento.status();
        this.reason = evento.reason();
        this.fromBalance = evento.fromBalance();
        this.toBalance = evento.toBalance();
        this.processedAt = evento.timestamp();
    }

    public static TransaccionJpaEntity desdeEvento(TransactionEventRecord evento) {
        return new TransaccionJpaEntity(evento);
    }

    /**
     * Reconstruye el agregado de dominio combinando el asiento inmutable con el estado
     * vigente del historial (o el estado original del asiento si no hay historial).
     */
    public Transaccion aDominio(Optional<EstadoTransaccionJpaEntity> estadoVigente) {
        String estado = estadoVigente.map(EstadoTransaccionJpaEntity::estado).orElse(status);
        String motivo = estadoVigente.map(EstadoTransaccionJpaEntity::motivo).orElse(reason);
        Instant fecha = estadoVigente.map(EstadoTransaccionJpaEntity::registradoEn).orElse(processedAt);
        return Transaccion.reconstituir(new TransactionId(txId), TipoMovimiento.valueOf(type), fromUser, toUser,
                new MontoDinero(amount, currency), CatalogoEstados.desdeCodigo(estado), motivo, fecha);
    }
}
