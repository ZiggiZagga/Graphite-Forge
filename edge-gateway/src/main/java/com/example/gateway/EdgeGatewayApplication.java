
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for the Edge Gateway microservice.
 *
 * <p>This service acts as the main API gateway for the Graphite-Forge platform, handling
 * routing, security, and service discovery for all backend microservices. It leverages
 * Spring Cloud Gateway and integrates with Eureka for dynamic service registration.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EdgeGatewayApplication {

    /**
     * Bootstraps the Edge Gateway application.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EdgeGatewayApplication.class, args);
    }
}
