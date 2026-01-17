package com.example.graphql.ironbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
/**
 * Service for querying audit logs from IronBucket audit service.
 * All audit operations flow through Sentinel-Gear for authentication.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final WebClient webClient;
    private final String sentinelGearBaseUrl;

    public AuditLogService(
            WebClient.Builder webClientBuilder,
            @Value("${ironbucket.sentinel-gear.base-url:http://localhost:8080}") String sentinelGearBaseUrl
    ) {
        this.sentinelGearBaseUrl = sentinelGearBaseUrl;
        this.webClient = webClientBuilder
                .baseUrl(sentinelGearBaseUrl)
                .build();
        log.info("AuditLogService initialized with Sentinel-Gear at {}", sentinelGearBaseUrl);
    }

    /**
     * Get audit logs with pagination.
     */
    public Flux<AuditLogEntry> getAuditLogs(String jwtToken, int limit, int offset) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit/logs")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by user.
     */
    public Flux<AuditLogEntry> getAuditLogsByUser(String jwtToken, String user) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/user/{user}", user)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs for user: {}", user, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by bucket.
     */
    public Flux<AuditLogEntry> getAuditLogsByBucket(String jwtToken, String bucketName) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/bucket/{bucketName}", bucketName)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs for bucket: {}", bucketName, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by action type.
     */
    public Flux<AuditLogEntry> getAuditLogsByAction(String jwtToken, String action) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/action/{action}", action)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs for action: {}", action, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by date range.
     */
    public Flux<AuditLogEntry> getAuditLogsByDateRange(String jwtToken, Instant startDate, Instant endDate) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit/logs/range")
                        .queryParam("start", startDate.toString())
                        .queryParam("end", endDate.toString())
                        .build())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs for date range", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Count audit logs matching criteria.
     */
    public Mono<Long> countAuditLogs(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/count")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Long.class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to count audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by result status.
     */
    public Flux<AuditLogEntry> getAuditLogsByResult(String jwtToken, String result) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/result/{result}", result)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs by result: {}", result, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get recent audit logs.
     */
    public Flux<AuditLogEntry> getRecentAuditLogs(String jwtToken, int limit) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit/logs/recent")
                        .queryParam("limit", limit)
                        .build())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get recent audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Filter audit logs with custom filter criteria.
     */
    public Flux<AuditLogEntry> filterAuditLogs(String jwtToken, AuditLogFilter filter) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/audit/logs/filter");
                    if (filter.users() != null && !filter.users().isEmpty()) {
                        builder.queryParam("users", String.join(",", filter.users()));
                    }
                    if (filter.actions() != null && !filter.actions().isEmpty()) {
                        builder.queryParam("actions", String.join(",", filter.actions()));
                    }
                    if (filter.buckets() != null && !filter.buckets().isEmpty()) {
                        builder.queryParam("buckets", String.join(",", filter.buckets()));
                    }
                    if (filter.results() != null && !filter.results().isEmpty()) {
                        builder.queryParam("results", String.join(",", filter.results()));
                    }
                    if (filter.startDate() != null) {
                        builder.queryParam("start", filter.startDate().toString());
                    }
                    if (filter.endDate() != null) {
                        builder.queryParam("end", filter.endDate().toString());
                    }
                    return builder.build();
                })
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to filter audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Export audit logs to CSV format.
     */
    public Mono<byte[]> exportAuditLogsToCsv(String jwtToken, AuditLogFilter filter) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/audit/logs/export/csv")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to export audit logs to CSV", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Export audit logs to JSON format.
     */
    public Mono<byte[]> exportAuditLogsToJson(String jwtToken, AuditLogFilter filter) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/audit/logs/export/json")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to export audit logs to JSON", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Export audit logs with custom format specification.
     */
    public Mono<byte[]> exportAuditLogs(String jwtToken, AuditLogFilter filter, AuditLogExportFormat format) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/audit/logs/export")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(Map.of("filter", filter, "format", format))
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to export audit logs with custom format", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Stream filtered audit logs as a continuous flux.
     */
    public Flux<AuditLogEntry> streamFilteredAuditLogs(String jwtToken, AuditLogFilter filter) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/audit/logs/stream");
                    if (filter.users() != null && !filter.users().isEmpty()) {
                        builder.queryParam("users", String.join(",", filter.users()));
                    }
                    if (filter.actions() != null && !filter.actions().isEmpty()) {
                        builder.queryParam("actions", String.join(",", filter.actions()));
                    }
                    if (filter.buckets() != null && !filter.buckets().isEmpty()) {
                        builder.queryParam("buckets", String.join(",", filter.buckets()));
                    }
                    if (filter.results() != null && !filter.results().isEmpty()) {
                        builder.queryParam("results", String.join(",", filter.results()));
                    }
                    return builder.build();
                })
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to stream filtered audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by date.
     */
    public Flux<AuditLogEntry> getAuditLogsByDate(String jwtToken, Instant date) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit/logs/date")
                        .queryParam("date", date.toString())
                        .build())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs by date", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Search audit logs by object key.
     */
    public Flux<AuditLogEntry> searchAuditLogsByObjectKey(String jwtToken, String objectKey) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/search/object/{objectKey}", objectKey)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to search audit logs by object key: {}", objectKey, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get audit logs by IP address.
     */
    public Flux<AuditLogEntry> getAuditLogsByIpAddress(String jwtToken, String ipAddress) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/ip/{ipAddress}", ipAddress)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit logs by IP: {}", ipAddress, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Stream audit logs in real-time.
     */
    public Flux<AuditLogEntry> streamAuditLogs(String jwtToken, String filter) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/logs/stream?filter={filter}", filter)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AuditLogEntryDto.class)
                .map(this::toAuditLogEntry)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to stream audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Stream audit logs with retry.
     */
    public Flux<AuditLogEntry> streamAuditLogsWithRetry(String jwtToken, String filter, int maxRetries) {
        validateJwt(jwtToken);
        
        return streamAuditLogs(jwtToken, filter)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10)));
    }

    /**
     * Get audit statistics for a date range.
     */
    public Mono<AuditStatistics> getAuditStatistics(String jwtToken, Instant startDate, Instant endDate) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/audit/statistics")
                        .queryParam("start", startDate.toString())
                        .queryParam("end", endDate.toString())
                        .build())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(AuditStatisticsDto.class)
                .map(dto -> new AuditStatistics(dto.totalOperations(), dto.successfulOperations(),
                                                 dto.failedOperations(), dto.uniqueUsers()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get audit statistics", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get top users by activity.
     */
    public Flux<UserActivitySummary> getTopUsersByActivity(String jwtToken, int limit) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/statistics/top-users?limit={limit}", limit)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(UserActivitySummaryDto.class)
                .map(dto -> new UserActivitySummary(dto.user(), dto.totalOperations(),
                                                     dto.successfulOperations(), dto.failedOperations()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get top users", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get most accessed buckets.
     */
    public Flux<BucketAccessSummary> getMostAccessedBuckets(String jwtToken, int limit) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/statistics/top-buckets?limit={limit}", limit)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(BucketAccessSummaryDto.class)
                .map(dto -> new BucketAccessSummary(dto.bucket(), dto.accessCount(), dto.uniqueUsers()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get most accessed buckets", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get operation distribution.
     */
    public Flux<OperationCount> getOperationDistribution(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/statistics/operations")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(OperationCountDto.class)
                .map(dto -> new OperationCount(dto.operation(), dto.count()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get operation distribution", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get hourly activity.
     */
    public Flux<HourlyActivity> getHourlyActivity(String jwtToken, Instant date) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/statistics/hourly?date={date}", date.toString())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(HourlyActivityDto.class)
                .map(dto -> new HourlyActivity(dto.hour(), dto.operationCount()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get hourly activity", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get retention policy.
     */
    public Mono<AuditRetentionPolicy> getRetentionPolicy(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/audit/retention/policy")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(AuditRetentionPolicyDto.class)
                .map(dto -> new AuditRetentionPolicy(Duration.ofDays(dto.retentionDays()),
                                                      dto.archiveEnabled(), dto.archiveLocation()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get retention policy", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Update retention policy.
     */
    public Mono<AuditRetentionPolicy> updateRetentionPolicy(String jwtToken, AuditRetentionPolicy policy) {
        validateJwt(jwtToken);
        
        return webClient.put()
                .uri("/api/v1/audit/retention/policy")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new AuditRetentionPolicyDto((int) policy.retentionPeriod().toDays(),
                                                        policy.archiveEnabled(), policy.archiveLocation()))
                .retrieve()
                .bodyToMono(AuditRetentionPolicyDto.class)
                .map(dto -> new AuditRetentionPolicy(Duration.ofDays(dto.retentionDays()),
                                                      dto.archiveEnabled(), dto.archiveLocation()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to update retention policy", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Archive audit logs older than specified date.
     */
    public Mono<ArchiveResult> archiveAuditLogs(String jwtToken, Instant beforeDate) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/audit/archive?before={before}", beforeDate.toString())
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(ArchiveResultDto.class)
                .map(dto -> new ArchiveResult(dto.success(), dto.archivedCount(),
                                               dto.archiveLocation(), dto.archiveTime()))
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to archive audit logs", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    // --- Validation ---

    private void validateJwt(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IllegalArgumentException("JWT token is required");
        }
    }

    // --- Mapping ---

    private AuditLogEntry toAuditLogEntry(AuditLogEntryDto dto) {
        return new AuditLogEntry(
                dto.id(),
                dto.timestamp(),
                dto.user(),
                dto.action(),
                dto.bucket(),
                dto.objectKey(),
                dto.result(),
                dto.ipAddress()
        );
    }

    // --- Error Handling ---

    private <T> Mono<T> handleWebClientError(WebClientResponseException e) {
        log.error("WebClient error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        return Mono.error(new IronBucketServiceException("IronBucket audit service error: " + e.getMessage(), e));
    }

    private Retry retrySpec() {
        return Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable
                        || throwable instanceof WebClientResponseException.GatewayTimeout)
                .doBeforeRetry(signal -> log.warn("Retrying request, attempt: {}", signal.totalRetries() + 1));
    }

    // --- DTOs ---

    record AuditLogEntryDto(
        String id,
        Instant timestamp,
        String user,
        String action,
        String bucket,
        String objectKey,
        String result,
        String ipAddress
    ) {}
    
    record AuditStatisticsDto(
        long totalOperations,
        long successfulOperations,
        long failedOperations,
        long uniqueUsers
    ) {}
    
    record UserActivitySummaryDto(
        String user,
        long totalOperations,
        long successfulOperations,
        long failedOperations
    ) {}
    
    record BucketAccessSummaryDto(
        String bucket,
        long accessCount,
        long uniqueUsers
    ) {}
    
    record OperationCountDto(String operation, long count) {}
    record HourlyActivityDto(int hour, long operationCount) {}
    
    record AuditRetentionPolicyDto(
        int retentionDays,
        boolean archiveEnabled,
        String archiveLocation
    ) {}
    
    record ArchiveResultDto(
        boolean success,
        long archivedCount,
        String archiveLocation,
        Instant archiveTime
    ) {}
}
