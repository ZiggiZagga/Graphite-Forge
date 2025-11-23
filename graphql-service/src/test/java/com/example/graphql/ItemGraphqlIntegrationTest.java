package com.example.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GraphQL API.
 *
 * <p>Tests the full GraphQL stack including:
 * - Query and mutation resolution
 * - Error handling and GraphQL error formatting
 * - Feature toggle enforcement
 * - Database persistence
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ItemGraphqlIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CrudFeatures crudFeatures;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        itemRepository.deleteAll().block();
        // Reset feature toggles to enabled
        crudFeatures.setCreateEnabled(true);
        crudFeatures.setReadEnabled(true);
        crudFeatures.setUpdateEnabled(true);
        crudFeatures.setDeleteEnabled(true);
    }

    @Nested
    @DisplayName("Query: items")
    class ItemsQueryTests {

        @Test
        @DisplayName("should return empty list when no items exist")
        void testItemsQuery_Empty() {
            graphQlTester.documentName("getItems")
                    .execute()
                    .path("items")
                    .entityList(Item.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("should return all items")
        void testItemsQuery_ReturnsItems() {
            // Given
            Item item1 = new Item(null, "Item 1", "Description 1");
            Item item2 = new Item(null, "Item 2", "Description 2");
            itemRepository.save(item1).block();
            itemRepository.save(item2).block();

            // When/Then
            graphQlTester.documentName("getItems")
                    .execute()
                    .path("items")
                    .entityList(Item.class)
                    .hasSize(2)
                    .extracting("name")
                    .containsExactly("Item 1", "Item 2");
        }

        @Test
        @DisplayName("should return error when read is disabled")
        void testItemsQuery_ReadDisabled() {
            // Given
            crudFeatures.setReadEnabled(false);

            // When/Then
            graphQlTester.documentName("getItems")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("Read operation is disabled");
                    });
        }
    }

    @Nested
    @DisplayName("Query: itemById")
    class ItemByIdQueryTests {

        @Test
        @DisplayName("should return item when found")
        void testItemByIdQuery_Success() {
            // Given
            Item saved = itemRepository.save(new Item(null, "TestItem", "Test Desc")).block();

            // When/Then
            graphQlTester.documentName("getItemById")
                    .variable("id", saved.id())
                    .execute()
                    .path("itemById")
                    .entity(Item.class)
                    .isEqualTo(saved);
        }

        @Test
        @DisplayName("should return NOT_FOUND error when item not found")
        void testItemByIdQuery_NotFound() {
            // When/Then
            graphQlTester.documentName("getItemById")
                    .variable("id", "999")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("not found");
                    });
        }

        @Test
        @DisplayName("should return error when read is disabled")
        void testItemByIdQuery_ReadDisabled() {
            // Given
            crudFeatures.setReadEnabled(false);

            // When/Then
            graphQlTester.documentName("getItemById")
                    .variable("id", "1")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("Read operation is disabled");
                    });
        }
    }

    @Nested
    @DisplayName("Mutation: createItem")
    class CreateItemMutationTests {

        @Test
        @DisplayName("should create item successfully")
        void testCreateItemMutation_Success() {
            // When/Then
            graphQlTester.documentName("createItem")
                    .variable("name", "NewItem")
                    .variable("description", "New Description")
                    .execute()
                    .path("createItem")
                    .entity(Item.class)
                    .satisfies(item -> {
                        assertThat(item.id()).isNotNull();
                        assertThat(item.name()).isEqualTo("NewItem");
                        assertThat(item.description()).isEqualTo("New Description");
                    });
        }

        @Test
        @DisplayName("should create item without description")
        void testCreateItemMutation_NoDescription() {
            // When/Then
            graphQlTester.documentName("createItem")
                    .variable("name", "NewItem")
                    .variable("description", null)
                    .execute()
                    .path("createItem")
                    .entity(Item.class)
                    .satisfies(item -> {
                        assertThat(item.name()).isEqualTo("NewItem");
                        assertThat(item.description()).isNull();
                    });
        }

        @Test
        @DisplayName("should return error when name is null")
        void testCreateItemMutation_NullName() {
            // When/Then
            graphQlTester.documentName("createItem")
                    .variable("name", null)
                    .variable("description", "Description")
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty());
        }

        @Test
        @DisplayName("should return error when create is disabled")
        void testCreateItemMutation_CreateDisabled() {
            // Given
            crudFeatures.setCreateEnabled(false);

            // When/Then
            graphQlTester.documentName("createItem")
                    .variable("name", "NewItem")
                    .variable("description", "Description")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("Create operation is disabled");
                    });
        }
    }

    @Nested
    @DisplayName("Mutation: updateItem")
    class UpdateItemMutationTests {

        @Test
        @DisplayName("should update item successfully")
        void testUpdateItemMutation_Success() {
            // Given
            Item saved = itemRepository.save(new Item(null, "Original", "Original Desc")).block();

            // When/Then
            graphQlTester.documentName("updateItem")
                    .variable("id", saved.id())
                    .variable("name", "Updated")
                    .variable("description", "Updated Desc")
                    .execute()
                    .path("updateItem")
                    .entity(Item.class)
                    .satisfies(item -> {
                        assertThat(item.id()).isEqualTo(saved.id());
                        assertThat(item.name()).isEqualTo("Updated");
                        assertThat(item.description()).isEqualTo("Updated Desc");
                    });
        }

        @Test
        @DisplayName("should update only name")
        void testUpdateItemMutation_PartialUpdate() {
            // Given
            Item saved = itemRepository.save(new Item(null, "Original", "Original Desc")).block();

            // When/Then
            graphQlTester.documentName("updateItem")
                    .variable("id", saved.id())
                    .variable("name", "Updated")
                    .variable("description", null)
                    .execute()
                    .path("updateItem")
                    .entity(Item.class)
                    .satisfies(item -> {
                        assertThat(item.name()).isEqualTo("Updated");
                        assertThat(item.description()).isEqualTo("Original Desc");
                    });
        }

        @Test
        @DisplayName("should return error when item not found")
        void testUpdateItemMutation_NotFound() {
            // When/Then
            graphQlTester.documentName("updateItem")
                    .variable("id", "999")
                    .variable("name", "Updated")
                    .variable("description", null)
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("not found");
                    });
        }

        @Test
        @DisplayName("should return error when update is disabled")
        void testUpdateItemMutation_UpdateDisabled() {
            // Given
            crudFeatures.setUpdateEnabled(false);

            // When/Then
            graphQlTester.documentName("updateItem")
                    .variable("id", "1")
                    .variable("name", "Updated")
                    .variable("description", null)
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("Update operation is disabled");
                    });
        }
    }

    @Nested
    @DisplayName("Mutation: deleteItem")
    class DeleteItemMutationTests {

        @Test
        @DisplayName("should delete item successfully")
        void testDeleteItemMutation_Success() {
            // Given
            Item saved = itemRepository.save(new Item(null, "ToDelete", "Description")).block();

            // When/Then
            graphQlTester.documentName("deleteItem")
                    .variable("id", saved.id())
                    .execute()
                    .path("deleteItem")
                    .entity(Boolean.class)
                    .isEqualTo(true);

            // Verify deletion
            StepVerifier.create(itemRepository.findById(saved.id()))
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("should return error when item not found")
        void testDeleteItemMutation_NotFound() {
            // When/Then
            graphQlTester.documentName("deleteItem")
                    .variable("id", "999")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("not found");
                    });
        }

        @Test
        @DisplayName("should return error when delete is disabled")
        void testDeleteItemMutation_DeleteDisabled() {
            // Given
            crudFeatures.setDeleteEnabled(false);

            // When/Then
            graphQlTester.documentName("deleteItem")
                    .variable("id", "1")
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).hasSize(1);
                        assertThat(errors.get(0).getMessage())
                                .contains("Delete operation is disabled");
                    });
        }
    }
}
