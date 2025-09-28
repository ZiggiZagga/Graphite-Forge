package com.example.graphql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends ReactiveCrudRepository<Item, String> {
  Flux<Item> findAll();
  Mono<Item> findById(String id);
}