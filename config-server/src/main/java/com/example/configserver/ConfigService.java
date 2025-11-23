package com.example.configserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service layer for configuration management.
 * Handles CRUD operations and encryption/decryption.
 * Note: Spring Cloud Bus integration will be added in a future release.
 */
@Slf4j
@Service
public class ConfigService {
    
    private final ConfigRepository configRepository;
    private final ConfigEncryptionService encryptionService;
    
    public ConfigService(ConfigRepository configRepository,
                        ConfigEncryptionService encryptionService) {
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Create a new configuration item.
     * Automatically encrypts sensitive values.
     */
    public Mono<ConfigItem> createConfig(String key, String value, String environment,
                                        String parentId, String description) {
        return Mono.fromCallable(() -> {
            String id = UUID.randomUUID().toString();
            boolean shouldEncrypt = isSensitiveKey(key);
            String encryptedValue = shouldEncrypt ? encryptionService.encrypt(value) : value;
            
            return new ConfigItem(
                id, key, encryptedValue, environment,
                parentId, true, description, shouldEncrypt,
                LocalDateTime.now(), LocalDateTime.now()
            );
        })
        .flatMap(configRepository::save)
        .doOnSuccess(saved -> {
            log.info("Created config item: key={}, environment={}, encrypted={}", 
                     key, environment, saved.isEncrypted());
        })
        .doOnError(error -> log.error("Error creating config item: {}", key, error));
    }
    
    /**
     * Get configuration by key and environment.
     * Automatically decrypts sensitive values.
     */
    public Mono<ConfigItem> getConfig(String key, String environment) {
        return configRepository.findByKeyAndEnvironment(key, environment)
            .map(this::decryptIfNeeded)
            .doOnError(error -> log.error("Error retrieving config: key={}, env={}", key, environment, error));
    }
    
    /**
     * Get all configurations for an environment.
     * Includes only root-level items (top-level applications).
     */
    public Flux<ConfigItem> getConfigsByEnvironment(String environment) {
        return configRepository.findRootItemsByEnvironment(environment)
            .map(this::decryptIfNeeded)
            .doOnError(error -> log.error("Error retrieving configs for environment: {}", environment, error));
    }
    
    /**
     * Get all configurations for an application in a specific environment.
     * Recursively retrieves the entire configuration tree.
     */
    public Flux<ConfigItem> getApplicationConfigs(String applicationId, String environment) {
        return configRepository.findApplicationConfigsByEnvironment(applicationId, environment)
            .map(this::decryptIfNeeded)
            .doOnError(error -> log.error("Error retrieving application configs: app={}, env={}", applicationId, environment, error));
    }
    
    /**
     * Get children configuration items for a parent.
     */
    public Flux<ConfigItem> getChildren(String parentId) {
        return configRepository.findByParentId(parentId)
            .map(this::decryptIfNeeded)
            .doOnError(error -> log.error("Error retrieving children for parent: {}", parentId, error));
    }
    
    /**
     * Update a configuration item.
     * Publishes refresh event to notify clients.
     */
    public Mono<ConfigItem> updateConfig(String id, String value, String description) {
        return configRepository.findById(id)
            .flatMap(existing -> {
                boolean shouldEncrypt = isSensitiveKey(existing.key());
                String encryptedValue = shouldEncrypt ? encryptionService.encrypt(value) : value;
                
                ConfigItem updated = new ConfigItem(
                    existing.id(),
                    existing.key(),
                    encryptedValue,
                    existing.environment(),
                    existing.parentId(),
                    existing.enabled(),
                    description != null ? description : existing.description(),
                    shouldEncrypt,
                    existing.createdAt(),
                    LocalDateTime.now()
                );
                
                return configRepository.save(updated);
            })
            .doOnSuccess(updated -> {
                log.info("Updated config item: id={}, key={}", id, updated.key());
            })
            .doOnError(error -> log.error("Error updating config: {}", id, error));
    }
    
    /**
     * Delete a configuration item.
     * Cascades to all children.
     */
    public Mono<Void> deleteConfig(String id) {
        return configRepository.findById(id)
            .flatMap(existing -> {
                // Delete all children first
                return configRepository.deleteByParentId(id)
                    .then(configRepository.deleteById(id))
                    .then(Mono.fromRunnable(() -> {
                        log.info("Deleted config item: id={}, key={}", id, existing.key());
                    }))
                    .then();
            })
            .doOnError(error -> log.error("Error deleting config: {}", id, error));
    }
    
    /**
     * Enable or disable a configuration item.
     */
    public Mono<ConfigItem> toggleConfig(String id, Boolean enabled) {
        return configRepository.findById(id)
            .flatMap(existing -> {
                ConfigItem updated = new ConfigItem(
                    existing.id(),
                    existing.key(),
                    existing.value(),
                    existing.environment(),
                    existing.parentId(),
                    enabled,
                    existing.description(),
                    existing.isEncrypted(),
                    existing.createdAt(),
                    LocalDateTime.now()
                );
                
                return configRepository.save(updated);
            })
            .doOnSuccess(updated -> {
                log.info("Toggled config item: id={}, enabled={}", id, enabled);
            });
    }
    
    /**
     * Get all enabled configurations for an environment.
     */
    public Flux<ConfigItem> getEnabledConfigs(String environment) {
        return configRepository.findEnabledByEnvironment(environment)
            .map(this::decryptIfNeeded);
    }
    
    /**
     * Check if a configuration key exists in an environment.
     */
    public Mono<Boolean> configExists(String key, String environment) {
        return configRepository.countByKeyAndEnvironment(key, environment)
            .map(count -> count > 0);
    }
    
    /**
     * Move a configuration to a new parent.
     */
    public Mono<ConfigItem> moveConfig(String id, String newParentId) {
        return configRepository.findById(id)
            .flatMap(existing -> {
                ConfigItem updated = new ConfigItem(
                    existing.id(),
                    existing.key(),
                    existing.value(),
                    existing.environment(),
                    newParentId,
                    existing.enabled(),
                    existing.description(),
                    existing.isEncrypted(),
                    existing.createdAt(),
                    LocalDateTime.now()
                );
                
                return configRepository.save(updated);
            })
            .doOnSuccess(moved -> {
                log.info("Moved config item: id={}, newParent={}", id, newParentId);
            });
    }
    
    /**
     * Decrypt a configuration item if it's encrypted.
     * Internal helper method.
     */
    private ConfigItem decryptIfNeeded(ConfigItem item) {
        if (!item.isEncrypted()) {
            return item;
        }
        
        try {
            String decryptedValue = encryptionService.decrypt(item.value());
            return new ConfigItem(
                item.id(),
                item.key(),
                decryptedValue,
                item.environment(),
                item.parentId(),
                item.enabled(),
                item.description(),
                item.isEncrypted(),
                item.createdAt(),
                item.updatedAt()
            );
        } catch (Exception e) {
            log.error("Error decrypting config: id={}, key={}", item.id(), item.key(), e);
            return item;
        }
    }
    
    /**
     * Check if a configuration key is sensitive and should be encrypted.
     */
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("token") ||
               lowerKey.contains("apikey") ||
               lowerKey.contains("api_key");
    }
}
