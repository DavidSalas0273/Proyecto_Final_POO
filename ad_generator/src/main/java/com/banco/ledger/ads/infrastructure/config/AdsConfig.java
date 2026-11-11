package com.banco.ledger.ads.infrastructure.config;

import com.banco.ledger.ads.domain.CatalogoPromociones;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdsConfig {

    @Bean
    public CatalogoPromociones catalogoPromociones() {
        return new CatalogoPromociones(List.of(
                "¡Obtenga nuestra nueva Tarjeta de Crédito!",
                "Transfiera sin comisiones durante todo el mes con Ledger P2P",
                "Abra su cuenta de ahorros y gane 8% E.A.",
                "Solicite su crédito de libre inversión 100% en línea"));
    }

    @Bean
    public Queue emailQueue(@Value("${ledger.rabbit.email-queue}") String queue) {
        return new Queue(queue, true);
    }
}
