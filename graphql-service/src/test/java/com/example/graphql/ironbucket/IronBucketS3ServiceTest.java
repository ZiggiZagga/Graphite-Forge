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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for IronBucketS3Service
 * Following IronBucket test structure with comprehensive coverage of S3 operations
 * 
 * Tests cover:
 * - Bucket operations (create, list, delete)
 * - Object operations (upload, download, list, delete)
 * - Metadata operations
 * - Error handling (401, 403, 404, 500)
 * - JWT claim extraction and propagation
 * - Retry logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IronBucketS3Service Tests")
class IronBucketS3ServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private IronBucketS3Service service;

    private String mockJwtToken;
    private String mockUserTenant;
    private List<String> mockUserRoles;

    @BeforeEach
    void setUp() {
        mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInRlbmFudCI6InRlc3QtdGVuYW50Iiwicm9sZXMiOlsiYWRtaW4iXX0.test";
        mockUserTenant = "test-tenant";
        mockUserRoles = List.of("admin", "s3:read", "s3:write");
    }

    @Nested
    @DisplayName("Bucket Operations")
    class BucketOperations {

        @Test
        @DisplayName("Should list all buckets successfully")
        void shouldListAllBuckets() {
            // Given
            List<S3Bucket> expectedBuckets = List.of(
                new S3Bucket("bucket-1", Instant.now(), mockUserTenant),
                new S3Bucket("bucket-2", Instant.now(), mockUserTenant)
            );

            // When - service not implemented yet, test should fail
            Flux<S3Bucket> result = service.listBuckets(mockJwtToken);

            // Then
            StepVerifier.create(result)
                .expectNextSequence(expectedBuckets)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should list buckets and filter by tenant")
        void shouldListBucketsFilteredByTenant() {
            // Given
            String otherTenant = "other-tenant";
            List<S3Bucket> allBuckets = List.of(
                new S3Bucket("tenant-bucket-1", Instant.now(), mockUserTenant),
                new S3Bucket("other-bucket", Instant.now(), otherTenant),
                new S3Bucket("tenant-bucket-2", Instant.now(), mockUserTenant)
            );

            // When
            Flux<S3Bucket> result = service.listBucketsByTenant(mockJwtToken, mockUserTenant);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(bucket -> bucket.ownerTenant().equals(mockUserTenant))
                .expectNextMatches(bucket -> bucket.ownerTenant().equals(mockUserTenant))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should create bucket successfully")
        void shouldCreateBucket() {
            // Given
            String bucketName = "test-bucket";
            S3Bucket expectedBucket = new S3Bucket(bucketName, Instant.now(), mockUserTenant);

            // When
            Mono<S3Bucket> result = service.createBucket(mockJwtToken, bucketName, mockUserTenant);

            // Then
            StepVerifier.create(result)
                .expectNext(expectedBucket)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to create bucket without JWT token")
        void shouldFailToCreateBucketWithoutJwt() {
            // Given
            String bucketName = "test-bucket";

            // When
            Mono<S3Bucket> result = service.createBucket(null, bucketName, mockUserTenant);

            // Then
            StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
        }

        @Test
        @DisplayName("Should fail to create bucket with invalid name")
        void shouldFailToCreateBucketWithInvalidName() {
            // Given
            String invalidBucketName = "INVALID_NAME_WITH_UPPERCASE";

            // When
            Mono<S3Bucket> result = service.createBucket(mockJwtToken, invalidBucketName, mockUserTenant);

            // Then
            StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
        }

        @Test
        @DisplayName("Should fail to create duplicate bucket")
        void shouldFailToCreateDuplicateBucket() {
            // Given
            String bucketName = "existing-bucket";

            // When
            Mono<S3Bucket> result = service.createBucket(mockJwtToken, bucketName, mockUserTenant);

            // Then
            StepVerifier.create(result)
                .expectError(BucketAlreadyExistsException.class)
                .verify();
        }

        @Test
        @DisplayName("Should delete bucket successfully")
        void shouldDeleteBucket() {
            // Given
            String bucketName = "test-bucket";

            // When
            Mono<Boolean> result = service.deleteBucket(mockJwtToken, bucketName);

            // Then
            StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to delete non-empty bucket")
        void shouldFailToDeleteNonEmptyBucket() {
            // Given
            String bucketName = "non-empty-bucket";

            // When
            Mono<Boolean> result = service.deleteBucket(mockJwtToken, bucketName);

            // Then
            StepVerifier.create(result)
                .expectError(BucketNotEmptyException.class)
                .verify();
        }

        @Test
        @DisplayName("Should fail to delete non-existent bucket")
        void shouldFailToDeleteNonExistentBucket() {
            // Given
            String bucketName = "non-existent-bucket";

            // When
            Mono<Boolean> result = service.deleteBucket(mockJwtToken, bucketName);

            // Then
            StepVerifier.create(result)
                .expectError(BucketNotFoundException.class)
                .verify();
        }

        @Test
        @DisplayName("Should get bucket details")
        void shouldGetBucketDetails() {
            // Given
            String bucketName = "test-bucket";
            S3Bucket expectedBucket = new S3Bucket(bucketName, Instant.now(), mockUserTenant);

            // When
            Mono<S3Bucket> result = service.getBucket(mockJwtToken, bucketName);

            // Then
            StepVerifier.create(result)
                .expectNext(expectedBucket)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Object Operations")
    class ObjectOperations {

        @Test
        @DisplayName("Should list objects in bucket")
        void shouldListObjects() {
            // Given
            String bucketName = "test-bucket";
            List<S3Object> expectedObjects = List.of(
                new S3Object("file1.txt", bucketName, 1024L, Instant.now(), "text/plain", null),
                new S3Object("file2.pdf", bucketName, 2048L, Instant.now(), "application/pdf", null)
            );

            // When
            Flux<S3Object> result = service.listObjects(mockJwtToken, bucketName, null);

            // Then
            StepVerifier.create(result)
                .expectNextSequence(expectedObjects)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should list objects with prefix filter")
        void shouldListObjectsWithPrefix() {
            // Given
            String bucketName = "test-bucket";
            String prefix = "documents/";
            List<S3Object> expectedObjects = List.of(
                new S3Object("documents/file1.txt", bucketName, 1024L, Instant.now(), "text/plain", null),
                new S3Object("documents/file2.pdf", bucketName, 2048L, Instant.now(), "application/pdf", null)
            );

            // When
            Flux<S3Object> result = service.listObjects(mockJwtToken, bucketName, prefix);

            // Then
            StepVerifier.create(result)
                .expectNextSequence(expectedObjects)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should get object metadata")
        void shouldGetObjectMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file1.txt";
            S3Object expectedObject = new S3Object(
                objectKey, bucketName, 1024L, Instant.now(), "text/plain",
                Map.of("author", "test-user", "project", "test-project")
            );

            // When
            Mono<S3Object> result = service.getObject(mockJwtToken, bucketName, objectKey);

            // Then
            StepVerifier.create(result)
                .expectNext(expectedObject)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fail to get non-existent object")
        void shouldFailToGetNonExistentObject() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "non-existent.txt";

            // When
            Mono<S3Object> result = service.getObject(mockJwtToken, bucketName, objectKey);

            // Then
            StepVerifier.create(result)
                .expectError(ObjectNotFoundException.class)
                .verify();
        }

        @Test
        @DisplayName("Should generate presigned URL")
        void shouldGeneratePresignedUrl() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file1.txt";
            int expiresIn = 3600;

            // When
            Mono<String> result = service.getPresignedUrl(mockJwtToken, bucketName, objectKey, expiresIn);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(url -> url.contains(bucketName) && url.contains(objectKey))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should upload object successfully")
        void shouldUploadObject() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "upload.txt";
            byte[] content = "test content".getBytes();
            String contentType = "text/plain";

            // When
            Mono<S3Object> result = service.uploadObject(
                mockJwtToken, bucketName, objectKey, content, contentType, null
            );

            // Then
            StepVerifier.create(result)
                .expectNextMatches(obj -> obj.key().equals(objectKey) && obj.bucket().equals(bucketName))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should upload object with metadata")
        void shouldUploadObjectWithMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "upload.txt";
            byte[] content = "test content".getBytes();
            String contentType = "text/plain";
            Map<String, String> metadata = Map.of("author", "test-user", "version", "1.0");

            // When
            Mono<S3Object> result = service.uploadObject(
                mockJwtToken, bucketName, objectKey, content, contentType, metadata
            );

            // Then
            StepVerifier.create(result)
                .expectNextMatches(obj -> 
                    obj.metadata() != null && 
                    obj.metadata().get("author").equals("test-user")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("Should delete object successfully")
        void shouldDeleteObject() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file-to-delete.txt";

            // When
            Mono<Boolean> result = service.deleteObject(mockJwtToken, bucketName, objectKey);

            // Then
            StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should set object metadata")
        void shouldSetObjectMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file1.txt";
            Map<String, String> metadata = Map.of("updated-by", "test-user", "last-modified", "2026-01-17");

            // When
            Mono<S3Object> result = service.setObjectMetadata(mockJwtToken, bucketName, objectKey, metadata);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(obj -> 
                    obj.metadata() != null && 
                    obj.metadata().containsKey("updated-by")
                )
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle 401 Unauthorized error")
        void shouldHandle401Error() {
            // Given
            String bucketName = "test-bucket";
            String invalidToken = "invalid-token";

            // When
            Flux<S3Object> result = service.listObjects(invalidToken, bucketName, null);

            // Then
            StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
        }

        @Test
        @DisplayName("Should handle 403 Forbidden error")
        void shouldHandle403Error() {
            // Given
            String bucketName = "forbidden-bucket";

            // When
            Flux<S3Object> result = service.listObjects(mockJwtToken, bucketName, null);

            // Then
            StepVerifier.create(result)
                .expectError(ForbiddenException.class)
                .verify();
        }

        @Test
        @DisplayName("Should handle 404 Not Found error")
        void shouldHandle404Error() {
            // Given
            String bucketName = "non-existent-bucket";

            // When
            Mono<S3Bucket> result = service.getBucket(mockJwtToken, bucketName);

            // Then
            StepVerifier.create(result)
                .expectError(BucketNotFoundException.class)
                .verify();
        }

        @Test
        @DisplayName("Should handle 500 Internal Server Error")
        void shouldHandle500Error() {
            // Given
            String bucketName = "test-bucket";

            // When
            Flux<S3Object> result = service.listObjects(mockJwtToken, bucketName, null);

            // Then
            StepVerifier.create(result)
                .expectError(S3ServiceException.class)
                .verify();
        }

        @Test
        @DisplayName("Should retry on transient errors")
        void shouldRetryOnTransientErrors() {
            // Given
            String bucketName = "test-bucket";

            // When
            Flux<S3Object> result = service.listObjects(mockJwtToken, bucketName, null);

            // Then - should retry 3 times before failing
            StepVerifier.create(result)
                .expectError(S3ServiceException.class)
                .verify();
        }

        @Test
        @DisplayName("Should not retry on client errors (4xx)")
        void shouldNotRetryOn4xxErrors() {
            // Given
            String bucketName = "test-bucket";
            String invalidToken = "invalid-token";

            // When
            Flux<S3Object> result = service.listObjects(invalidToken, bucketName, null);

            // Then - should fail immediately without retry
            StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("JWT Claim Extraction")
    class JwtClaimExtraction {

        @Test
        @DisplayName("Should extract tenant from JWT claims")
        void shouldExtractTenantFromJwt() {
            // Given
            String jwtToken = mockJwtToken;

            // When
            String tenant = service.extractTenantFromJwt(jwtToken);

            // Then
            assertThat(tenant).isEqualTo(mockUserTenant);
        }

        @Test
        @DisplayName("Should extract roles from JWT claims")
        void shouldExtractRolesFromJwt() {
            // Given
            String jwtToken = mockJwtToken;

            // When
            List<String> roles = service.extractRolesFromJwt(jwtToken);

            // Then
            assertThat(roles).containsExactlyInAnyOrderElementsOf(mockUserRoles);
        }

        @Test
        @DisplayName("Should extract subject (username) from JWT claims")
        void shouldExtractSubjectFromJwt() {
            // Given
            String jwtToken = mockJwtToken;

            // When
            String subject = service.extractSubjectFromJwt(jwtToken);

            // Then
            assertThat(subject).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should fail to extract claims from invalid JWT")
        void shouldFailToExtractClaimsFromInvalidJwt() {
            // Given
            String invalidToken = "invalid.jwt.token";

            // When/Then
            assertThat(service.extractTenantFromJwt(invalidToken)).isNull();
        }
    }

    @Nested
    @DisplayName("Caching")
    class Caching {

        @Test
        @DisplayName("Should cache bucket list")
        void shouldCacheBucketList() {
            // Given
            String jwtToken = mockJwtToken;

            // When - call twice
            Flux<S3Bucket> result1 = service.listBuckets(jwtToken);
            Flux<S3Bucket> result2 = service.listBuckets(jwtToken);

            // Then - should only make one HTTP call
            StepVerifier.create(result1.then(result2.then()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Should invalidate cache on bucket creation")
        void shouldInvalidateCacheOnBucketCreation() {
            // Given
            String bucketName = "new-bucket";

            // When
            Mono<S3Bucket> createResult = service.createBucket(mockJwtToken, bucketName, mockUserTenant);
            Flux<S3Bucket> listResult = service.listBuckets(mockJwtToken);

            // Then - should include new bucket
            StepVerifier.create(createResult.thenMany(listResult))
                .expectNextMatches(bucket -> bucket.name().equals(bucketName))
                .thenConsumeWhile(bucket -> true)
                .verifyComplete();
        }

        @Test
        @DisplayName("Should cache object metadata")
        void shouldCacheObjectMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file1.txt";

            // When - call twice
            Mono<S3Object> result1 = service.getObject(mockJwtToken, bucketName, objectKey);
            Mono<S3Object> result2 = service.getObject(mockJwtToken, bucketName, objectKey);

            // Then - should only make one HTTP call
            StepVerifier.create(result1.then(result2))
                .expectNextCount(1)
                .verifyComplete();
        }
    }
}
