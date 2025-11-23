
package com.example.graphql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GraphQL Controller for managing Item entities.
 *
 * <p>Exposes queries and mutations for CRUD operations on items, with feature toggles
 * for each operation. Uses reactive programming for non-blocking data access.
 * Delegates business logic to the ItemService layer.</p>
 */
@Controller
public class ItemGraphqlController {

    /** Service for Item business logic. */
    @Autowired
    private ItemService service;

    /**
     * Returns all items if read is enabled.
     *
     * @return Flux of Item
     */
    @QueryMapping
    public Flux<Item> items() {
        return service.getAllItems();
    }

    /**
     * Returns a single item by ID if read is enabled.
     *
     * @param id the item ID
     * @return Mono of Item or error if not found
     */
    @QueryMapping
    public Mono<Item> itemById(@Argument String id) {
        return service.getItemById(id);
    }

    /**
     * Creates a new item if create is enabled.
     *
     * @param name item name (required)
     * @param description item description (optional)
     * @return Mono of created Item
     */
    @MutationMapping
    public Mono<Item> createItem(@Argument String name, @Argument String description) {
        if (name == null || name.isBlank()) {
            return Mono.error(new IllegalArgumentException("Item name is required and cannot be blank"));
        }
        Item item = new Item(null, name, description);
        return service.createItem(item);
    }

    /**
     * Updates an existing item if update is enabled.
     *
     * @param id item ID
     * @param name new name (optional)
     * @param description new description (optional)
     * @return Mono of updated Item
     */
    @MutationMapping
    public Mono<Item> updateItem(@Argument String id, @Argument String name,
                                 @Argument String description) {
        if (id == null || id.isBlank()) {
            return Mono.error(new IllegalArgumentException("Item ID is required and cannot be blank"));
        }
        return service.updateItem(id, name, description);
    }

    /**
     * Deletes an item by ID if delete is enabled.
     *
     * @param id item ID
     * @return Mono of true if deleted
     */
    @MutationMapping
    public Mono<Boolean> deleteItem(@Argument String id) {
        if (id == null || id.isBlank()) {
            return Mono.error(new IllegalArgumentException("Item ID is required and cannot be blank"));
        }
        return service.deleteItem(id);
    }
}
