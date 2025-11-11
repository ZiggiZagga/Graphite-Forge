package com.example.graphql;

/**
 * Exception thrown when a CRUD operation is disabled via feature toggle.
 */
public class ItemOperationDisabledException extends ItemException {
    private final String operation;

    public ItemOperationDisabledException(String message) {
        super(message);
        this.operation = extractOperation(message);
    }

    private static String extractOperation(String message) {
        if (message.contains("Read")) return "READ";
        if (message.contains("Create")) return "CREATE";
        if (message.contains("Update")) return "UPDATE";
        if (message.contains("Delete")) return "DELETE";
        return "UNKNOWN";
    }

    public String getOperation() {
        return operation;
    }
}
