package com.example.configserver;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for ConfigItem entities.
 * Provides custom queries for environment-specific and hierarchical config retrieval.
 */
public interface ConfigRepository extends R2dbcRepository<ConfigItem, String> {
    
    /**
     * Find all config items for a specific environment.
     */
    Flux<ConfigItem> findByEnvironment(String environment);
    
    /**
     * Find all config items for a specific application (parent).
     */
    Flux<ConfigItem> findByParentId(String parentId);
    
    /**
     * Find all root-level config items (no parent).
     */
    @Query("SELECT * FROM config_items WHERE parent_id IS NULL OR parent_id = '' ORDER BY key ASC")
    Flux<ConfigItem> findRootItems();
    
    /**
     * Find all root-level config items for a specific environment.
     */
    @Query("SELECT * FROM config_items WHERE (parent_id IS NULL OR parent_id = '') " +
           "AND environment = :environment ORDER BY key ASC")
    Flux<ConfigItem> findRootItemsByEnvironment(@Param("environment") String environment);
    
    /**
     * Find config items by environment and parent.
     */
    @Query("SELECT * FROM config_items WHERE environment = :environment AND parent_id = :parentId ORDER BY key ASC")
    Flux<ConfigItem> findByEnvironmentAndParent(@Param("environment") String environment,
                                                @Param("parentId") String parentId);
    
    /**
     * Find config by key and environment.
     */
    @Query("SELECT * FROM config_items WHERE key = :key AND environment = :environment LIMIT 1")
    Mono<ConfigItem> findByKeyAndEnvironment(@Param("key") String key,
                                             @Param("environment") String environment);
    
    /**
     * Find all configs for an application in a specific environment.
     * Traverses the hierarchy to get all descendant configs.
     */
    @Query("WITH RECURSIVE config_tree AS (" +
           "  SELECT * FROM config_items WHERE parent_id = :applicationId " +
           "  UNION ALL " +
           "  SELECT c.* FROM config_items c " +
           "  INNER JOIN config_tree ct ON c.parent_id = ct.id " +
           ") " +
           "SELECT * FROM config_tree WHERE environment = :environment ORDER BY key ASC")
    Flux<ConfigItem> findApplicationConfigsByEnvironment(@Param("applicationId") String applicationId,
                                                         @Param("environment") String environment);
    
    /**
     * Find all enabled config items for an environment.
     */
    @Query("SELECT * FROM config_items WHERE environment = :environment AND enabled = true ORDER BY key ASC")
    Flux<ConfigItem> findEnabledByEnvironment(@Param("environment") String environment);
    
    /**
     * Check if a config key exists for an environment.
     */
    @Query("SELECT COUNT(*) FROM config_items WHERE key = :key AND environment = :environment")
    Mono<Long> countByKeyAndEnvironment(@Param("key") String key,
                                        @Param("environment") String environment);
    
    /**
     * Find all sensitive (encrypted) configs for an environment.
     */
    @Query("SELECT * FROM config_items WHERE environment = :environment AND is_encrypted = true ORDER BY key ASC")
    Flux<ConfigItem> findEncryptedByEnvironment(@Param("environment") String environment);
    
    /**
     * Delete all configs for a parent (cascade).
     */
    @Query("DELETE FROM config_items WHERE parent_id = :parentId")
    Mono<Integer> deleteByParentId(@Param("parentId") String parentId);
}
