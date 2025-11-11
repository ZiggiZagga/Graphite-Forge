package com.example.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Comprehensive GraphQL controller tests with edge cases.
 *
 * <p>Tests include:
 * - Happy path scenarios
 * - Error handling (not found, disabled operations)
 * - Edge cases (null inputs, empty results)
 * - Feature toggle behavior
 * </p>
 */
@GraphQlTest(ItemGraphqlController.class)
class ItemGraphqlControllerTest {

    @Autowired
    private WebGraphQlTester graphQlTester;

    @MockBean
    private ItemService service;

    private final Item testItem = new Item("1", "TestItem", "A test item");

    @BeforeEach
    void setup() {
        Mockito.reset(service);
    }

    @Nested
    @DisplayName("Query: items")
    class ItemsQueryTests {

        @Test
        @DisplayName("should return all items")
        void testItemsQuery_returnsItems() {
            when(service.getAllItems()).thenReturn(Flux.just(testItem));

            String query = "{ items { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .path("items[0].id").entity(String.class).isEqualTo("1")
                    .path("items[0].name").entity(String.class).isEqualTo("TestItem")
                    .path("items[0].description").entity(String.class).isEqualTo("A test item");
        }

        @Test
        @DisplayName("should return empty list when no items exist")
        void testItemsQuery_returnsEmptyList() {
            when(service.getAllItems()).thenReturn(Flux.empty());

            String query = "{ items { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .path("items").entityList(Object.class).hasSize(0);
        }

