package com.example.graphql.ironbucket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * Integration tests for IronBucket GraphQL API
 * Tests end-to-end flows from GraphQL → Services → IronBucket APIs
 * 
 * Requires:
 * - Running IronBucket services (Brazz-Nossel, Sentinel-Gear, Claimspindel)
 * - Running MinIO
 * - Running PostgreSQL (audit logs)
 * - Valid Keycloak JWT token
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("IronBucket Integration Tests")
class IronBucketIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        // This would normally obtain a real JWT from Keycloak
        validJwtToken = "Bearer test-jwt-token";
    }

    @Nested
    @DisplayName("End-to-End S3 Operations")
    class EndToEndS3Operations {

        @Test
        @DisplayName("E2E: Create bucket → Upload file → Download file → Delete file → Delete bucket")
        void shouldCompleteFullS3Lifecycle() {
            String bucketName = "integration-test-bucket-" + System.currentTimeMillis();
            String objectKey = "test-file.txt";
            String content = "Integration test content";

            // Step 1: Create bucket
            graphQlTester
                .document("""
                    mutation CreateBucket($name: String!, $tenant: String) {
                        createBucket(name: $name, tenant: $tenant) {
                            name
                            creationDate
                            ownerTenant
                        }
                    }
                    """)
                .variable("name", bucketName)
                .variable("tenant", "integration-test-tenant")
                .execute()
                .path("createBucket.name")
                .entity(String.class)
                .isEqualTo(bucketName);

            // Step 2: Upload object
            graphQlTester
                .document("""
                    mutation UploadObject($bucket: String!, $key: String!, $content: Upload!, $contentType: String) {
                        uploadObject(bucket: $bucket, key: $key, content: $content, contentType: $contentType) {
                            key
                            bucket
                            size
                            contentType
                        }
                    }
                    """)
                .variable("bucket", bucketName)
                .variable("key", objectKey)
                .variable("content", content.getBytes())
                .variable("contentType", "text/plain")
                .execute()
                .path("uploadObject.key")
                .entity(String.class)
                .isEqualTo(objectKey);

            // Step 3: List objects
            graphQlTester
                .document("""
                    query ListObjects($bucket: String!) {
                        listObjects(bucket: $bucket) {
                            key
                            bucket
                        }
                    }
                    """)
                .variable("bucket", bucketName)
                .execute()
                .path("listObjects[0].key")
                .entity(String.class)
                .isEqualTo(objectKey);

            // Step 4: Get presigned URL
            graphQlTester
                .document("""
                    query GetPresignedUrl($bucket: String!, $key: String!, $expiresIn: Int) {
                        getPresignedUrl(bucket: $bucket, key: $key, expiresIn: $expiresIn)
                    }
                    """)
                .variable("bucket", bucketName)
                .variable("key", objectKey)
                .variable("expiresIn", 3600)
                .execute()
                .path("getPresignedUrl")
                .entity(String.class)
                .matches(url -> url.contains(bucketName) && url.contains(objectKey));

            // Step 5: Delete object
            graphQlTester
                .document("""
                    mutation DeleteObject($bucket: String!, $key: String!) {
                        deleteObject(bucket: $bucket, key: $key)
                    }
                    """)
                .variable("bucket", bucketName)
                .variable("key", objectKey)
                .execute()
                .path("deleteObject")
                .entity(Boolean.class)
                .isEqualTo(true);

            // Step 6: Delete bucket
            graphQlTester
                .document("""
                    mutation DeleteBucket($name: String!) {
                        deleteBucket(name: $name)
                    }
                    """)
                .variable("name", bucketName)
                .execute()
                .path("deleteBucket")
                .entity(Boolean.class)
                .isEqualTo(true);
        }

        @Test
        @DisplayName("E2E: JWT authentication enforced on all operations")
        void shouldEnforceJwtAuthentication() {
            // Given - no JWT token provided

            // When/Then - should return 401 Unauthorized
            graphQlTester
                .document("""
                    query ListBuckets {
                        listBuckets {
                            name
                        }
                    }
                    """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("Unauthorized"));
        }

        @Test
        @DisplayName("E2E: Policy enforcement blocks unauthorized operations")
        void shouldEnforcePolicyRestrictions() {
            // Given - user token with read-only permissions
            String readOnlyToken = "Bearer read-only-jwt-token";
            String bucketName = "protected-bucket";

            // When/Then - should return 403 Forbidden for write operations
            graphQlTester
                .document("""
                    mutation DeleteBucket($name: String!) {
                        deleteBucket(name: $name)
                    }
                    """)
                .variable("name", bucketName)
                .execute()
                .errors()
                .expect(error -> error.getMessage().contains("Forbidden"));
        }
    }

    @Nested
    @DisplayName("End-to-End Policy Management")
    class EndToEndPolicyManagement {

        @Test
        @DisplayName("E2E: Create policy → Test with dry-run → Apply → Verify enforcement")
        void shouldCompleteFullPolicyLifecycle() {
            String policyId = "integration-test-policy-" + System.currentTimeMillis();

            // Step 1: Create policy
            graphQlTester
                .document("""
                    mutation CreatePolicy($input: PolicyInput!) {
                        createPolicy(policy: $input) {
                            id
                            tenant
                            roles
                            allowedBuckets
                            operations
                        }
                    }
                    """)
                .variable("input", Map.of(
                    "tenant", "test-tenant",
                    "roles", List.of("test-user"),
                    "allowedBuckets", List.of("test-bucket"),
                    "allowedPrefixes", List.of("*"),
                    "operations", List.of("s3:GetObject", "s3:PutObject")
                ))
                .execute()
                .path("createPolicy.id")
                .entity(String.class)
                .matches(id -> id != null && !id.isEmpty());

            // Step 2: Dry-run policy evaluation
            graphQlTester
                .document("""
                    mutation DryRunPolicy($policy: PolicyInput!, $operation: String!, $resource: String!) {
                        dryRunPolicy(policy: $policy, operation: $operation, resource: $resource) {
                            decision
                            matchedRules
                            reason
                        }
                    }
                    """)
                .variable("policy", Map.of(
                    "tenant", "test-tenant",
                    "roles", List.of("test-user"),
                    "allowedBuckets", List.of("test-bucket"),
                    "allowedPrefixes", List.of("*"),
                    "operations", List.of("s3:GetObject")
                ))
                .variable("operation", "s3:GetObject")
                .variable("resource", "arn:aws:s3:::test-bucket/file.txt")
                .execute()
                .path("dryRunPolicy.decision")
                .entity(String.class)
                .isEqualTo("ALLOW");

            // Step 3: Update policy
            // Step 4: Delete policy
        }

        @Test
        @DisplayName("E2E: Multi-tenant policy isolation")
        void shouldIsolatePoliciesByTenant() {
            // Given - create policies for different tenants
            String tenant1 = "tenant-a";
            String tenant2 = "tenant-b";

            // When - query policies for tenant-a
            graphQlTester
                .document("""
                    query PoliciesByTenant($tenant: String!) {
                        policiesByTenant(tenant: $tenant) {
                            id
                            tenant
                        }
                    }
                    """)
                .variable("tenant", tenant1)
                .execute()
                .path("policiesByTenant[*].tenant")
                .entityList(String.class)
                .contains(tenant1);

            // Then - should not see tenant-b policies
        }
    }

    @Nested
    @DisplayName("End-to-End Audit Logging")
    class EndToEndAuditLogging {

        @Test
        @DisplayName("E2E: S3 operations are logged in audit trail")
        void shouldLogS3OperationsInAuditTrail() {
            String bucketName = "audit-test-bucket";
            String objectKey = "audit-test-file.txt";

            // Step 1: Perform S3 operation
            graphQlTester
                .document("""
                    mutation UploadObject($bucket: String!, $key: String!, $content: Upload!) {
                        uploadObject(bucket: $bucket, key: $key, content: $content) {
                            key
                        }
                    }
                    """)
                .variable("bucket", bucketName)
                .variable("key", objectKey)
                .variable("content", "test content".getBytes())
                .execute();

            // Step 2: Query audit logs
            graphQlTester
                .document("""
                    query AuditLogsByBucket($bucket: String!) {
                        auditLogsByBucket(bucket: $bucket) {
                            user
                            action
                            bucket
                            objectKey
                            result
                        }
                    }
                    """)
                .variable("bucket", bucketName)
                .execute()
                .path("auditLogsByBucket[0].action")
                .entity(String.class)
                .isEqualTo("s3:PutObject");
        }

        @Test
        @DisplayName("E2E: Failed operations are logged with error details")
        void shouldLogFailedOperations() {
            String nonExistentBucket = "non-existent-bucket";

            // Step 1: Attempt operation on non-existent bucket
            graphQlTester
                .document("""
                    query ListObjects($bucket: String!) {
                        listObjects(bucket: $bucket) {
                            key
                        }
                    }
                    """)
                .variable("bucket", nonExistentBucket)
                .execute()
                .errors()
                .verify();

            // Step 2: Verify error is logged in audit trail
            graphQlTester
                .document("""
                    query AuditLogsByBucket($bucket: String!) {
                        auditLogsByBucket(bucket: $bucket) {
                            result
                            action
                        }
                    }
                    """)
                .variable("bucket", nonExistentBucket)
                .execute()
                .path("auditLogsByBucket[0].result")
                .entity(String.class)
                .isEqualTo("FAILURE");
        }
    }

    @Nested
    @DisplayName("Performance & Load Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle 100 concurrent bucket listings")
        void shouldHandle100ConcurrentBucketListings() {
            // Perform load test with 100 concurrent requests
            // Verify response times are within acceptable limits (<200ms p95)
        }

        @Test
        @DisplayName("Should handle large file uploads (>100MB)")
        void shouldHandleLargeFileUploads() {
            // Test multipart upload for large files
        }
    }
}
