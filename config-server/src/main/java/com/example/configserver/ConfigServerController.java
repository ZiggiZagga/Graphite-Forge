package com.example.configserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * REST Controller implementing Spring Cloud Config Server protocol.
 * 
 * Provides endpoints following the standard format:
 * /config/{application}/{profile}
 * 
 * Compatible with Spring Cloud Config clients.
 */
@Slf4j
@RestController
@RequestMapping("/config")
public class ConfigServerController {
    
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    
    public ConfigServerController(ConfigService configService) {
        this.configService = configService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get configuration for an application and profile (environment).
     * Returns PropertySource in Spring Cloud Config format.
     * 
     * GET /config/{application}/{profile}
     */
    @GetMapping("/{application}/{profile}")
    public Mono<ResponseEntity<PropertySourceResponse>> getConfig(
            @PathVariable String application,
            @PathVariable String profile) {
        
        log.info("Fetching config for application={}, profile={}", application, profile);
        
        return configService.getApplicationConfigs(application, profile)
            .collectMap(ConfigItem::key, ConfigItem::value)
            .map(properties -> new PropertySourceResponse(
                application,
                profile,
                Collections.singletonList(new PropertySource((Map<String, Object>) (Object) properties))
            ))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    /**
     * Get all configurations for an environment (profile).
     * 
     * GET /config/env/{profile}
     */
    @GetMapping("/env/{profile}")
    public Mono<ResponseEntity<Map<String, Object>>> getEnvironmentConfigs(
            @PathVariable String profile) {
        
        log.info("Fetching all configs for profile={}", profile);
        
        return configService.getConfigsByEnvironment(profile)
            .collectMap(ConfigItem::key, ConfigItem::value)
            .map(config -> {
                Map<String, Object> response = new HashMap<>();
                response.put("environment", profile);
                response.put("properties", config);
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Get a specific configuration value.
     * 
     * GET /config/{key}/{profile}
     */
    @GetMapping("/value/{profile}/{key}")
    public Mono<ResponseEntity<Map<String, String>>> getConfigValue(
            @PathVariable String key,
            @PathVariable String profile) {
        
        log.info("Fetching config value: key={}, profile={}", key, profile);
        
        return configService.getConfig(key, profile)
            .map(item -> {
                Map<String, String> response = new HashMap<>();
                response.put("key", item.key());
                response.put("value", item.value());
                response.put("encrypted", String.valueOf(item.isEncrypted()));
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Create a new configuration item.
     * 
     * POST /config
     */
    @PostMapping
    public Mono<ResponseEntity<ConfigItem>> createConfig(
            @RequestBody CreateConfigRequest request) {
        
        log.info("Creating config: key={}, environment={}", request.key(), request.environment());
        
        return configService.createConfig(
            request.key(),
            request.value(),
            request.environment(),
            request.parentId(),
            request.description()
        )
        .map(ResponseEntity::ok)
        .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
    
    /**
     * Update an existing configuration item.
     * 
     * PUT /config/{id}
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ConfigItem>> updateConfig(
            @PathVariable String id,
            @RequestBody UpdateConfigRequest request) {
        
        log.info("Updating config: id={}", id);
        
        return configService.updateConfig(id, request.value(), request.description())
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    /**
     * Toggle configuration enabled state.
     * 
     * PATCH /config/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<ConfigItem>> toggleConfig(
            @PathVariable String id,
            @RequestParam Boolean enabled) {
        
        log.info("Toggling config: id={}, enabled={}", id, enabled);
        
        return configService.toggleConfig(id, enabled)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    /**
     * Move configuration to a new parent.
     * 
     * PATCH /config/{id}/move
     */
    @PatchMapping("/{id}/move")
    public Mono<ResponseEntity<ConfigItem>> moveConfig(
            @PathVariable String id,
            @RequestParam String parentId) {
        
        log.info("Moving config: id={}, newParent={}", id, parentId);
        
        return configService.moveConfig(id, parentId)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    /**
     * Delete a configuration item.
     * 
     * DELETE /config/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteConfig(@PathVariable String id) {
        log.info("Deleting config: id={}", id);
        
        return configService.deleteConfig(id)
            .map(_ -> ResponseEntity.ok().<Void>build())
            .onErrorReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    
    /**
     * Get configuration children.
     * 
     * GET /config/{parentId}/children
     */
    @GetMapping("/{parentId}/children")
    public Mono<ResponseEntity<Map<String, Object>>> getChildren(
            @PathVariable String parentId) {
        
        log.info("Fetching children for parent={}", parentId);
        
        return configService.getChildren(parentId)
            .collectList()
            .map(children -> {
                Map<String, Object> response = new HashMap<>();
                response.put("parentId", parentId);
                response.put("children", children);
                return ResponseEntity.ok(response);
            });
    }
    
    /**
     * Health check endpoint.
     * 
     * GET /config/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "config-server");
        return ResponseEntity.ok(response);
    }
    
    // ============================================================================
    // Request/Response DTOs
    // ============================================================================
    
    public record CreateConfigRequest(
            String key,
            String value,
            String environment,
            String parentId,
            String description
    ) {}
    
    public record UpdateConfigRequest(
            String value,
            String description
    ) {}
    
    public record PropertySourceResponse(
            String application,
            String profile,
            List<PropertySource> propertySources
    ) {}
    
    public record PropertySource(
            Map<String, Object> source
    ) {
        public PropertySource(Map<String, Object> source) {
            this.source = source;
        }
    }
}
