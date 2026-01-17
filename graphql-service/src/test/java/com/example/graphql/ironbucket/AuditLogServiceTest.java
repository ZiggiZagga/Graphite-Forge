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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for AuditLogService
 * Following IronBucket audit logging patterns
 * 
 * Tests cover:
 * - Audit log queries (all, by user, by bucket, by action)
 * - Filtering and pagination
 * - Date range queries
 * - Audit log export
 * - Real-time audit streaming
 * - Audit log retention policies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private AuditLogService service;

    private String mockJwtToken;
    private String mockUser;

    @BeforeEach
    void setUp() {
        mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        mockUser = "testuser";
    }

    @Nested
    @DisplayName("Audit Log Queries")
    class AuditLogQueries {

        @Test
        @DisplayName("Should list all audit logs")
        void shouldListAllAuditLogs() {
            // Given
            List<AuditLogEntry> expectedLogs = List.of(
                createMockAuditLog("1", mockUser, "s3:GetObject", "test-bucket", "file.txt", "SUCCESS"),
                createMockAuditLog("2", mockUser, "s3:PutObject", "test-bucket", "upload.txt", "SUCCESS")
            );

            // When
            Flux<AuditLogEntry> result = service.getAuditLogs(mockJwtToken, 10, 0);

            // Then
            StepVerifier.create(result)
                .expectNextSequence(expectedLogs)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should list audit logs with pagination")
        void shouldListAuditLogsWithPagination() {
            // Given
            int limit = 5;
            int offset = 10;

            // When
            Flux<AuditLogEntry> result = service.getAuditLogs(mockJwtToken, limit, offset);

            // Then
            StepVerifier.create(result)
                .expectNextCount(5)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs by user")
        void shouldGetAuditLogsByUser() {
            // Given
            String user = "alice";

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByUser(mockJwtToken, user);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> log.user().equals(user))
                .thenConsumeWhile(log -> log.user().equals(user))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs by bucket")
        void shouldGetAuditLogsByBucket() {
            // Given
            String bucket = "test-bucket";

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByBucket(mockJwtToken, bucket);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> log.bucket().equals(bucket))
                .thenConsumeWhile(log -> log.bucket().equals(bucket))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs by action")
        void shouldGetAuditLogsByAction() {
            // Given
            String action = "s3:DeleteObject";

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByAction(mockJwtToken, action);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> log.action().equals(action))
                .thenConsumeWhile(log -> log.action().equals(action))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs by result (SUCCESS/FAILURE)")
        void shouldGetAuditLogsByResult() {
            // Given
            String result = "FAILURE";

            // When
            Flux<AuditLogEntry> logs = service.getAuditLogsByResult(mockJwtToken, result);

            // Then
            StepVerifier.create(logs)
                .expectNextMatches(log -> log.result().equals(result))
                .thenConsumeWhile(log -> log.result().equals(result))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Date Range Queries")
    class DateRangeQueries {

        @Test
        @DisplayName("Should get audit logs by date range")
        void shouldGetAuditLogsByDateRange() {
            // Given
            Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
            Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByDateRange(mockJwtToken, startDate, endDate);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> 
                    !log.timestamp().isBefore(startDate) &&
                    !log.timestamp().isAfter(endDate)
                )
                .thenConsumeWhile(log -> 
                    !log.timestamp().isBefore(startDate) &&
                    !log.timestamp().isAfter(endDate)
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs for last 24 hours")
        void shouldGetAuditLogsForLast24Hours() {
            // Given
            Instant now = Instant.now();
            Instant yesterday = now.minusSeconds(86400);

            // When
            Flux<AuditLogEntry> result = service.getRecentAuditLogs(mockJwtToken, 86400);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> !log.timestamp().isBefore(yesterday))
                .thenConsumeWhile(log -> !log.timestamp().isBefore(yesterday))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs for specific date")
        void shouldGetAuditLogsForSpecificDate() {
            // Given
            Instant date = Instant.parse("2026-01-17T00:00:00Z");

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByDate(mockJwtToken, date);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> {
                    Instant logDate = log.timestamp().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                    Instant queryDate = date.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                    return logDate.equals(queryDate);
                })
                .thenConsumeWhile(log -> true)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Advanced Filtering")
    class AdvancedFiltering {

        @Test
        @DisplayName("Should filter audit logs by multiple criteria")
        void shouldFilterAuditLogsByMultipleCriteria() {
            // Given
            AuditLogFilter filter = AuditLogFilter.builder()
                .user("alice")
                .bucket("test-bucket")
                .action("s3:GetObject")
                .result("SUCCESS")
                .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                .endDate(Instant.parse("2026-01-31T23:59:59Z"))
                .build();

            // When
            Flux<AuditLogEntry> result = service.filterAuditLogs(mockJwtToken, filter);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> 
                    log.user().equals("alice") &&
                    log.bucket().equals("test-bucket") &&
                    log.action().equals("s3:GetObject") &&
                    log.result().equals("SUCCESS")
                )
                .thenConsumeWhile(log -> true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should search audit logs by object key pattern")
        void shouldSearchAuditLogsByObjectKeyPattern() {
            // Given
            String keyPattern = "documents/*.pdf";

            // When
            Flux<AuditLogEntry> result = service.searchAuditLogsByObjectKey(mockJwtToken, keyPattern);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> 
                    log.objectKey() != null &&
                    log.objectKey().startsWith("documents/") &&
                    log.objectKey().endsWith(".pdf")
                )
                .thenConsumeWhile(log -> true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get audit logs by IP address")
        void shouldGetAuditLogsByIpAddress() {
            // Given
            String ipAddress = "192.168.1.100";

            // When
            Flux<AuditLogEntry> result = service.getAuditLogsByIpAddress(mockJwtToken, ipAddress);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(log -> log.ipAddress().equals(ipAddress))
                .thenConsumeWhile(log -> log.ipAddress().equals(ipAddress))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Audit Log Export")
    class AuditLogExport {

        @Test
        @DisplayName("Should export audit logs to CSV")
        void shouldExportAuditLogsToCsv() {
            // Given
            AuditLogFilter filter = AuditLogFilter.builder()
                .bucket("test-bucket")
                .build();

            // When
            Mono<byte[]> result = service.exportAuditLogsToCsv(mockJwtToken, filter);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(bytes -> bytes.length > 0)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should export audit logs to JSON")
        void shouldExportAuditLogsToJson() {
            // Given
            AuditLogFilter filter = AuditLogFilter.builder()
                .user("alice")
                .build();

            // When
            Mono<byte[]> result = service.exportAuditLogsToJson(mockJwtToken, filter);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(bytes -> {
                    String json = new String(bytes);
                    return json.contains("\"user\":\"alice\"");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should export audit logs with custom format")
        void shouldExportAuditLogsWithCustomFormat() {
            // Given
            AuditLogFilter filter = AuditLogFilter.builder().build();
            AuditLogExportFormat format = AuditLogExportFormat.builder()
                .format("JSON")
                .includeHeaders(true)
                .fields(List.of("timestamp", "user", "action", "result"))
                .build();

            // When
            Mono<byte[]> result = service.exportAuditLogs(mockJwtToken, filter, format);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(bytes -> bytes.length > 0)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Real-Time Audit Streaming")
    class RealTimeAuditStreaming {

        @Test
        @DisplayName("Should stream audit logs in real-time")
        void shouldStreamAuditLogsInRealTime() {
            // Given
            String bucket = "test-bucket";

            // When
            Flux<AuditLogEntry> stream = service.streamAuditLogs(mockJwtToken, bucket);

            // Then - should receive new logs as they come
            StepVerifier.create(stream.take(5))
                .expectNextCount(5)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should stream audit logs with filter")
        void shouldStreamAuditLogsWithFilter() {
            // Given
            AuditLogFilter filter = AuditLogFilter.builder()
                .action("s3:DeleteObject")
                .result("SUCCESS")
                .build();

            // When
            Flux<AuditLogEntry> stream = service.streamFilteredAuditLogs(mockJwtToken, filter);

            // Then
            StepVerifier.create(stream.take(3))
                .expectNextMatches(log -> 
                    log.action().equals("s3:DeleteObject") &&
                    log.result().equals("SUCCESS")
                )
                .expectNextCount(2)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle stream disconnection and reconnect")
        void shouldHandleStreamDisconnectionAndReconnect() {
            // Given
            String bucket = "test-bucket";

            // When
            Flux<AuditLogEntry> stream = service.streamAuditLogsWithRetry(mockJwtToken, bucket, 3);

            // Then - should retry on disconnection
            StepVerifier.create(stream.take(10))
                .expectNextCount(10)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Audit Statistics")
    class AuditStatisticsTests {

        @Test
        @DisplayName("Should get audit statistics summary")
        void shouldGetAuditStatisticsSummary() {
            // Given
            Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
            Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

            // When
            Mono<AuditStatistics> result = service.getAuditStatistics(mockJwtToken, startDate, endDate);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(stats -> 
                    stats.totalOperations() > 0 &&
                    stats.successfulOperations() >= 0 &&
                    stats.failedOperations() >= 0 &&
                    stats.uniqueUsers() > 0
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get top users by activity")
        void shouldGetTopUsersByActivity() {
            // Given
            int topN = 10;

            // When
            Flux<UserActivitySummary> result = service.getTopUsersByActivity(mockJwtToken, topN);

            // Then
            StepVerifier.create(result)
                .expectNextCount(topN)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get most accessed buckets")
        void shouldGetMostAccessedBuckets() {
            // Given
            int topN = 5;

            // When
            Flux<BucketAccessSummary> result = service.getMostAccessedBuckets(mockJwtToken, topN);

            // Then
            StepVerifier.create(result)
                .expectNextCount(5)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get operation distribution")
        void shouldGetOperationDistribution() {
            // When
            Flux<OperationCount> result = service.getOperationDistribution(mockJwtToken);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(opCount -> 
                    opCount.operation().startsWith("s3:") &&
                    opCount.count() > 0
                )
                .thenConsumeWhile(opCount -> true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get hourly activity chart")
        void shouldGetHourlyActivityChart() {
            // Given
            Instant date = Instant.parse("2026-01-17T00:00:00Z");

            // When
            Flux<HourlyActivity> result = service.getHourlyActivity(mockJwtToken, date);

            // Then
            StepVerifier.create(result)
                .expectNextCount(24) // 24 hours
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Audit Retention Policies")
    class AuditRetentionPolicies {

        @Test
        @DisplayName("Should get audit retention policy")
        void shouldGetAuditRetentionPolicy() {
            // When
            Mono<AuditRetentionPolicy> result = service.getRetentionPolicy(mockJwtToken);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(policy -> 
                    policy.retentionDays() > 0 &&
                    policy.archiveEnabled() == true
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should update audit retention policy (admin only)")
        void shouldUpdateAuditRetentionPolicy() {
            // Given
            AuditRetentionPolicy newPolicy = AuditRetentionPolicy.builder()
                .retentionDays(90)
                .archiveEnabled(true)
                .archiveLocation("s3://archive-bucket/audit-logs/")
                .build();

            // When
            Mono<AuditRetentionPolicy> result = service.updateRetentionPolicy(mockJwtToken, newPolicy);

            // Then
            StepVerifier.create(result)
                .expectNext(newPolicy)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to update retention policy without admin role")
        void shouldFailToUpdateRetentionPolicyWithoutAdminRole() {
            // Given
            String userToken = "user-jwt-token";
            AuditRetentionPolicy newPolicy = AuditRetentionPolicy.builder()
                .retentionDays(90)
                .build();

            // When
            Mono<AuditRetentionPolicy> result = service.updateRetentionPolicy(userToken, newPolicy);

            // Then
            StepVerifier.create(result)
                .expectError(ForbiddenException.class)
                .verify();
        }

        @Test
        @DisplayName("Should archive old audit logs")
        void shouldArchiveOldAuditLogs() {
            // Given
            Instant archiveBefore = Instant.now().minusSeconds(90 * 86400); // 90 days ago

            // When
            Mono<ArchiveResult> result = service.archiveAuditLogs(mockJwtToken, archiveBefore);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(archiveResult -> 
                    archiveResult.archivedCount() >= 0 &&
                    archiveResult.archiveLocation() != null
                )
                .verifyComplete();
        }
    }

    // Helper method
    private AuditLogEntry createMockAuditLog(String id, String user, String action, 
                                             String bucket, String objectKey, String result) {
        return new AuditLogEntry(
            id, Instant.now(), user, action, bucket, objectKey, result, "192.168.1.1"
        );
    }
}
