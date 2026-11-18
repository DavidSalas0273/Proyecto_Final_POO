package com.banco.ledger.api.infrastructure.config;

import com.banco.ledger.api.application.port.TxIdGenerator;
import com.banco.ledger.api.domain.usuario.PoliticaSesion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.Duration;
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

    @Bean
    public PoliticaSesion politicaSesion(@Value("${ledger.auth.duracion-sesion-minutos}") long minutos) {
        return new PoliticaSesion(Duration.ofMinutes(minutos));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/transfer").allowedOrigins("*").allowedMethods("POST", "OPTIONS");
        registry.addMapping("/deposit").allowedOrigins("*").allowedMethods("POST", "OPTIONS");
        registry.addMapping("/auth/**").allowedOrigins("*").allowedMethods("GET", "POST", "OPTIONS");
    }
}
