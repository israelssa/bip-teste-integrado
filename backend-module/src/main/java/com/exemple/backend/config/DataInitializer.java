package com.exemple.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.exemple.backend.entity.Beneficio;
import com.exemple.backend.repository.BeneficioRepository;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(BeneficioRepository repository) {
        return args -> {
            // Dados iniciais para teste
            repository.save(new Beneficio("Vale Alimentação", "Benefício para alimentação", new BigDecimal("500.00")));
            repository.save(new Beneficio("Vale Refeição", "Benefício para refeições", new BigDecimal("800.00")));
            repository.save(new Beneficio("Plano de Saúde", "Plano de saúde empresarial", new BigDecimal("1200.00")));
            repository.save(new Beneficio("Seguro de Vida", "Seguro de vida em grupo", new BigDecimal("150.00"), false));
        };
    }
}
