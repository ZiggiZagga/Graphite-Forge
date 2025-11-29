package com.example.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the API Gateway.
 * The gateway validates incoming JWTs (Keycloak) and forwards requests to downstream services.
 * Includes CORS configuration for browser-based clients.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())  // Stateless API - CSRF not needed
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health/live", "/actuator/health/ready").permitAll()  // Kubernetes probes
                .pathMatchers("/actuator/**").authenticated()  // Protect other actuator endpoints
                .anyExchange().authenticated()  // Require auth for all other paths
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }
}

