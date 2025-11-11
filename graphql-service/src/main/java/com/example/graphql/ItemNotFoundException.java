package com.example.graphql;

/**
 * Exception thrown when an item is not found in the database.
 */
public class ItemNotFoundException extends ItemException {
    private final String itemId;

    public ItemNotFoundException(String itemId) {
        super(String.format("Item with ID '%s' not found", itemId));
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }
}
