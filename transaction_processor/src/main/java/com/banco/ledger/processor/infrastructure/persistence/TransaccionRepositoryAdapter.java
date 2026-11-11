package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;
import org.springframework.stereotype.Repository;

/** Adaptador de persistencia: guarda cada asiento del ledger en la tabla ledger_movimientos. */
@Repository
public class TransaccionRepositoryAdapter implements TransaccionRepository {

    private final SpringDataTransaccionRepository jpa;

    public TransaccionRepositoryAdapter(SpringDataTransaccionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existe(TransactionId id) {
        return jpa.existsById(id.valor());
    }

    @Override
    public void guardar(TransactionEventRecord asiento) {
        jpa.save(TransaccionJpaEntity.desdeEvento(asiento));
    }
}
