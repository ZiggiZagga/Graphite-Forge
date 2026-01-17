package com.example.graphql.ironbucket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for PolicyResolver
 * Tests GraphQL queries and mutations for policy management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyResolver Tests")
class PolicyResolverTest {

    @Mock
    private PolicyManagementService policyService;

    @InjectMocks
    private PolicyResolver resolver;

    private String mockJwtToken;

    @BeforeEach
    void setUp() {
        mockJwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
    }

    @Nested
    @DisplayName("Query: policies")
    class Policies {

        @Test
        @DisplayName("Should list all policies")
        void shouldListAllPolicies() {
            // Given
            when(policyService.listPolicies(anyString()))
                .thenReturn(Flux.just(
                    new PolicyRule("policy-1", "tenant-a", List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*"), 1, false),
                    new PolicyRule("policy-2", "tenant-a", List.of("user"), List.of("user-bucket"), List.of("*"), List.of("s3:GetObject"), 1, false)
                ));

            // When
            Flux<PolicyRule> result = resolver.policies(mockJwtToken);

            // Then
            result.collectList().block();
            verify(policyService).listPolicies(mockJwtToken);
        }
    }

    @Nested
    @DisplayName("Query: policyById")
    class PolicyById {

        @Test
        @DisplayName("Should get policy by ID")
        void shouldGetPolicyById() {
            // Given
            String policyId = "policy-1";
            PolicyRule mockPolicy = new PolicyRule(policyId, "tenant-a", List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*"), 1, false);
            when(policyService.getPolicyById(anyString(), eq(policyId)))
                .thenReturn(Mono.just(mockPolicy));

            // When
            Mono<PolicyRule> result = resolver.policyById(mockJwtToken, policyId);

            // Then
            result.block();
            verify(policyService).getPolicyById(mockJwtToken, policyId);
        }
    }

    @Nested
    @DisplayName("Query: policiesByTenant")
    class PoliciesByTenant {

        @Test
        @DisplayName("Should list policies for tenant")
        void shouldListPoliciesForTenant() {
            // Given
            String tenant = "tenant-a";
            when(policyService.getPoliciesByTenant(anyString(), eq(tenant)))
                .thenReturn(Flux.just(
                    new PolicyRule("policy-1", tenant, List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*"), 1, false)
                ));

            // When
            Flux<PolicyRule> result = resolver.policiesByTenant(mockJwtToken, tenant);

            // Then
            result.collectList().block();
            verify(policyService).getPoliciesByTenant(mockJwtToken, tenant);
        }
    }

    @Nested
    @DisplayName("Mutation: createPolicy")
    class CreatePolicy {

        @Test
        @DisplayName("Should create policy")
        void shouldCreatePolicy() {
            // Given
            PolicyInput input = new PolicyInput("tenant-a", List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*"));
            PolicyRule mockPolicy = new PolicyRule("new-policy", "tenant-a", List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*"), 1, false);
            when(policyService.createPolicy(anyString(), any(PolicyInput.class)))
                .thenReturn(Mono.just(mockPolicy));

            // When
            Mono<PolicyRule> result = resolver.createPolicy(mockJwtToken, input);

            // Then
            result.block();
            verify(policyService).createPolicy(mockJwtToken, input);
        }
    }

    @Nested
    @DisplayName("Mutation: updatePolicy")
    class UpdatePolicy {

        @Test
        @DisplayName("Should update policy")
        void shouldUpdatePolicy() {
            // Given
            String policyId = "policy-1";
            PolicyInput input = new PolicyInput("tenant-a", List.of("admin", "power-user"), List.of("*"), List.of("*"), List.of("s3:*"));
            PolicyRule mockPolicy = new PolicyRule(policyId, "tenant-a", List.of("admin", "power-user"), List.of("*"), List.of("*"), List.of("s3:*"), 2, false);
            when(policyService.updatePolicy(anyString(), eq(policyId), any(PolicyInput.class)))
                .thenReturn(Mono.just(mockPolicy));

            // When
            Mono<PolicyRule> result = resolver.updatePolicy(mockJwtToken, policyId, input);

            // Then
            result.block();
            verify(policyService).updatePolicy(mockJwtToken, policyId, input);
        }
    }

    @Nested
    @DisplayName("Mutation: deletePolicy")
    class DeletePolicy {

        @Test
        @DisplayName("Should delete policy")
        void shouldDeletePolicy() {
            // Given
            String policyId = "policy-to-delete";
            when(policyService.deletePolicy(anyString(), eq(policyId)))
                .thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = resolver.deletePolicy(mockJwtToken, policyId);

            // Then
            result.block();
            verify(policyService).deletePolicy(mockJwtToken, policyId);
        }
    }

    @Nested
    @DisplayName("Mutation: dryRunPolicy")
    class DryRunPolicy {

        @Test
        @DisplayName("Should evaluate policy dry-run")
        void shouldEvaluatePolicyDryRun() {
            // Given
            PolicyInput input = new PolicyInput("tenant-a", List.of("user"), List.of("user-bucket"), List.of("*"), List.of("s3:GetObject"));
            String operation = "s3:GetObject";
            String resource = "arn:aws:s3:::user-bucket/file.txt";
            PolicyEvaluationResult mockResult = new PolicyEvaluationResult(PolicyDecision.ALLOW, List.of(), "Allowed by policy");
            when(policyService.evaluatePolicy(anyString(), anyString(), any(PolicyEvaluationContext.class)))
                .thenReturn(Mono.just(mockResult));

            // When
            Mono<PolicyEvaluationResult> result = resolver.dryRunPolicy(mockJwtToken, input, operation, resource);

            // Then
            result.block();
            // Verify dry-run was executed
        }
    }
}
