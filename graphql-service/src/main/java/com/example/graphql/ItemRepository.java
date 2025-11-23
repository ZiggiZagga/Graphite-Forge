package com.example.graphql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for hierarchical Item entities.
 *
 * <p>Extends ReactiveCrudRepository to provide non-blocking CRUD operations
 * for Item records. Includes custom queries for hierarchical relationships.</p>
 */
public interface ItemRepository extends ReactiveCrudRepository<Item, String> {
    
    /**
     * Find all children of a parent item.
     * 
     * @param parentId the parent item ID
     * @return Flux of child items
     */
    Flux<Item> findByParentId(String parentId);

    /**
     * Find all root items (items without parent).
     * 
     * @return Flux of root items ordered by creation date
     */
    @Query("SELECT * FROM items WHERE parent_id IS NULL ORDER BY created_at DESC")
    Flux<Item> findRootItems();

    /**
     * Check if an item can be set as a child of another.
     * Prevents circular references (item cannot be parent of itself or ancestor).
     * 
     * @param itemId the item to move
     * @param potentialParentId the potential new parent
     * @return Mono containing true if valid parent, false otherwise
     */
    @Query("SELECT CASE WHEN EXISTS(" +
           "  WITH RECURSIVE ancestor_chain AS (" +
           "    SELECT id, parent_id FROM items WHERE id = :potentialParentId" +
           "    UNION ALL" +
           "    SELECT i.id, i.parent_id FROM items i" +
           "    INNER JOIN ancestor_chain ac ON i.id = ac.parent_id" +
           "  )" +
           "  SELECT 1 FROM ancestor_chain WHERE id = :itemId" +
           ") THEN false ELSE true END")
    Mono<Boolean> isValidParent(String itemId, String potentialParentId);

    /**
     * Count all children of a parent (recursive count).
     * 
     * @param parentId the parent item ID
     * @return Mono with count of all descendants
     */
    @Query("SELECT COUNT(*) FROM items WHERE parent_id = :parentId")
    Mono<Long> countChildren(String parentId);
}
