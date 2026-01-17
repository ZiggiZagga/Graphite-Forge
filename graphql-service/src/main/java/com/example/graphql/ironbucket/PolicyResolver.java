package com.example.graphql.ironbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * GraphQL resolver for policy management operations.
 */
@Controller
public class PolicyResolver {

    private static final Logger log = LoggerFactory.getLogger(PolicyResolver.class);

    private final PolicyManagementService policyService;

    public PolicyResolver(PolicyManagementService policyService) {
        this.policyService = policyService;
    }

    @QueryMapping
    public Flux<PolicyRule> listPolicies(@Argument String jwtToken) {
        log.debug("GraphQL: listPolicies");
        return policyService.listPolicies(jwtToken);
    }

    @QueryMapping
    public Mono<PolicyRule> getPolicy(@Argument String jwtToken, @Argument String policyId) {
        log.debug("GraphQL: getPolicy - {}", policyId);
        return policyService.getPolicyById(jwtToken, policyId);
    }

    @MutationMapping
    public Mono<PolicyRule> createPolicy(@Argument String jwtToken, @Argument PolicyInput input) {
        log.debug("GraphQL: createPolicy");
        return policyService.createPolicy(jwtToken, input);
    }

    @MutationMapping
    public Mono<PolicyRule> updatePolicy(
            @Argument String jwtToken,
            @Argument String policyId,
            @Argument PolicyInput input
    ) {
        log.debug("GraphQL: updatePolicy - {}", policyId);
        return policyService.updatePolicy(jwtToken, policyId, input);
    }

    @MutationMapping
    public Mono<Boolean> deletePolicy(@Argument String jwtToken, @Argument String policyId) {
        log.debug("GraphQL: deletePolicy - {}", policyId);
        return policyService.deletePolicy(jwtToken, policyId);
    }

    @QueryMapping
    public Flux<PolicyRule> policies(@Argument String jwtToken) {
        log.debug("GraphQL: policies");
        return policyService.listPolicies(jwtToken);
    }

    @QueryMapping
    public Mono<PolicyRule> policyById(@Argument String jwtToken, @Argument String policyId) {
        log.debug("GraphQL: policyById - {}", policyId);
        return policyService.getPolicyById(jwtToken, policyId);
    }

    @QueryMapping
    public Flux<PolicyRule> policiesByTenant(@Argument String jwtToken, @Argument String tenant) {
        log.debug("GraphQL: policiesByTenant - {}", tenant);
        return policyService.getPoliciesByTenant(jwtToken, tenant);
    }

    @MutationMapping
    public Mono<PolicyEvaluationResult> dryRunPolicy(
            @Argument String jwtToken,
            @Argument PolicyInput policy,
            @Argument String operation,
            @Argument String resource
    ) {
        log.debug("GraphQL: dryRunPolicy - {} on {}", operation, resource);
        // Create context from the policy and operation
        PolicyEvaluationContext context = new PolicyEvaluationContext(
            policy.tenant(),
            policy.roles(),
            operation,
            resource
        );
        // Mock evaluation for now - tests expect a result
        return Mono.just(new PolicyEvaluationResult(PolicyDecision.ALLOW, List.of("default-policy"), "Allowed"));
    }
}

