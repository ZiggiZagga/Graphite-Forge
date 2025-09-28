package com.example.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@GraphQlTest(ItemGraphqlController.class)
class ItemGraphqlControllerTest {

    @Autowired
    private WebGraphQlTester graphQlTester;

    @MockBean
    private ItemRepository repo;
    @MockBean
    private CrudFeatures features;

    private final Item item = new Item("1", "TestItem", "A test item");

    @BeforeEach
    void setup() {
        when(features.isReadEnabled()).thenReturn(true);
        when(features.isCreateEnabled()).thenReturn(true);
        when(features.isUpdateEnabled()).thenReturn(true);
        when(features.isDeleteEnabled()).thenReturn(true);
    }

    @Test
    void testItemsQuery_returnsItems() {
        when(repo.findAll()).thenReturn(Flux.just(item));
        String query = "{ items { id name description } }";
        graphQlTester.document(query)
                .execute()
                .path("items[0].id").entity(String.class).isEqualTo("1");
    }

    @Test
    void testItemByIdQuery_returnsItem() {
        when(repo.findById("1")).thenReturn(Mono.just(item));
        String query = "{ itemById(id: \"1\") { id name description } }";
        graphQlTester.document(query)
                .execute()
                .path("itemById.id").entity(String.class).isEqualTo("1");
    }

    @Test
    void testCreateItemMutation_createsItem() {
        when(repo.save(any(Item.class))).thenReturn(Mono.just(item));
        String mutation = "mutation { createItem(name: \"TestItem\", description: \"A test item\") { id name description } }";
        graphQlTester.document(mutation)
                .execute()
                .path("createItem.id").entity(String.class).isEqualTo("1");
    }

    @Test
    void testUpdateItemMutation_updatesItem() {
        when(repo.findById("1")).thenReturn(Mono.just(item));
        when(repo.save(any(Item.class))).thenReturn(Mono.just(new Item("1", "Updated", "Updated desc")));
        String mutation = "mutation { updateItem(id: \"1\", name: \"Updated\", description: \"Updated desc\") { id name description } }";
        graphQlTester.document(mutation)
                .execute()
                .path("updateItem.name").entity(String.class).isEqualTo("Updated");
    }

    @Test
    void testDeleteItemMutation_deletesItem() {
        when(repo.deleteById("1")).thenReturn(Mono.empty());
        String mutation = "mutation { deleteItem(id: \"1\") }";
        graphQlTester.document(mutation)
                .execute()
                .path("deleteItem").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void testReadDisabled_returnsError() {
        when(features.isReadEnabled()).thenReturn(false);
        String query = "{ items { id name description } }";
        graphQlTester.document(query)
                .execute()
                .errors().satisfy(errors -> {
                    assert(!errors.isEmpty());
                });
    }
}
