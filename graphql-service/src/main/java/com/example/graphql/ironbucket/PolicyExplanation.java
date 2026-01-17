package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Detailed explanation of why a policy decision was made.
 */
public record PolicyExplanation(
    PolicyDecision decision,
    List<String> evaluatedPolicies,
    List<String> matchedRules,
    List<String> failedConditions,
    String summary
) {
    public PolicyExplanation {
        if (decision == null) {
            throw new IllegalArgumentException("Decision cannot be null");
        }
    }
    
    // Getter alias for compatibility
    public List<String> evaluationSteps() {
        return this.evaluatedPolicies;
    }
}

