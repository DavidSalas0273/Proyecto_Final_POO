package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
import org.springframework.stereotype.Repository;

/** Adaptador de persistencia: traduce el agregado de dominio a la tabla del ledger. */
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
    public void guardar(Transaccion transaccion) {
        jpa.save(TransaccionJpaEntity.desdeEvento(transaccion.aEvento()));
    }
}
