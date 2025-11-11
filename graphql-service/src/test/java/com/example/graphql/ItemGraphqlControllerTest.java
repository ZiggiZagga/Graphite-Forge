package com.example.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the GraphQL controller that call mapping methods directly.
 * This avoids depending on Spring GraphQL test slices and focuses on controller
 * behavior and delegation to the service layer.
 */
class ItemGraphqlControllerTest {

    private ItemService service;
    private ItemGraphqlController controller;

    private final Item testItem = new Item("1", "TestItem", "A test item");

    @BeforeEach
    void setup() {
        service = Mockito.mock(ItemService.class);
        controller = new ItemGraphqlController();
        ReflectionTestUtils.setField(controller, "service", service);
    }

    @Nested
    @DisplayName("Query: items")
    class ItemsQueryTests {

        @Test
        @DisplayName("should return all items")
        void testItemsQuery_returnsItems() {
            when(service.getAllItems()).thenReturn(Flux.just(testItem));

            StepVerifier.create(controller.items())
                    .expectNext(testItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty list when no items exist")
        void testItemsQuery_returnsEmptyList() {
            when(service.getAllItems()).thenReturn(Flux.empty());

            StepVerifier.create(controller.items())
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle read disabled error")
        void testItemsQuery_readDisabled() {
            when(service.getAllItems())
                    .thenReturn(Flux.error(new ItemOperationDisabledException("Read operation is disabled")));

            StepVerifier.create(controller.items())
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle database error gracefully")
        void testItemsQuery_databaseError() {
            when(service.getAllItems())
                    .thenReturn(Flux.error(new ItemDatabaseException("Connection failed")));

            StepVerifier.create(controller.items())
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return multiple items")
        void testItemsQuery_returnsMultipleItems() {
            Item item2 = new Item("2", "TestItem2", "Another test item");
            when(service.getAllItems()).thenReturn(Flux.just(testItem, item2));

            StepVerifier.create(controller.items())
                    .expectNext(testItem, item2)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Query: itemById")
    class ItemByIdQueryTests {

        @Test
        @DisplayName("should return item by id")
        void testItemByIdQuery_returnsItem() {
            when(service.getItemById("1")).thenReturn(Mono.just(testItem));

            StepVerifier.create(controller.itemById("1"))
                    .expectNext(testItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testItemByIdQuery_itemNotFound() {
            when(service.getItemById("999"))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            StepVerifier.create(controller.itemById("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle read disabled error")
        void testItemByIdQuery_readDisabled() {
            when(service.getItemById("1"))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Read operation is disabled")));

            StepVerifier.create(controller.itemById("1"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle database error")
        void testItemByIdQuery_databaseError() {
            when(service.getItemById("1"))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database connection failed")));

            StepVerifier.create(controller.itemById("1"))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Mutation: createItem")
    class CreateItemMutationTests {

        @Test
        @DisplayName("should create item with required fields")
        void testCreateItemMutation_createsItem() {
            when(service.createItem(any(Item.class))).thenReturn(Mono.just(testItem));

            StepVerifier.create(controller.createItem("TestItem", "A test item"))
                    .expectNext(testItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create item without description")
        void testCreateItemMutation_createsItemWithoutDescription() {
            Item itemNoDesc = new Item("1", "TestItem", null);
            when(service.createItem(any(Item.class))).thenReturn(Mono.just(itemNoDesc));

            StepVerifier.create(controller.createItem("TestItem", null))
                    .expectNext(itemNoDesc)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when create is disabled")
        void testCreateItemMutation_createDisabled() {
            when(service.createItem(any(Item.class)))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Create operation is disabled")));

            StepVerifier.create(controller.createItem("TestItem", "A test item"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle database error during creation")
        void testCreateItemMutation_databaseError() {
            when(service.createItem(any(Item.class)))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            StepVerifier.create(controller.createItem("TestItem", "A test item"))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Mutation: updateItem")
    class UpdateItemMutationTests {

        @Test
        @DisplayName("should update item name and description")
        void testUpdateItemMutation_updatesItem() {
            Item updatedItem = new Item("1", "Updated", "Updated desc");
            when(service.updateItem("1", "Updated", "Updated desc"))
                    .thenReturn(Mono.just(updatedItem));

            StepVerifier.create(controller.updateItem("1", "Updated", "Updated desc"))
                    .expectNext(updatedItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should update item with partial fields")
        void testUpdateItemMutation_updatePartial() {
            Item updatedItem = new Item("1", "Updated", "A test item");
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.just(updatedItem));

            StepVerifier.create(controller.updateItem("1", "Updated", null))
                    .expectNext(updatedItem)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testUpdateItemMutation_itemNotFound() {
            when(service.updateItem("999", "Updated", null))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            StepVerifier.create(controller.updateItem("999", "Updated", null))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return error when update is disabled")
        void testUpdateItemMutation_updateDisabled() {
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Update operation is disabled")));

            StepVerifier.create(controller.updateItem("1", "Updated", null))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle database error during update")
        void testUpdateItemMutation_databaseError() {
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            StepVerifier.create(controller.updateItem("1", "Updated", null))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Mutation: deleteItem")
    class DeleteItemMutationTests {

        @Test
        @DisplayName("should delete item successfully")
        void testDeleteItemMutation_deletesItem() {
            when(service.deleteItem("1")).thenReturn(Mono.just(true));

            StepVerifier.create(controller.deleteItem("1"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testDeleteItemMutation_itemNotFound() {
            when(service.deleteItem("999"))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            StepVerifier.create(controller.deleteItem("999"))
                    .expectError(ItemNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("should return error when delete is disabled")
        void testDeleteItemMutation_deleteDisabled() {
            when(service.deleteItem("1"))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Delete operation is disabled")));

            StepVerifier.create(controller.deleteItem("1"))
                    .expectError(ItemOperationDisabledException.class)
                    .verify();
        }

        @Test
        @DisplayName("should handle database error during deletion")
        void testDeleteItemMutation_databaseError() {
            when(service.deleteItem("1"))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            StepVerifier.create(controller.deleteItem("1"))
                    .expectError(ItemDatabaseException.class)
                    .verify();
        }
    }
}
