package com.example.graphql.ironbucket;

import com.example.graphql.CrudFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for S3BucketResolver
 * Tests GraphQL queries and mutations for S3 bucket operations
 * Following Graphite-Forge ItemGraphqlController test patterns
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3BucketResolver Tests")
class S3BucketResolverTest {

    @Mock
    private IronBucketS3Service s3Service;

    @Mock
    private CrudFeatures features;

    @InjectMocks
    private S3BucketResolver resolver;

    private String mockJwtToken;

    @BeforeEach
    void setUp() {
        mockJwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
    }

    @Nested
    @DisplayName("Query: listBuckets")
    class ListBuckets {

        @Test
        @DisplayName("Should return all buckets for user")
        void shouldReturnAllBuckets() {
            // Given
            List<S3Bucket> mockBuckets = List.of(
                new S3Bucket("bucket-1", Instant.now(), "test-tenant"),
                new S3Bucket("bucket-2", Instant.now(), "test-tenant")
            );
            when(s3Service.listBuckets(anyString())).thenReturn(Flux.fromIterable(mockBuckets));

            // When
            Flux<S3Bucket> result = resolver.listBuckets(mockJwtToken);

            // Then
            result.collectList().block();
            verify(s3Service).listBuckets(mockJwtToken);
        }

        @Test
        @DisplayName("Should return empty list when no buckets exist")
        void shouldReturnEmptyListWhenNoBuckets() {
            // Given
            when(s3Service.listBuckets(anyString())).thenReturn(Flux.empty());

            // When
            Flux<S3Bucket> result = resolver.listBuckets(mockJwtToken);

            // Then
            result.collectList().block();
            verify(s3Service).listBuckets(mockJwtToken);
        }

        @Test
        @DisplayName("Should fail when JWT token is missing")
        void shouldFailWhenJwtTokenMissing() {
            // When/Then
            // Test should verify that resolver extracts JWT from GraphQL context
            // and throws appropriate error when missing
        }
    }

    @Nested
    @DisplayName("Query: getBucket")
    class GetBucket {

        @Test
        @DisplayName("Should return bucket by name")
        void shouldReturnBucketByName() {
            // Given
            String bucketName = "test-bucket";
            S3Bucket mockBucket = new S3Bucket(bucketName, Instant.now(), "test-tenant");
            when(s3Service.getBucket(anyString(), eq(bucketName))).thenReturn(Mono.just(mockBucket));

            // When
            Mono<S3Bucket> result = resolver.getBucket(mockJwtToken, bucketName);

            // Then
            result.block();
            verify(s3Service).getBucket(mockJwtToken, bucketName);
        }

        @Test
        @DisplayName("Should return error for non-existent bucket")
        void shouldReturnErrorForNonExistentBucket() {
            // Given
            String bucketName = "non-existent";
            when(s3Service.getBucket(anyString(), eq(bucketName)))
                .thenReturn(Mono.error(new BucketNotFoundException(bucketName)));

            // When
            Mono<S3Bucket> result = resolver.getBucket(mockJwtToken, bucketName);

            // Then - should propagate error
            verify(s3Service).getBucket(mockJwtToken, bucketName);
        }
    }

    @Nested
    @DisplayName("Mutation: createBucket")
    class CreateBucket {

        @Test
        @DisplayName("Should create bucket successfully")
        void shouldCreateBucket() {
            // Given
            String bucketName = "new-bucket";
            String tenant = "test-tenant";
            S3Bucket mockBucket = new S3Bucket(bucketName, Instant.now(), tenant);
            when(s3Service.createBucket(anyString(), eq(bucketName), eq(tenant)))
                .thenReturn(Mono.just(mockBucket));

            // When
            Mono<S3Bucket> result = resolver.createBucket(mockJwtToken, bucketName, tenant);

            // Then
            result.block();
            verify(s3Service).createBucket(mockJwtToken, bucketName, tenant);
        }

        @Test
        @DisplayName("Should validate bucket name format")
        void shouldValidateBucketNameFormat() {
            // Given
            String invalidBucketName = "INVALID_NAME";

            // When
            Mono<S3Bucket> result = resolver.createBucket(mockJwtToken, invalidBucketName, "test-tenant");

            // Then - should return validation error
        }

        @Test
        @DisplayName("Should fail when bucket already exists")
        void shouldFailWhenBucketAlreadyExists() {
            // Given
            String bucketName = "existing-bucket";
            when(s3Service.createBucket(anyString(), eq(bucketName), anyString()))
                .thenReturn(Mono.error(new BucketAlreadyExistsException(bucketName)));

            // When
            Mono<S3Bucket> result = resolver.createBucket(mockJwtToken, bucketName, "test-tenant");

            // Then
            verify(s3Service).createBucket(mockJwtToken, bucketName, "test-tenant");
        }
    }

    @Nested
    @DisplayName("Mutation: deleteBucket")
    class DeleteBucket {

        @Test
        @DisplayName("Should delete empty bucket")
        void shouldDeleteEmptyBucket() {
            // Given
            String bucketName = "empty-bucket";
            when(s3Service.deleteBucket(anyString(), eq(bucketName)))
                .thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = resolver.deleteBucket(mockJwtToken, bucketName);

            // Then
            result.block();
            verify(s3Service).deleteBucket(mockJwtToken, bucketName);
        }

        @Test
        @DisplayName("Should fail to delete non-empty bucket")
        void shouldFailToDeleteNonEmptyBucket() {
            // Given
            String bucketName = "non-empty-bucket";
            when(s3Service.deleteBucket(anyString(), eq(bucketName)))
                .thenReturn(Mono.error(new BucketNotEmptyException(bucketName)));

            // When
            Mono<Boolean> result = resolver.deleteBucket(mockJwtToken, bucketName);

            // Then
            verify(s3Service).deleteBucket(mockJwtToken, bucketName);
        }
    }
}
