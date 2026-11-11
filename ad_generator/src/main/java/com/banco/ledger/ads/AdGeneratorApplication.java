package com.banco.ledger.ads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdGeneratorApplication.class, args);
    }
}
