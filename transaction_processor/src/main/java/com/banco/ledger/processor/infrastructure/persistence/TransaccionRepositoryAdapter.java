package com.banco.ledger.processor.infrastructure.persistence;

import com.banco.ledger.processor.application.port.TransaccionRepository;
import com.banco.ledger.processor.domain.contract.TransactionEventRecord;
import com.banco.ledger.processor.domain.model.TransactionId;
import com.banco.ledger.processor.domain.model.Transaccion;
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
