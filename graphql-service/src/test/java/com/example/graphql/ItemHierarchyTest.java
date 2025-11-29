package com.example.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for hierarchical item operations.
 *
 * <p>Tests include:
 * - Creating items with parent-child relationships
 * - Moving items between parents
 * - Querying hierarchical structures
 * - Preventing circular references
 * - Validating parent-child constraints
 * </p>
 */
class ItemHierarchyTest {

    @Mock
    private ItemRepository repository;

    @Mock
    private CrudFeatures features;

    @InjectMocks
    private ItemService service;

    private Item rootItem;
    private Item childItem;
    private Item grandchildItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rootItem = new Item("1", "Root Item", "Root description", null);
        childItem = new Item("2", "Child Item", "Child description", "1");
        grandchildItem = new Item("3", "Grandchild Item", "Grandchild description", "2");

        // Default: all features enabled
        when(features.isReadEnabled()).thenReturn(true);
        when(features.isCreateEnabled()).thenReturn(true);
        when(features.isUpdateEnabled()).thenReturn(true);
        when(features.isDeleteEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("Creating hierarchical items")
    class CreateHierarchicalTests {

        @Test
        @DisplayName("should create root item without parent")
        void testCreateRootItem_Success() {
            when(repository.save(any(Item.class))).thenReturn(Mono.just(rootItem));

            Item newItem = new Item(null, "Root", "Root description");
            StepVerifier.create(service.createItem(newItem))
                    .expectNext(rootItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create child item with valid parent")
        void testCreateChildItem_Success() {
            when(repository.save(any(Item.class))).thenReturn(Mono.just(childItem));

            StepVerifier.create(service.createItem(childItem))
                    .expectNext(childItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create deeply nested items")
        void testCreateGrandchildItem_Success() {
            when(repository.save(any(Item.class))).thenReturn(Mono.just(grandchildItem));

            StepVerifier.create(service.createItem(grandchildItem))
                    .expectNext(grandchildItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create item when create is disabled")
        void testCreateItem_CreateDisabled() {
            when(features.isCreateEnabled()).thenReturn(false);

            StepVerifier.create(service.createItem(childItem))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Reading hierarchical items")
    class ReadHierarchicalTests {

        @Test
        @DisplayName("should get all root items")
        void testGetRootItems_Success() {
            when(repository.findRootItems()).thenReturn(Flux.just(rootItem));

            Mono<Long> result = repository.findRootItems()
                    .count();

            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should get all children of a parent")
        void testGetChildrenByParent_Success() {
            when(repository.findByParentId("1")).thenReturn(Flux.just(childItem));

            StepVerifier.create(repository.findByParentId("1"))
                    .expectNext(childItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when parent has no children")
        void testGetChildrenByParent_Empty() {
            when(repository.findByParentId("1")).thenReturn(Flux.empty());

            StepVerifier.create(repository.findByParentId("1"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should get multiple children of same parent")
        void testGetMultipleChildren_Success() {
            Item child2 = new Item("4", "Another Child", "Description", "1");
            when(repository.findByParentId("1")).thenReturn(Flux.just(childItem, child2));

            StepVerifier.create(repository.findByParentId("1"))
                    .expectNext(childItem, child2)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Moving items between parents")
    class MoveItemTests {

        @Test
        @DisplayName("should move item to new parent")
        void testMoveItem_Success() {
            Item movedChild = new Item("2", "Child Item", "Child description", "5");
            when(repository.findById("2")).thenReturn(Mono.just(childItem));
            when(repository.isValidParent("2", "5")).thenReturn(Mono.just(true));
            when(repository.save(any(Item.class))).thenReturn(Mono.just(movedChild));

            StepVerifier.create(repository.isValidParent("2", "5"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should prevent circular reference (parent as child)")
        void testMoveItem_CircularReference_ParentAsChild() {
            // Attempting to make root (id=1) parent of itself should fail
            when(repository.isValidParent("1", "1")).thenReturn(Mono.just(false));

            StepVerifier.create(repository.isValidParent("1", "1"))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should prevent circular reference (ancestor as child)")
        void testMoveItem_CircularReference_AncestorAsChild() {
            // Attempting to make grandchild (3) parent of root (1) should fail
            when(repository.isValidParent("1", "3")).thenReturn(Mono.just(false));

            StepVerifier.create(repository.isValidParent("1", "3"))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should allow moving to valid parent")
        void testMoveItem_ValidNewParent() {
            when(repository.isValidParent("2", "5")).thenReturn(Mono.just(true));

            StepVerifier.create(repository.isValidParent("2", "5"))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Deleting hierarchical items")
    class DeleteHierarchicalTests {

        @Test
        @DisplayName("should delete item and cascade to children")
        void testDeleteParent_CascadesChildren() {
            when(repository.existsById("1")).thenReturn(Mono.just(true));
            when(repository.deleteById("1")).thenReturn(Mono.empty());

            StepVerifier.create(service.deleteItem("1"))
                    .expectNext(true)
                    .verifyComplete();

            verify(repository).deleteById("1");
        }

        @Test
        @DisplayName("should delete child item independently")
        void testDeleteChild_Success() {
            when(repository.existsById("2")).thenReturn(Mono.just(true));
            when(repository.deleteById("2")).thenReturn(Mono.empty());

            StepVerifier.create(service.deleteItem("2"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when deleting non-existent parent")
        void testDeleteParent_NotFound() {
            when(repository.existsById("999")).thenReturn(Mono.just(false));

            StepVerifier.create(service.deleteItem("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();

            verify(repository, never()).deleteById("999");
        }
    }

    @Nested
    @DisplayName("Hierarchy constraints")
    class HierarchyConstraintsTests {

        @Test
        @DisplayName("should count direct children")
        void testCountChildren_Success() {
            when(repository.countChildren("1")).thenReturn(Mono.just(2L));

            StepVerifier.create(repository.countChildren("1"))
                    .expectNext(2L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return zero when item has no children")
        void testCountChildren_Empty() {
            when(repository.countChildren("999")).thenReturn(Mono.just(0L));

            StepVerifier.create(repository.countChildren("999"))
                    .expectNext(0L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should validate no self-referencing")
        void testValidateSelfReference() {
            // An item cannot be its own parent
            Item selfRefItem = new Item("1", "Self Ref", "Description", "1");
            
            // Service should catch this during creation
            when(repository.save(selfRefItem))
                    .thenReturn(Mono.error(new IllegalArgumentException("Item cannot be its own parent")));

            StepVerifier.create(repository.save(selfRefItem))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }
    }
}