        @Test
        @DisplayName("should handle read disabled error")
        void testItemsQuery_readDisabled() {
            when(service.getAllItems())
                    .thenReturn(Flux.error(new ItemOperationDisabledException("Read operation is disabled")));

            String query = "{ items { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .errors().satisfy(errors -> {
                        assert !errors.isEmpty();
                        assert errors.get(0).getMessage().contains("Read operation is disabled");
                    });
        }

        @Test
        @DisplayName("should handle database error gracefully")
        void testItemsQuery_databaseError() {
            when(service.getAllItems())
                    .thenReturn(Flux.error(new ItemDatabaseException("Connection failed")));

            String query = "{ items { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .errors().satisfy(errors -> {
                        assert !errors.isEmpty();
                        assert errors.get(0).getMessage().contains("Database operation failed");
                    });
        }

        @Test
        @DisplayName("should return multiple items")
        void testItemsQuery_returnsMultipleItems() {
            Item item2 = new Item("2", "TestItem2", "Another test item");
            when(service.getAllItems()).thenReturn(Flux.just(testItem, item2));

            String query = "{ items { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .path("items").entityList(Object.class).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Query: itemById")
    class ItemByIdQueryTests {

        @Test
        @DisplayName("should return item by id")
        void testItemByIdQuery_returnsItem() {
            when(service.getItemById("1")).thenReturn(Mono.just(testItem));

            String query = "{ itemById(id: \"1\") { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .path("itemById.id").entity(String.class).isEqualTo("1")
                    .path("itemById.name").entity(String.class).isEqualTo("TestItem");
        }

        @Test
        @DisplayName("should return error when item not found")
        void testItemByIdQuery_itemNotFound() {
            when(service.getItemById("999"))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            String query = "{ itemById(id: \"999\") { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .errors().satisfy(errors -> {
                        assert !errors.isEmpty();
                        assert errors.get(0).getMessage().contains("not found");
                    });
        }

        @Test
        @DisplayName("should handle read disabled error")
        void testItemByIdQuery_readDisabled() {
            when(service.getItemById("1"))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Read operation is disabled")));

            String query = "{ itemById(id: \"1\") { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .errors().satisfy(errors -> {
                        assert !errors.isEmpty();
                        assert errors.get(0).getMessage().contains("disabled");
                    });
        }

        @Test
        @DisplayName("should handle database error")
        void testItemByIdQuery_databaseError() {
            when(service.getItemById("1"))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database connection failed")));

            String query = "{ itemById(id: \"1\") { id name description } }";
            graphQlTester.document(query)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }
    }

    @Nested
    @DisplayName("Mutation: createItem")
    class CreateItemMutationTests {

        @Test
        @DisplayName("should create item with required fields")
        void testCreateItemMutation_createsItem() {
            when(service.createItem(any(Item.class))).thenReturn(Mono.just(testItem));

            String mutation = "mutation { createItem(name: \"TestItem\", description: \"A test item\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .path("createItem.id").entity(String.class).isEqualTo("1")
                    .path("createItem.name").entity(String.class).isEqualTo("TestItem");
        }

        @Test
        @DisplayName("should create item without description")
        void testCreateItemMutation_createsItemWithoutDescription() {
            Item itemNoDesc = new Item("1", "TestItem", null);
            when(service.createItem(any(Item.class))).thenReturn(Mono.just(itemNoDesc));

            String mutation = "mutation { createItem(name: \"TestItem\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .path("createItem.id").entity(String.class).isEqualTo("1");
        }

        @Test
        @DisplayName("should return error when create is disabled")
        void testCreateItemMutation_createDisabled() {
            when(service.createItem(any(Item.class)))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Create operation is disabled")));

            String mutation = "mutation { createItem(name: \"TestItem\", description: \"A test item\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> {
                        assert !errors.isEmpty();
                        assert errors.get(0).getMessage().contains("disabled");
                    });
        }

        @Test
        @DisplayName("should handle database error during creation")
        void testCreateItemMutation_databaseError() {
            when(service.createItem(any(Item.class)))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            String mutation = "mutation { createItem(name: \"TestItem\", description: \"A test item\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
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

            String mutation = "mutation { updateItem(id: \"1\", name: \"Updated\", description: \"Updated desc\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .path("updateItem.name").entity(String.class).isEqualTo("Updated")
                    .path("updateItem.description").entity(String.class).isEqualTo("Updated desc");
        }

        @Test
        @DisplayName("should update item with partial fields")
        void testUpdateItemMutation_updatePartial() {
            Item updatedItem = new Item("1", "Updated", "A test item");
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.just(updatedItem));

            String mutation = "mutation { updateItem(id: \"1\", name: \"Updated\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .path("updateItem.name").entity(String.class).isEqualTo("Updated");
        }

        @Test
        @DisplayName("should return error when item not found")
        void testUpdateItemMutation_itemNotFound() {
            when(service.updateItem("999", "Updated", null))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            String mutation = "mutation { updateItem(id: \"999\", name: \"Updated\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }

        @Test
        @DisplayName("should return error when update is disabled")
        void testUpdateItemMutation_updateDisabled() {
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Update operation is disabled")));

            String mutation = "mutation { updateItem(id: \"1\", name: \"Updated\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }

        @Test
        @DisplayName("should handle database error during update")
        void testUpdateItemMutation_databaseError() {
            when(service.updateItem("1", "Updated", null))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            String mutation = "mutation { updateItem(id: \"1\", name: \"Updated\") { id name description } }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }
    }

    @Nested
    @DisplayName("Mutation: deleteItem")
    class DeleteItemMutationTests {

        @Test
        @DisplayName("should delete item successfully")
        void testDeleteItemMutation_deletesItem() {
            when(service.deleteItem("1")).thenReturn(Mono.just(true));

            String mutation = "mutation { deleteItem(id: \"1\") }";
            graphQlTester.document(mutation)
                    .execute()
                    .path("deleteItem").entity(Boolean.class).isEqualTo(true);
        }

        @Test
        @DisplayName("should return error when item not found")
        void testDeleteItemMutation_itemNotFound() {
            when(service.deleteItem("999"))
                    .thenReturn(Mono.error(new ItemNotFoundException("999")));

            String mutation = "mutation { deleteItem(id: \"999\") }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }

        @Test
        @DisplayName("should return error when delete is disabled")
        void testDeleteItemMutation_deleteDisabled() {
            when(service.deleteItem("1"))
                    .thenReturn(Mono.error(new ItemOperationDisabledException("Delete operation is disabled")));

            String mutation = "mutation { deleteItem(id: \"1\") }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }

        @Test
        @DisplayName("should handle database error during deletion")
        void testDeleteItemMutation_databaseError() {
            when(service.deleteItem("1"))
                    .thenReturn(Mono.error(new ItemDatabaseException("Database error")));

            String mutation = "mutation { deleteItem(id: \"1\") }";
            graphQlTester.document(mutation)
                    .execute()
                    .errors().satisfy(errors -> { assert !errors.isEmpty(); });
        }
    }
}
