package com.example.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the API Gateway.
 *
 * <p>Configures Cross-Origin Resource Sharing for the gateway,
 * allowing frontend applications from authorized origins to access downstream services.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * Configures CORS for the reactive web environment.
     * 
     * @return CorsConfigurationSource configured for reactive WebFlux
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated string
        List<String> origins = Arrays.asList(allowedOrigins.split(",\\s*"));
        configuration.setAllowedOrigins(origins);
        
        // Parse allowed methods from comma-separated string
        List<String> methods = Arrays.asList(allowedMethods.split(",\\s*"));
        configuration.setAllowedMethods(methods);
        
        // Allow specified headers (or all with "*")
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",\\s*")));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Cache preflight response
        configuration.setMaxAge(maxAge);
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
