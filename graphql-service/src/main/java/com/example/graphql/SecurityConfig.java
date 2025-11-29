package com.example.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Reactive security configuration for graphql-service.
 * Secures /graphql endpoints and enables JWT validation via OAuth2 resource server (Keycloak).
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
            .csrf(csrf -> csrf.disable())  // Stateless GraphQL API - CSRF not needed
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health/live", "/actuator/health/ready").permitAll()  // Kubernetes probes
                .pathMatchers("/actuator/**").authenticated()  // Protect other actuator endpoints
                .pathMatchers("/graphql").authenticated()  // Protect GraphQL endpoint
                .pathMatchers("/graphiql").permitAll()  // GraphQL IDE for development
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }
}

