package com.seruhioCode30.survival72.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // No es necesario agregar métodos aquí.
    // La anotación @EnableScheduling es suficiente para activar las tareas programadas.
}

