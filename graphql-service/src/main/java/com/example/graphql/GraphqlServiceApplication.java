package com.example.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Entry point for the GraphQL CRUD microservice.
 *
 * <p>Bootstraps the Spring Boot application that exposes a reactive GraphQL API
 * for managing Item entities. Designed for extensibility and cloud-native deployment.</p>
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class GraphqlServiceApplication {

    /**
     * Bootstraps the GraphQL service.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GraphqlServiceApplication.class, args);
    }
}
