package com.umc.menteup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Injetado no DesafioService para os testes controlarem o tempo sem Thread.sleep
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
