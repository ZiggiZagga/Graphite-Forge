package com.example.graphql.ironbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for managing IronBucket policies through Claimspindel.
 * All policy operations flow through Sentinel-Gear for authentication and authorization.
 * 
 * Security: Only admin users can create/update/delete policies.
 */
@Service
public class PolicyManagementService {

    private static final Logger log = LoggerFactory.getLogger(PolicyManagementService.class);

    private final WebClient webClient;
    private final String sentinelGearBaseUrl;

    public PolicyManagementService(
            WebClient.Builder webClientBuilder,
            @Value("${ironbucket.sentinel-gear.base-url:http://localhost:8080}") String sentinelGearBaseUrl
    ) {
        this.sentinelGearBaseUrl = sentinelGearBaseUrl;
        this.webClient = webClientBuilder
                .baseUrl(sentinelGearBaseUrl)
                .build();
        log.info("PolicyManagementService initialized with Sentinel-Gear at {}", sentinelGearBaseUrl);
    }

    /**
     * List all policies accessible to the authenticated user.
     */
    public Flux<PolicyRule> listPolicies(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/policies")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to list policies", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get a policy by ID.
     */
    public Mono<PolicyRule> getPolicyById(String jwtToken, String policyId) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.get()
                .uri("/api/v1/policies/{policyId}", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new PolicyNotFoundException(policyId, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Create a new policy.
     */
    public Mono<PolicyRule> createPolicy(String jwtToken, PolicyInput policyInput) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(policyInput)
                .retrieve()
                .bodyToMono(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnSuccess(policy -> log.info("Created policy: {}", policy.id()))
                .doOnError(e -> log.error("Failed to create policy", e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.error(new ForbiddenException("Insufficient permissions to create policy", e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Update an existing policy.
     */
    public Mono<PolicyRule> updatePolicy(String jwtToken, String policyId, PolicyInput policyInput) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.put()
                .uri("/api/v1/policies/{policyId}", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(policyInput)
                .retrieve()
                .bodyToMono(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnSuccess(policy -> log.info("Updated policy: {}", policyId))
                .doOnError(e -> log.error("Failed to update policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new PolicyNotFoundException(policyId, e));
                    }
                    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.error(new ForbiddenException("Insufficient permissions to update policy", e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Delete a policy.
     */
    public Mono<Boolean> deletePolicy(String jwtToken, String policyId) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.delete()
                .uri("/api/v1/policies/{policyId}", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .retryWhen(retrySpec())
                .doOnSuccess(result -> log.info("Deleted policy: {}", policyId))
                .doOnError(e -> log.error("Failed to delete policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new PolicyNotFoundException(policyId, e));
                    }
                    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.error(new ForbiddenException("Insufficient permissions to delete policy", e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * List policies by tenant.
     */
    public Flux<PolicyRule> listPoliciesByTenant(String jwtToken, String tenantId) {
        validateJwt(jwtToken);
        
        return listPolicies(jwtToken)
                .filter(policy -> tenantId.equals(policy.tenant()));
    }

    /**
     * List policies by role.
     */
    public Flux<PolicyRule> listPoliciesByRole(String jwtToken, String role) {
        validateJwt(jwtToken);
        
        return listPolicies(jwtToken)
                .filter(policy -> policy.roles().contains(role));
    }

    /**
     * Validate a policy before creation/update.
     */
    public Mono<PolicyValidationResult> validatePolicy(PolicyInput policyInput) {
        return webClient.post()
                .uri("/api/v1/policies/validate")
                .bodyValue(policyInput)
                .retrieve()
                .bodyToMono(PolicyValidationResultDto.class)
                .map(dto -> new PolicyValidationResult(dto.valid(), dto.errors()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to validate policy", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get policies filtered by tenant (wrapper around listPoliciesByTenant).
     */
    public Flux<PolicyRule> getPoliciesByTenant(String jwtToken, String tenantId) {
        return listPoliciesByTenant(jwtToken, tenantId);
    }

    /**
     * Get policies filtered by role (wrapper around listPoliciesByRole).
     */
    public Flux<PolicyRule> getPoliciesByRole(String jwtToken, String role) {
        return listPoliciesByRole(jwtToken, role);
    }

    /**
     * Get policies that apply to a specific bucket.
     */
    public Flux<PolicyRule> getPoliciesByBucket(String jwtToken, String bucketName) {
        validateJwt(jwtToken);
        
        return listPolicies(jwtToken)
                .filter(policy -> policy.buckets().contains(bucketName) || policy.buckets().contains("*"));
    }

    /**
     * Dry-run policy evaluation (simulate policy without applying it).
     */
    public Flux<PolicyEvaluationResult> dryRunPolicy(String jwtToken, PolicyInput policyInput, 
                                                      List<String> testActions, String testBucket) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/dry-run")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new DryRunRequest(policyInput, testActions, testBucket))
                .retrieve()
                .bodyToFlux(PolicyEvaluationResultDto.class)
                .map(dto -> new PolicyEvaluationResult(
                        PolicyDecision.valueOf(dto.decision()),
                        dto.matchedPolicies(),
                        dto.reason()
                ))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to dry-run policy", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Dry-run policy evaluation with operation and resource.
     */
    public Mono<PolicyDecision> dryRunPolicy(String jwtToken, PolicyInput policyInput, 
                                              String operation, String resource) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/dry-run/simple")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new DryRunRequest(policyInput, List.of(operation), resource))
                .retrieve()
                .bodyToMono(PolicyEvaluationResultDto.class)
                .map(dto -> PolicyDecision.valueOf(dto.decision()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to dry-run policy", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get all versions of a policy.
     */
    public Flux<PolicyVersion> getPolicyVersions(String jwtToken, String policyId) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.get()
                .uri("/api/v1/policies/{policyId}/versions", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(PolicyVersionDto.class)
                .map(dto -> new PolicyVersion(dto.policyId(), dto.version(), dto.createdAt(), 
                                               dto.createdBy(), dto.changeDescription()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get policy versions: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Rollback policy to a previous version.
     */
    public Mono<PolicyRule> rollbackPolicyVersion(String jwtToken, String policyId, int version) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.post()
                .uri("/api/v1/policies/{policyId}/rollback/{version}", policyId, version)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnSuccess(policy -> log.info("Rolled back policy {} to version {}", policyId, version))
                .doOnError(e -> log.error("Failed to rollback policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Compare two versions of a policy.
     */
    public Mono<PolicyDiff> comparePolicyVersions(String jwtToken, String policyId, int fromVersion, int toVersion) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.get()
                .uri("/api/v1/policies/{policyId}/compare/{fromVersion}/{toVersion}", policyId, fromVersion, toVersion)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(PolicyDiffDto.class)
                .map(dto -> new PolicyDiff(dto.policyId(), dto.fromVersion(), dto.toVersion(),
                                            dto.addedRoles(), dto.removedRoles(),
                                            dto.addedBuckets(), dto.removedBuckets(),
                                            dto.addedOperations(), dto.removedOperations()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to compare policy versions: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Evaluate a policy against a specific context.
     */
    public Mono<PolicyEvaluationResult> evaluatePolicy(String jwtToken, String policyId, PolicyEvaluationContext context) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.post()
                .uri("/api/v1/policies/{policyId}/evaluate", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(context)
                .retrieve()
                .bodyToMono(PolicyEvaluationResultDto.class)
                .map(dto -> new PolicyEvaluationResult(
                        PolicyDecision.valueOf(dto.decision()),
                        dto.matchedPolicies(),
                        dto.reason()
                ))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to evaluate policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get explanation of why a policy decision was made.
     */
    public Mono<PolicyExplanation> explainPolicy(String jwtToken, String policyId, PolicyEvaluationContext context) {
        validateJwt(jwtToken);
        validatePolicyId(policyId);
        
        return webClient.post()
                .uri("/api/v1/policies/{policyId}/explain", policyId)
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(context)
                .retrieve()
                .bodyToMono(PolicyExplanationDto.class)
                .map(dto -> new PolicyExplanation(
                        PolicyDecision.valueOf(dto.decision()),
                        dto.evaluationSteps(),
                        dto.matchedRules(),
                        dto.failedConditions(),
                        dto.reason()
                ))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to explain policy: {}", policyId, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Create default policies for a tenant.
     */
    public Flux<PolicyRule> createDefaultPoliciesForTenant(String jwtToken, String tenant) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/defaults")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(Map.of("tenant", tenant))
                .retrieve()
                .bodyToFlux(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to create default policies for tenant: {}", tenant, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Export policies to a Git repository.
     */
    public Mono<GitCommitResult> exportPoliciesToGit(String jwtToken, String branch, String message, String exportPath) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/export/git")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new GitExportRequest(branch, message, exportPath))
                .retrieve()
                .bodyToMono(GitCommitResultDto.class)
                .map(dto -> new GitCommitResult(
                        dto.success(),
                        dto.commitHash(),
                        dto.branch(),
                        dto.commitTime(),
                        dto.message(),
                        dto.filesCount()
                ))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to export policies to Git", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Import policies from a Git repository.
     */
    public Flux<PolicyRule> importPoliciesFromGit(String jwtToken, String gitUrl, String branch, String path) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/import/git")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new GitImportRequest(gitUrl, branch, path))
                .retrieve()
                .bodyToFlux(PolicyRuleDto.class)
                .map(this::toPolicy)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to import policies from Git", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Synchronize policies with Git repository.
     */
    public Mono<PolicySyncResult> syncPoliciesWithGit(String jwtToken, String gitUrl) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/sync/git")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new GitSyncRequest(gitUrl))
                .retrieve()
                .bodyToMono(PolicySyncResultDto.class)
                .map(dto -> new PolicySyncResult(dto.success(), dto.syncTime(), dto.policiesAdded(),
                                                  dto.policiesUpdated(), dto.policiesDeleted(), dto.errors()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to sync policies with Git", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Commit policy changes to Git.
     */
    public Mono<GitCommitResult> commitPoliciesToGit(String jwtToken, String message) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/policies/commit/git")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new GitCommitRequest(message))
                .retrieve()
                .bodyToMono(GitCommitResultDto.class)
                .map(dto -> new GitCommitResult(dto.success(), dto.commitHash(), dto.branch(),
                                                 dto.commitTime(), dto.message(), dto.filesCount()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to commit policies to Git", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    // --- Validation ---

    private void validateJwt(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IllegalArgumentException("JWT token is required");
        }
    }

    private void validatePolicyId(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be null or blank");
        }
    }

    // --- Mapping ---

    private PolicyRule toPolicy(PolicyRuleDto dto) {
        return new PolicyRule(
                dto.id(),
                dto.tenant(),
                dto.roles(),
                dto.buckets(),
                dto.tags(),
                dto.operations(),
                dto.version(),
                dto.deleted()
        );
    }

    // --- Error Handling ---

    private <T> Mono<T> handleWebClientError(WebClientResponseException e) {
        log.error("WebClient error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        return Mono.error(new IronBucketServiceException("IronBucket policy service error: " + e.getMessage(), e));
    }

    private Retry retrySpec() {
        return Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable
                        || throwable instanceof WebClientResponseException.GatewayTimeout)
                .doBeforeRetry(signal -> log.warn("Retrying request, attempt: {}", signal.totalRetries() + 1));
    }

    // --- DTOs ---

    record PolicyRuleDto(
        String id,
        String tenant,
        List<String> roles,
        List<String> buckets,
        List<String> tags,
        List<String> operations,
        int version,
        boolean deleted
    ) {}
    
    record PolicyValidationResultDto(
        boolean valid,
        List<String> errors
    ) {}
    
    record DryRunRequest(
        PolicyInput policy,
        List<String> testActions,
        String testBucket
    ) {}
    
    record PolicyEvaluationResultDto(
        String decision,
        List<String> matchedPolicies,
        String reason
    ) {}
    
    record PolicyVersionDto(
        String policyId,
        int version,
        Instant createdAt,
        String createdBy,
        String changeDescription
    ) {}
    
    record PolicyDiffDto(
        String policyId,
        int fromVersion,
        int toVersion,
        List<String> addedRoles,
        List<String> removedRoles,
        List<String> addedBuckets,
        List<String> removedBuckets,
        List<String> addedOperations,
        List<String> removedOperations
    ) {}
    
    record GitImportRequest(String gitUrl, String branch, String path) {}
    record GitSyncRequest(String gitUrl) {}
    record GitCommitRequest(String message) {}
    
    record PolicySyncResultDto(
        boolean success,
        Instant syncTime,
        int policiesAdded,
        int policiesUpdated,
        int policiesDeleted,
        List<String> errors
    ) {}
    
    record GitCommitResultDto(
        boolean success,
        String commitHash,
        String branch,
        Instant commitTime,
        String message,
        int filesCount
    ) {}
    
    record GitExportRequest(String branch, String message, String exportPath) {}
    
    record PolicyExplanationDto(
        String decision,
        List<String> evaluationSteps,
        List<String> matchedRules,
        List<String> failedConditions,
        String reason
    ) {}
}

