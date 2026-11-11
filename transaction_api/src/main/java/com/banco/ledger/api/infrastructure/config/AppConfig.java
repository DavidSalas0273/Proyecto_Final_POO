package com.banco.ledger.api.infrastructure.config;

import com.banco.ledger.api.application.port.TxIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TxIdGenerator txIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    /** Permite que dashboard.html (abierto como archivo local) envíe transferencias. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/transfer").allowedOrigins("*").allowedMethods("POST", "OPTIONS");
    }
}
