package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Result of policy validation.
 * 
 * @param valid Whether the policy is valid
 * @param errors List of validation error messages (empty if valid)
 */
public record PolicyValidationResult(
    boolean valid,
    List<String> errors
) {
    public PolicyValidationResult {
        if (errors == null) {
            throw new IllegalArgumentException("Errors list cannot be null");
        }
    }
    
    public static PolicyValidationResult success() {
        return new PolicyValidationResult(true, List.of());
    }
    
    public static PolicyValidationResult failure(List<String> errors) {
        return new PolicyValidationResult(false, errors);
    }
    
    public boolean isValid() {
        return valid;
    }
}
