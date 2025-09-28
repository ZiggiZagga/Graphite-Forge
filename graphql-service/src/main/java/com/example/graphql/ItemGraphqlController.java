package com.example.graphql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ItemGraphqlController {

  @Autowired private ItemRepository repo;
  @Autowired private CrudFeatures features;

  @QueryMapping
  public Flux<Item> items() {
    if (!features.isReadEnabled()) 
      return Flux.error(new IllegalStateException("Read disabled"));
    return repo.findAll();
  }

  @QueryMapping
  public Mono<Item> itemById(@Argument String id) {
    if (!features.isReadEnabled()) 
      return Mono.error(new IllegalStateException("Read disabled"));
    return repo.findById(id);
  }

  @MutationMapping
  public Mono<Item> createItem(@Argument String name, @Argument String description) {
    if (!features.isCreateEnabled()) 
      return Mono.error(new IllegalStateException("Create disabled"));
    return repo.save(new Item(null, name, description));
  }

  @MutationMapping
  public Mono<Item> updateItem(@Argument String id, @Argument String name,
                               @Argument String description) {
    if (!features.isUpdateEnabled()) 
      return Mono.error(new IllegalStateException("Update disabled"));
    return repo.findById(id)
               .map(it -> new Item(it.id(), 
                                   name != null ? name : it.name(),
                                   description != null ? description : it.description()))
               .flatMap(repo::save);
  }

  @MutationMapping
  public Mono<Boolean> deleteItem(@Argument String id) {
    if (!features.isDeleteEnabled()) 
      return Mono.error(new IllegalStateException("Delete disabled"));
    return repo.deleteById(id).thenReturn(true);
  }
}
