package com.banco.ledger.processor.repository;

import com.banco.ledger.processor.dto.TransactionEventRecord;
import com.banco.ledger.processor.entity.EstadoTransaccionJpaEntity;
import com.banco.ledger.processor.entity.TransaccionJpaEntity;
import com.banco.ledger.processor.model.Transaccion;
import com.banco.ledger.processor.model.TransactionId;

import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class TransaccionRepositoryAdapter implements TransaccionRepository {
    private final SpringDataTransaccionRepository ledger;
    private final SpringDataEstadoRepository historial;

    public TransaccionRepositoryAdapter(SpringDataTransaccionRepository ledger,
                                        SpringDataEstadoRepository historial) {
        this.ledger = ledger;
        this.historial = historial;
    }

    @Override
    public boolean existe(TransactionId id) {
        return ledger.existsById(id.valor());
    }

    @Override
    public void guardar(TransactionEventRecord asiento) {
        ledger.save(TransaccionJpaEntity.desdeEvento(asiento));
        historial.save(EstadoTransaccionJpaEntity.desdeAsiento(asiento));
    }

    @Override
    public Optional<Transaccion> buscar(TransactionId id) {
        return ledger.findById(id.valor())
                .map(asiento -> asiento.aDominio(historial.findFirstByTxIdOrderByIdDesc(id.valor())));
    }

    @Override
    public void registrarCambioDeEstado(TransactionEventRecord asiento) {
        historial.save(EstadoTransaccionJpaEntity.desdeAsiento(asiento));
    }
}
