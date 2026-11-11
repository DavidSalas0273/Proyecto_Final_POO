package com.banco.ledger.processor.infrastructure.config;

import com.banco.ledger.processor.domain.model.MontoDinero;
import com.banco.ledger.processor.domain.model.PoliticaAperturaCuentas;
import com.banco.ledger.processor.domain.regla.CadenaValidacion;
import com.banco.ledger.processor.domain.regla.DivisaCompatibleRegla;
import com.banco.ledger.processor.domain.regla.FondosSuficientesRegla;
import com.banco.ledger.processor.domain.regla.LimitePorTransaccionRegla;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/** Composición de dependencias: aquí se eligen las estrategias concretas del dominio. */
@Configuration
public class ProcessorConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CadenaValidacion cadenaValidacion(@Value("${ledger.reglas.limite-por-transaccion}") BigDecimal limite) {
        return new CadenaValidacion(List.of(
                new DivisaCompatibleRegla(),
                new LimitePorTransaccionRegla(limite),
                new FondosSuficientesRegla()));
    }

    @Bean
    public PoliticaAperturaCuentas politicaAperturaCuentas(
            @Value("${ledger.cuentas.saldo-inicial}") BigDecimal saldoInicial,
            @Value("${ledger.cuentas.divisa-base}") String divisaBase) {
        return new PoliticaAperturaCuentas(new MontoDinero(saldoInicial, divisaBase));
    }

    @Bean
    public Queue transferCommandsQueue(@Value("${ledger.rabbit.transfer-queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public NewTopic transactionsLogTopic(@Value("${ledger.kafka.transactions-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
