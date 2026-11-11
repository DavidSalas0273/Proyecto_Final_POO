package com.banco.ledger.fraud.infrastructure.persistence;

import com.banco.ledger.fraud.domain.contract.FraudAlertRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/** Registro de auditoría forense (solo inserción) de una alerta de fraude. */
@Entity
@Immutable
@Table(name = "alertas_fraude")
public class AlertaFraudeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_id", nullable = false, length = 64)
    private String txId;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "from_user", length = 100)
    private String fromUser;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected AlertaFraudeJpaEntity() {
        // requerido por JPA
    }

    private AlertaFraudeJpaEntity(FraudAlertRecord alerta) {
        this.txId = alerta.txId();
        this.reason = alerta.reason();
        this.fromUser = alerta.fromUser();
        this.amount = alerta.amount();
        this.detectedAt = alerta.detectedAt();
    }

    public static AlertaFraudeJpaEntity desdeAlerta(FraudAlertRecord alerta) {
        return new AlertaFraudeJpaEntity(alerta);
    }
}
