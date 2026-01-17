package com.example.graphql.ironbucket;

/**
 * Thrown when attempting to access a policy that does not exist.
 */
public class PolicyNotFoundException extends RuntimeException {
    
    public PolicyNotFoundException(String policyId) {
        super("Policy not found: " + policyId);
    }
    
    public PolicyNotFoundException(String policyId, Throwable cause) {
        super("Policy not found: " + policyId, cause);
    }
}
