package com.example.graphql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
@Table("items")
public record Item(@Id String id, String name, String description) {}
