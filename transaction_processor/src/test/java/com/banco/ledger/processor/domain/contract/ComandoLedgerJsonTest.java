package com.banco.ledger.processor.domain.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica el contrato JSON polimórfico de la cola transfer_commands. */
class ComandoLedgerJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializaUnaRecarga() throws Exception {
        String json = """
                {"type":"DEPOSIT","tx_id":"tx-r","to_user":"A","amount":100,"currency":"USD",
                 "requested_at":"2026-11-11T11:30:00Z"}""";

        ComandoLedger comando = mapper.readValue(json, ComandoLedger.class);

        assertThat(comando).isInstanceOf(DepositCommandRecord.class);
        assertThat(comando.crearTransaccion().destino()).isEqualTo("A");
    }

    @Test
    void sinTipoSeAsumeTransferencia() throws Exception {
        String json = """
                {"tx_id":"tx-1","from_user":"A","to_user":"B","amount":50,"currency":"USD",
                 "requested_at":"2026-11-11T11:30:00Z"}""";

        ComandoLedger comando = mapper.readValue(json, ComandoLedger.class);

        assertThat(comando).isInstanceOf(TransferCommandRecord.class);
        assertThat(comando.crearTransaccion().origen()).contains("A");
    }
}
