
package com.example.graphql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for Item entities.
 *
 * <p>Extends ReactiveCrudRepository to provide non-blocking CRUD operations
 * for Item records. Custom queries can be added as needed.</p>
 */
public interface ItemRepository extends ReactiveCrudRepository<Item, String> {
    // The following methods are already provided by ReactiveCrudRepository:
    // Flux<Item> findAll();
    // Mono<Item> findById(String id);
    // Add custom queries here if needed.
}