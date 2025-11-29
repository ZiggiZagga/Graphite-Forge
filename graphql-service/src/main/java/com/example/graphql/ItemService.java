package com.example.graphql;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service layer for Item business logic.
 *
 * <p>Encapsulates CRUD operations, validation, and feature toggle checks.
 * Provides a clean separation between the GraphQL controller and repository layer.</p>
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository repo;

    @Autowired
    private CrudFeatures features;

    /**
     * Retrieves all items if read is enabled.
     *
     * @return Flux of items
     * @throws IllegalStateException if read operation is disabled
     */
    public Flux<Item> getAllItems() {
        if (!features.isReadEnabled()) {
            return Flux.error(new ItemOperationDisabledException("Read operation is disabled"));
        }
        return repo.findAll()
                .onErrorResume(e -> Flux.error(new ItemDatabaseException("Failed to retrieve items", e)));
    }

    /**
     * Retrieves a single item by ID if read is enabled.
     *
     * @param id the item ID (must not be blank)
     * @return Mono of Item
     * @throws IllegalStateException if read operation is disabled
     * @throws ItemNotFoundException if item not found
     * @throws ItemDatabaseException if database error occurs
     */
    public Mono<Item> getItemById(@NotBlank(message = "Item ID cannot be blank") String id) {
        if (!features.isReadEnabled()) {
            return Mono.error(new ItemOperationDisabledException("Read operation is disabled"));
        }
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .onErrorMap(e -> {
                    if (e instanceof ItemNotFoundException) {
                        return e;  // Preserve ItemNotFoundException without wrapping
                    }
                    return new ItemDatabaseException("Failed to retrieve item: " + id, e);
                });
    }

    /**
     * Creates a new item if create is enabled.
     *
     * @param item the item to create (must be valid)
     * @return Mono of created Item
     * @throws IllegalStateException if create operation is disabled
     * @throws ItemDatabaseException if database error occurs
     */
    public Mono<Item> createItem(@Valid Item item) {
        if (!features.isCreateEnabled()) {
            return Mono.error(new ItemOperationDisabledException("Create operation is disabled"));
        }
        // Create with null ID so database generates it
        Item newItem = new Item(null, item.name(), item.description());
        return repo.save(newItem)
                .onErrorResume(e -> Mono.error(new ItemDatabaseException("Failed to create item", e)));
    }

    /**
     * Updates an existing item if update is enabled.
     *
     * @param id item ID (must not be blank)
     * @param name new name (optional)
     * @param description new description (optional)
     * @return Mono of updated Item
     * @throws IllegalStateException if update operation is disabled
     * @throws ItemNotFoundException if item not found
     * @throws ItemDatabaseException if database error occurs
     */
    public Mono<Item> updateItem(
            @NotBlank(message = "Item ID cannot be blank") String id,
            String name,
            String description) {
        if (!features.isUpdateEnabled()) {
            return Mono.error(new ItemOperationDisabledException("Update operation is disabled"));
        }

        return repo.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(id)))
                .map(existingItem -> new Item(
                        existingItem.id(),
                        name != null && !name.isBlank() ? name : existingItem.name(),
                        description != null ? description : existingItem.description()
                ))
                .flatMap(updatedItem -> repo.save(updatedItem)
                        .onErrorResume(e -> Mono.error(new ItemDatabaseException("Failed to update item: " + id, e))))
                .onErrorResume(ItemNotFoundException.class, e -> Mono.error(e));
    }

    /**
     * Deletes an item by ID if delete is enabled.
     *
     * @param id item ID (must not be blank)
     * @return Mono of true if deleted
     * @throws IllegalStateException if delete operation is disabled
     * @throws ItemNotFoundException if item not found
     * @throws ItemDatabaseException if database error occurs
     */
    public Mono<Boolean> deleteItem(@NotBlank(message = "Item ID cannot be blank") String id) {
        if (!features.isDeleteEnabled()) {
            return Mono.error(new ItemOperationDisabledException("Delete operation is disabled"));
        }

        return repo.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ItemNotFoundException(id));
                    }
                    return repo.deleteById(id)
                            .thenReturn(true)
                            .onErrorResume(e -> Mono.error(new ItemDatabaseException("Failed to delete item: " + id, e)));
                });
    }

    /**
     * Retrieves all root items (items without parent) if read is enabled.
     *
     * @return Flux of root items
     * @throws IllegalStateException if read operation is disabled
     */
    public Flux<Item> getRootItems() {
        if (!features.isReadEnabled()) {
            return Flux.error(new ItemOperationDisabledException("Read operation is disabled"));
        }
        return repo.findRootItems()
                .onErrorResume(e -> Flux.error(new ItemDatabaseException("Failed to retrieve root items", e)));
    }

    /**
     * Retrieves all child items of a parent if read is enabled.
     *
     * @param parentId the parent item ID (must not be blank)
     * @return Flux of child items
     * @throws IllegalStateException if read operation is disabled
     * @throws ItemDatabaseException if database error occurs
     */
    public Flux<Item> getChildrenByParent(@NotBlank(message = "Parent ID cannot be blank") String parentId) {
        if (!features.isReadEnabled()) {
            return Flux.error(new ItemOperationDisabledException("Read operation is disabled"));
        }
        return repo.findByParentId(parentId)
                .onErrorResume(e -> Flux.error(new ItemDatabaseException("Failed to retrieve children for parent: " + parentId, e)));
    }

    /**
     * Moves an item to a new parent if update is enabled.
     * Prevents circular references.
     *
     * @param itemId the item to move (must not be blank)
     * @param parentId the new parent ID (must not be blank)
     * @return Mono of updated Item
     * @throws IllegalStateException if update operation is disabled
     * @throws ItemNotFoundException if item not found
     * @throws IllegalArgumentException if circular reference detected
     * @throws ItemDatabaseException if database error occurs
     */
    public Mono<Item> moveItemToParent(
            @NotBlank(message = "Item ID cannot be blank") String itemId,
            @NotBlank(message = "Parent ID cannot be blank") String parentId) {
        if (!features.isUpdateEnabled()) {
            return Mono.error(new ItemOperationDisabledException("Update operation is disabled"));
        }

        // Prevent self-referencing
        if (itemId.equals(parentId)) {
            return Mono.error(new IllegalArgumentException("An item cannot be its own parent"));
        }

        return repo.findById(itemId)
                .switchIfEmpty(Mono.error(new ItemNotFoundException(itemId)))
                .flatMap(existingItem -> 
                    // Check for circular reference
                    repo.isValidParent(itemId, parentId)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                return Mono.error(new IllegalArgumentException(
                                    "Cannot move item to this parent: would create circular reference"
                                ));
                            }
                            // Create updated item with new parent
                            Item movedItem = new Item(existingItem.id(), existingItem.name(), 
                                                     existingItem.description(), parentId);
                            return repo.save(movedItem);
                        })
                )
                .onErrorResume(ItemNotFoundException.class, e -> Mono.error(e))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(e))
                .onErrorResume(e -> Mono.error(new ItemDatabaseException("Failed to move item: " + itemId, e)));
    }
}
