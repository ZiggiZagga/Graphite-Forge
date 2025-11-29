package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server application.
 * 
 * This server provides centralized configuration management using Graphite-Forge as the backend.
 * It supports:
 * - Hierarchical configuration organization (applications → environments → configs)
 * - Encryption of sensitive values (passwords, secrets, tokens)
 * - Dynamic refresh via Spring Cloud Bus (AMQP/Kafka)
 * - Standard Spring Cloud Config API
 * 
 * Endpoints:
 * - GET /config/{application}/{profile} - Get app config for environment
 * - GET /config/env/{profile} - Get all configs for environment
 * - GET /config/value/{profile}/{key} - Get specific config value
 * - POST /config - Create new config
 * - PUT /config/{id} - Update config
 * - DELETE /config/{id} - Delete config
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
