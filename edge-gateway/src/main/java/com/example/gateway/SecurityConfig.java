package com.example.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the API Gateway.
 * The gateway validates incoming JWTs (Keycloak) and forwards requests to downstream services.
 * Includes CORS configuration for browser-based clients.
 * 
 * For development (dev profile), security is disabled to allow testing without Keycloak.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Production/Docker security configuration - requires JWT validation
     */
    @Bean
    @Profile("!dev")
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())  // Stateless API - CSRF not needed
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/health/live", "/actuator/health/ready").permitAll()  // Health checks + Kubernetes probes
                .pathMatchers("/actuator/**").authenticated()  // Protect other actuator endpoints
                .anyExchange().authenticated()  // Require auth for all other paths
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    /**
     * Development security configuration - permits all requests for local testing
     */
    @Bean
    @Profile("dev")
    public SecurityWebFilterChain springSecurityFilterChainDev(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());

        return http.build();
    }
}

