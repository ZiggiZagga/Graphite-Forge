package com.example.graphql.ironbucket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for PolicyManagementService
 * Following IronBucket Claimspindel test structure (72 tests)
 * 
 * Tests cover:
 * - Policy CRUD operations
 * - Policy evaluation (dry-run)
 * - Policy validation
 * - Tenant-based policy filtering
 * - Role-based policy enforcement
 * - Policy versioning
 * - GitOps workflow integration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyManagementService Tests")
class PolicyManagementServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private PolicyManagementService service;

    private String mockJwtToken;
    private String mockTenant;

    @BeforeEach
    void setUp() {
        mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        mockTenant = "test-tenant";
    }

    @Nested
    @DisplayName("Policy CRUD Operations")
    class PolicyCrudOperations {

        @Test
        @DisplayName("Should list all policies")
        void shouldListAllPolicies() {
            // Given
            List<PolicyRule> expectedPolicies = List.of(
                createMockPolicy("policy-1", mockTenant, List.of("admin"), List.of("*"), List.of("s3:*")),
                createMockPolicy("policy-2", mockTenant, List.of("user"), List.of("user-bucket"), List.of("s3:GetObject"))
            );

            // When
            Flux<PolicyRule> result = service.listPolicies(mockJwtToken);

            // Then
            StepVerifier.create(result)
                .expectNextSequence(expectedPolicies)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get policy by ID")
        void shouldGetPolicyById() {
            // Given
            String policyId = "policy-1";
            PolicyRule expectedPolicy = createMockPolicy(
                policyId, mockTenant, List.of("admin"), List.of("*"), List.of("s3:*")
            );

            // When
            Mono<PolicyRule> result = service.getPolicyById(mockJwtToken, policyId);

            // Then
            StepVerifier.create(result)
                .expectNext(expectedPolicy)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to get non-existent policy")
        void shouldFailToGetNonExistentPolicy() {
            // Given
            String policyId = "non-existent";

            // When
            Mono<PolicyRule> result = service.getPolicyById(mockJwtToken, policyId);

            // Then
            StepVerifier.create(result)
                .expectError(PolicyNotFoundException.class)
                .verify();
        }

        @Test
        @DisplayName("Should create policy")
        void shouldCreatePolicy() {
            // Given
            PolicyInput policyInput = new PolicyInput(
                mockTenant,
                List.of("admin"),
                List.of("*"),
                List.of("*"),
                List.of("s3:*")
            );

            // When
            Mono<PolicyRule> result = service.createPolicy(mockJwtToken, policyInput);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> 
                    policy.tenant().equals(mockTenant) &&
                    policy.roles().contains("admin")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to create policy without admin role")
        void shouldFailToCreatePolicyWithoutAdminRole() {
            // Given
            String userToken = "user-jwt-token";
            PolicyInput policyInput = new PolicyInput(
                mockTenant, List.of("user"), List.of("*"), List.of("*"), List.of("s3:GetObject")
            );

            // When
            Mono<PolicyRule> result = service.createPolicy(userToken, policyInput);

            // Then
            StepVerifier.create(result)
                .expectError(ForbiddenException.class)
                .verify();
        }

        @Test
        @DisplayName("Should update policy")
        void shouldUpdatePolicy() {
            // Given
            String policyId = "policy-1";
            PolicyInput updatedInput = new PolicyInput(
                mockTenant,
                List.of("admin", "power-user"),
                List.of("*"),
                List.of("*"),
                List.of("s3:*")
            );

            // When
            Mono<PolicyRule> result = service.updatePolicy(mockJwtToken, policyId, updatedInput);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> 
                    policy.id().equals(policyId) &&
                    policy.roles().contains("power-user")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should delete policy")
        void shouldDeletePolicy() {
            // Given
            String policyId = "policy-to-delete";

            // When
            Mono<Boolean> result = service.deletePolicy(mockJwtToken, policyId);

            // Then
            StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to delete non-existent policy")
        void shouldFailToDeleteNonExistentPolicy() {
            // Given
            String policyId = "non-existent";

            // When
            Mono<Boolean> result = service.deletePolicy(mockJwtToken, policyId);

            // Then
            StepVerifier.create(result)
                .expectError(PolicyNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Policy Filtering")
    class PolicyFiltering {

        @Test
        @DisplayName("Should filter policies by tenant")
        void shouldFilterPoliciesByTenant() {
            // Given
            String tenant = "tenant-a";

            // When
            Flux<PolicyRule> result = service.getPoliciesByTenant(mockJwtToken, tenant);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> policy.tenant().equals(tenant))
                .thenConsumeWhile(policy -> policy.tenant().equals(tenant))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should filter policies by role")
        void shouldFilterPoliciesByRole() {
            // Given
            String role = "admin";

            // When
            Flux<PolicyRule> result = service.getPoliciesByRole(mockJwtToken, role);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> policy.roles().contains(role))
                .thenConsumeWhile(policy -> policy.roles().contains(role))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should filter policies by bucket")
        void shouldFilterPoliciesByBucket() {
            // Given
            String bucket = "test-bucket";

            // When
            Flux<PolicyRule> result = service.getPoliciesByBucket(mockJwtToken, bucket);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> 
                    policy.allowedBuckets().contains(bucket) ||
                    policy.allowedBuckets().contains("*")
                )
                .thenConsumeWhile(policy -> true)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Policy Evaluation (Dry-Run)")
    class PolicyEvaluation {

        @Test
        @DisplayName("Should evaluate policy for allowed operation")
        void shouldEvaluatePolicyForAllowedOperation() {
            // Given
            String policyId = "policy-1";
            String operation = "s3:GetObject";
            String resource = "arn:aws:s3:::test-bucket/file.txt";
            PolicyEvaluationContext context = new PolicyEvaluationContext(
                mockTenant, List.of("admin"), operation, resource
            );

            // When
            Mono<PolicyEvaluationResult> result = service.evaluatePolicy(mockJwtToken, policyId, context);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(evalResult -> 
                    evalResult.decision() == PolicyDecision.ALLOW &&
                    evalResult.matchedRules().contains(policyId)
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should evaluate policy for denied operation")
        void shouldEvaluatePolicyForDeniedOperation() {
            // Given
            String policyId = "policy-1";
            String operation = "s3:DeleteBucket";
            String resource = "arn:aws:s3:::protected-bucket";
            PolicyEvaluationContext context = new PolicyEvaluationContext(
                mockTenant, List.of("user"), operation, resource
            );

            // When
            Mono<PolicyEvaluationResult> result = service.evaluatePolicy(mockJwtToken, policyId, context);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(evalResult -> 
                    evalResult.decision() == PolicyDecision.DENY &&
                    evalResult.reason().contains("insufficient permissions")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should explain policy decision")
        void shouldExplainPolicyDecision() {
            // Given
            String policyId = "policy-1";
            String operation = "s3:PutObject";
            String resource = "arn:aws:s3:::test-bucket/file.txt";
            PolicyEvaluationContext context = new PolicyEvaluationContext(
                mockTenant, List.of("admin"), operation, resource
            );

            // When
            Mono<PolicyExplanation> result = service.explainPolicy(mockJwtToken, policyId, context);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(explanation -> 
                    explanation.matchedRules().size() > 0 &&
                    explanation.evaluationSteps().size() > 0
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should dry-run policy against multiple operations")
        void shouldDryRunPolicyAgainstMultipleOperations() {
            // Given
            PolicyInput policyInput = new PolicyInput(
                mockTenant,
                List.of("user"),
                List.of("user-bucket"),
                List.of("documents/*"),
                List.of("s3:GetObject", "s3:PutObject")
            );
            List<String> operations = List.of("s3:GetObject", "s3:PutObject", "s3:DeleteObject");
            String resource = "arn:aws:s3:::user-bucket/documents/file.txt";

            // When
            Flux<PolicyEvaluationResult> results = service.dryRunPolicy(
                mockJwtToken, policyInput, operations, resource
            );

            // Then
            StepVerifier.create(results)
                .expectNextMatches(result -> result.decision() == PolicyDecision.ALLOW) // GetObject
                .expectNextMatches(result -> result.decision() == PolicyDecision.ALLOW) // PutObject
                .expectNextMatches(result -> result.decision() == PolicyDecision.DENY)  // DeleteObject
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Policy Validation")
    class PolicyValidation {

        @Test
        @DisplayName("Should validate policy syntax")
        void shouldValidatePolicySyntax() {
            // Given
            PolicyInput validPolicy = new PolicyInput(
                mockTenant,
                List.of("admin"),
                List.of("*"),
                List.of("*"),
                List.of("s3:*")
            );

            // When
            Mono<PolicyValidationResult> result = service.validatePolicy(validPolicy);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(validation -> validation.isValid() && validation.errors().isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail validation for invalid policy")
        void shouldFailValidationForInvalidPolicy() {
            // Given
            PolicyInput invalidPolicy = new PolicyInput(
                null, // missing tenant
                List.of(),  // empty roles
                List.of("*"),
                List.of("*"),
                List.of("invalid:action")
            );

            // When
            Mono<PolicyValidationResult> result = service.validatePolicy(invalidPolicy);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(validation -> 
                    !validation.isValid() &&
                    validation.errors().contains("Tenant is required") &&
                    validation.errors().contains("At least one role is required") &&
                    validation.errors().contains("Invalid action: invalid:action")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should validate bucket name patterns")
        void shouldValidateBucketNamePatterns() {
            // Given
            PolicyInput policyWithInvalidBucket = new PolicyInput(
                mockTenant,
                List.of("user"),
                List.of("INVALID_BUCKET_NAME"),  // uppercase not allowed
                List.of("*"),
                List.of("s3:GetObject")
            );

            // When
            Mono<PolicyValidationResult> result = service.validatePolicy(policyWithInvalidBucket);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(validation -> 
                    !validation.isValid() &&
                    validation.errors().stream().anyMatch(e -> e.contains("Invalid bucket name"))
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should validate operation patterns")
        void shouldValidateOperationPatterns() {
            // Given
            List<String> validOperations = List.of(
                "s3:GetObject", "s3:PutObject", "s3:DeleteObject",
                "s3:ListBucket", "s3:*"
            );

            // When/Then - all should be valid
            validOperations.forEach(op -> {
                PolicyInput policy = new PolicyInput(
                    mockTenant, List.of("user"), List.of("*"), List.of("*"), List.of(op)
                );
                Mono<PolicyValidationResult> result = service.validatePolicy(policy);
                StepVerifier.create(result)
                    .expectNextMatches(PolicyValidationResult::isValid)
                    .verifyComplete();
            });
        }
    }

    @Nested
    @DisplayName("Policy Versioning")
    class PolicyVersioning {

        @Test
        @DisplayName("Should create new policy version on update")
        void shouldCreateNewPolicyVersionOnUpdate() {
            // Given
            String policyId = "policy-1";
            PolicyInput updatedInput = new PolicyInput(
                mockTenant, List.of("admin"), List.of("*"), List.of("*"), List.of("s3:*")
            );

            // When
            Mono<PolicyRule> result = service.updatePolicy(mockJwtToken, policyId, updatedInput);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> policy.version() > 1)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should list policy versions")
        void shouldListPolicyVersions() {
            // Given
            String policyId = "policy-1";

            // When
            Flux<PolicyVersion> result = service.getPolicyVersions(mockJwtToken, policyId);

            // Then
            StepVerifier.create(result)
                .expectNextCount(3) // Assuming 3 versions exist
                .verifyComplete();
        }

        @Test
        @DisplayName("Should rollback to previous policy version")
        void shouldRollbackToPreviousPolicyVersion() {
            // Given
            String policyId = "policy-1";
            int targetVersion = 2;

            // When
            Mono<PolicyRule> result = service.rollbackPolicyVersion(mockJwtToken, policyId, targetVersion);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> policy.version() == targetVersion)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should compare policy versions")
        void shouldComparePolicyVersions() {
            // Given
            String policyId = "policy-1";
            int version1 = 1;
            int version2 = 2;

            // When
            Mono<PolicyDiff> result = service.comparePolicyVersions(mockJwtToken, policyId, version1, version2);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(diff -> 
                    diff.addedRoles().size() > 0 ||
                    diff.removedRoles().size() > 0 ||
                    diff.changedBuckets().size() > 0
                )
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("GitOps Integration")
    class GitOpsIntegration {

        @Test
        @DisplayName("Should import policies from Git repository")
        void shouldImportPoliciesFromGitRepository() {
            // Given
            String gitRepoUrl = "https://github.com/org/policies.git";
            String branch = "main";
            String path = "policies/";

            // When
            Flux<PolicyRule> result = service.importPoliciesFromGit(mockJwtToken, gitRepoUrl, branch, path);

            // Then
            StepVerifier.create(result)
                .expectNextCount(5) // Assuming 5 policies in Git
                .verifyComplete();
        }

        @Test
        @DisplayName("Should sync policies with Git repository")
        void shouldSyncPoliciesWithGitRepository() {
            // Given
            String gitRepoUrl = "https://github.com/org/policies.git";

            // When
            Mono<PolicySyncResult> result = service.syncPoliciesWithGit(mockJwtToken, gitRepoUrl);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(syncResult -> 
                    syncResult.addedPolicies() >= 0 &&
                    syncResult.updatedPolicies() >= 0 &&
                    syncResult.deletedPolicies() >= 0
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should export policies to Git repository")
        void shouldExportPoliciesToGitRepository() {
            // Given
            String gitRepoUrl = "https://github.com/org/policies.git";
            String branch = "update-policies";
            String commitMessage = "Update policies for tenant: test-tenant";

            // When
            Mono<GitCommitResult> result = service.exportPoliciesToGit(
                mockJwtToken, gitRepoUrl, branch, commitMessage
            );

            // Then
            StepVerifier.create(result)
                .expectNextMatches(commitResult -> 
                    commitResult.commitSha() != null &&
                    commitResult.filesChanged() > 0
                )
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Support")
    class MultiTenantSupport {

        @Test
        @DisplayName("Should isolate policies by tenant")
        void shouldIsolatePoliciesByTenant() {
            // Given
            String tenant1 = "tenant-a";
            String tenant2 = "tenant-b";

            // When
            Flux<PolicyRule> tenant1Policies = service.getPoliciesByTenant(mockJwtToken, tenant1);
            Flux<PolicyRule> tenant2Policies = service.getPoliciesByTenant(mockJwtToken, tenant2);

            // Then
            StepVerifier.create(tenant1Policies)
                .expectNextMatches(policy -> policy.tenant().equals(tenant1))
                .thenConsumeWhile(policy -> policy.tenant().equals(tenant1))
                .verifyComplete();

            StepVerifier.create(tenant2Policies)
                .expectNextMatches(policy -> policy.tenant().equals(tenant2))
                .thenConsumeWhile(policy -> policy.tenant().equals(tenant2))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to access other tenant's policies")
        void shouldFailToAccessOtherTenantsPolicies() {
            // Given
            String userTenant = "tenant-a";
            String otherPolicyId = "tenant-b-policy";

            // When
            Mono<PolicyRule> result = service.getPolicyById(mockJwtToken, otherPolicyId);

            // Then
            StepVerifier.create(result)
                .expectError(ForbiddenException.class)
                .verify();
        }

        @Test
        @DisplayName("Should apply tenant-specific default policies")
        void shouldApplyTenantSpecificDefaultPolicies() {
            // Given
            String newTenant = "new-tenant";

            // When
            Flux<PolicyRule> result = service.createDefaultPoliciesForTenant(mockJwtToken, newTenant);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> 
                    policy.tenant().equals(newTenant) &&
                    policy.isDefault()
                )
                .thenConsumeWhile(policy -> policy.tenant().equals(newTenant))
                .verifyComplete();
        }
    }

    // Helper method
    private PolicyRule createMockPolicy(String id, String tenant, List<String> roles, 
                                       List<String> buckets, List<String> operations) {
        return new PolicyRule(id, tenant, roles, buckets, List.of("*"), operations, 1, false);
    }
}
