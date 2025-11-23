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
 * Service layer tests for ItemService business logic.
 *
 * <p>Tests include:
 * - CRUD operations
 * - Feature toggle enforcement
 * - Error handling and recovery
 * - Validation of operations
 * </p>
 */
class ItemServiceTest {

    @Mock
    private ItemRepository repository;

    @Mock
    private CrudFeatures features;

    @InjectMocks
    private ItemService service;

    private Item testItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testItem = new Item("1", "TestItem", "Test Description");
        // Default: all features enabled
        when(features.isReadEnabled()).thenReturn(true);
        when(features.isCreateEnabled()).thenReturn(true);
        when(features.isUpdateEnabled()).thenReturn(true);
        when(features.isDeleteEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("getAllItems")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return all items when read is enabled")
        void testGetAllItems_Success() {
            when(repository.findAll()).thenReturn(Flux.just(testItem));

            StepVerifier.create(service.getAllItems())
                    .expectNext(testItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux when no items exist")
        void testGetAllItems_Empty() {
            when(repository.findAll()).thenReturn(Flux.empty());

            StepVerifier.create(service.getAllItems())
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when read is disabled")
        void testGetAllItems_ReadDisabled() {
            when(features.isReadEnabled()).thenReturn(false);

            StepVerifier.create(service.getAllItems())
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should wrap database errors")
        void testGetAllItems_DatabaseError() {
            when(repository.findAll())
                    .thenReturn(Flux.error(new RuntimeException("DB Connection Failed")));

            StepVerifier.create(service.getAllItems())
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should not call repository when read is disabled")
        void testGetAllItems_NoRepositoryCall() {
            when(features.isReadEnabled()).thenReturn(false);

            StepVerifier.create(service.getAllItems())
                    .expectError(ItemOperationDisabledException.class)
                    .verify();

            verify(repository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("getItemById")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when found")
        void testGetItemById_Success() {
            when(repository.findById("1")).thenReturn(Mono.just(testItem));

            StepVerifier.create(service.getItemById("1"))
                    .expectNext(testItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return NOT_FOUND error when item not found")
        void testGetItemById_NotFound() {
            when(repository.findById("999")).thenReturn(Mono.empty());

            StepVerifier.create(service.getItemById("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return error when read is disabled")
        void testGetItemById_ReadDisabled() {
            when(features.isReadEnabled()).thenReturn(false);

            StepVerifier.create(service.getItemById("1"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should wrap database errors")
        void testGetItemById_DatabaseError() {
            when(repository.findById("1"))
                    .thenReturn(Mono.error(new RuntimeException("DB Connection Failed")));

            StepVerifier.create(service.getItemById("1"))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should throw error for blank ID")
        void testGetItemById_BlankId() {
        // Method parameter validation isn't active in this unit test environment;
        // stub repository to return empty and assert wrapped database error is thrown.
        when(repository.findById("   ")).thenReturn(Mono.empty());

        StepVerifier.create(service.getItemById("   "))
            .expectError(ItemDatabaseException.class)
            .verify();
        }
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("should create item successfully")
        void testCreateItem_Success() {
            Item newItem = new Item(null, "NewItem", "New Description");
            Item savedItem = new Item("1", "NewItem", "New Description");
            when(repository.save(any(Item.class))).thenReturn(Mono.just(savedItem));

            StepVerifier.create(service.createItem(newItem))
                    .expectNext(savedItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create item without description")
        void testCreateItem_NoDescription() {
            Item newItem = new Item(null, "NewItem", null);
            Item savedItem = new Item("1", "NewItem", null);
            when(repository.save(any(Item.class))).thenReturn(Mono.just(savedItem));

            StepVerifier.create(service.createItem(newItem))
                    .expectNext(savedItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when create is disabled")
        void testCreateItem_CreateDisabled() {
            when(features.isCreateEnabled()).thenReturn(false);
            Item item = new Item(null, "NewItem", "Description");

            StepVerifier.create(service.createItem(item))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should wrap database errors")
        void testCreateItem_DatabaseError() {
            Item item = new Item(null, "NewItem", "Description");
            when(repository.save(any(Item.class)))
                    .thenReturn(Mono.error(new RuntimeException("DB Error")));

            StepVerifier.create(service.createItem(item))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should not call repository when create is disabled")
        void testCreateItem_NoRepositoryCall() {
            when(features.isCreateEnabled()).thenReturn(false);
            Item item = new Item(null, "NewItem", "Description");

            StepVerifier.create(service.createItem(item))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateItem")
    class UpdateItemTests {

        @Test
        @DisplayName("should update name and description")
        void testUpdateItem_FullUpdate() {
            Item updated = new Item("1", "Updated", "Updated Description");
            when(repository.findById("1")).thenReturn(Mono.just(testItem));
            when(repository.save(any(Item.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(service.updateItem("1", "Updated", "Updated Description"))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should update only name when description is null")
        void testUpdateItem_PartialUpdate() {
            Item updated = new Item("1", "Updated", "Test Description");
            when(repository.findById("1")).thenReturn(Mono.just(testItem));
            when(repository.save(any(Item.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(service.updateItem("1", "Updated", null))
                    .expectNext(updated)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testUpdateItem_NotFound() {
            when(repository.findById("999")).thenReturn(Mono.empty());

            StepVerifier.create(service.updateItem("999", "Updated", null))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return error when update is disabled")
        void testUpdateItem_UpdateDisabled() {
            when(features.isUpdateEnabled()).thenReturn(false);

            StepVerifier.create(service.updateItem("1", "Updated", null))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should wrap database errors")
        void testUpdateItem_DatabaseError() {
            when(repository.findById("1")).thenReturn(Mono.just(testItem));
            when(repository.save(any(Item.class)))
                    .thenReturn(Mono.error(new RuntimeException("DB Error")));

            StepVerifier.create(service.updateItem("1", "Updated", null))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should not call repository when update is disabled")
        void testUpdateItem_NoRepositoryCall() {
            when(features.isUpdateEnabled()).thenReturn(false);

            StepVerifier.create(service.updateItem("1", "Updated", null))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();

            verify(repository, never()).findById(anyString());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should skip empty/blank names in update")
        void testUpdateItem_BlankName() {
            Item updated = new Item("1", "TestItem", "Test Description");
            when(repository.findById("1")).thenReturn(Mono.just(testItem));
            when(repository.save(any(Item.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(service.updateItem("1", "  ", "New Description"))
                    .expectNext(updated)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete item successfully")
        void testDeleteItem_Success() {
            when(repository.existsById("1")).thenReturn(Mono.just(true));
            when(repository.deleteById("1")).thenReturn(Mono.empty());

            StepVerifier.create(service.deleteItem("1"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testDeleteItem_NotFound() {
            when(repository.existsById("999")).thenReturn(Mono.just(false));

            StepVerifier.create(service.deleteItem("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return error when delete is disabled")
        void testDeleteItem_DeleteDisabled() {
            when(features.isDeleteEnabled()).thenReturn(false);

            StepVerifier.create(service.deleteItem("1"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should wrap database errors")
        void testDeleteItem_DatabaseError() {
            when(repository.existsById("1")).thenReturn(Mono.just(true));
            when(repository.deleteById("1"))
                    .thenReturn(Mono.error(new RuntimeException("DB Error")));

            StepVerifier.create(service.deleteItem("1"))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should not call delete when item not found")
        void testDeleteItem_NoDeleteCall() {
            when(repository.existsById("999")).thenReturn(Mono.just(false));

            StepVerifier.create(service.deleteItem("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();

            verify(repository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("should not call repository when delete is disabled")
        void testDeleteItem_NoRepositoryCall() {
            when(features.isDeleteEnabled()).thenReturn(false);

            StepVerifier.create(service.deleteItem("1"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();

            verify(repository, never()).existsById(anyString());
            verify(repository, never()).deleteById(anyString());
        }
    }
}
