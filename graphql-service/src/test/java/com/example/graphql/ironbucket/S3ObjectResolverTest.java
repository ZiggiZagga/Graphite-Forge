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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for S3ObjectResolver
 * Tests GraphQL queries and mutations for S3 object operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3ObjectResolver Tests")
class S3ObjectResolverTest {

    @Mock
    private IronBucketS3Service s3Service;

    @InjectMocks
    private S3ObjectResolver resolver;

    private String mockJwtToken;

    @BeforeEach
    void setUp() {
        mockJwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
    }

    @Nested
    @DisplayName("Query: listObjects")
    class ListObjects {

        @Test
        @DisplayName("Should list all objects in bucket")
        void shouldListAllObjectsInBucket() {
            // Given
            String bucketName = "test-bucket";
            when(s3Service.listObjects(anyString(), eq(bucketName), isNull()))
                .thenReturn(Flux.just(
                    new S3Object("file1.txt", bucketName, 1024L, Instant.now(), "text/plain", null),
                    new S3Object("file2.pdf", bucketName, 2048L, Instant.now(), "application/pdf", null)
                ));

            // When
            Flux<S3Object> result = resolver.listObjects(mockJwtToken, bucketName, null);

            // Then
            result.collectList().block();
            verify(s3Service).listObjects(mockJwtToken, bucketName, null);
        }

        @Test
        @DisplayName("Should list objects with prefix filter")
        void shouldListObjectsWithPrefix() {
            // Given
            String bucketName = "test-bucket";
            String prefix = "documents/";
            when(s3Service.listObjects(anyString(), eq(bucketName), eq(prefix)))
                .thenReturn(Flux.just(
                    new S3Object("documents/file1.txt", bucketName, 1024L, Instant.now(), "text/plain", null)
                ));

            // When
            Flux<S3Object> result = resolver.listObjects(mockJwtToken, bucketName, prefix);

            // Then
            result.collectList().block();
            verify(s3Service).listObjects(mockJwtToken, bucketName, prefix);
        }
    }

    @Nested
    @DisplayName("Query: getObject")
    class GetObject {

        @Test
        @DisplayName("Should get object metadata")
        void shouldGetObjectMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file.txt";
            S3Object mockObject = new S3Object(objectKey, bucketName, 1024L, Instant.now(), "text/plain", null);
            when(s3Service.getObject(anyString(), eq(bucketName), eq(objectKey)))
                .thenReturn(Mono.just(mockObject));

            // When
            Mono<S3Object> result = resolver.getObject(mockJwtToken, bucketName, objectKey);

            // Then
            result.block();
            verify(s3Service).getObject(mockJwtToken, bucketName, objectKey);
        }
    }

    @Nested
    @DisplayName("Query: getPresignedUrl")
    class GetPresignedUrl {

        @Test
        @DisplayName("Should generate presigned URL")
        void shouldGeneratePresignedUrl() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file.txt";
            int expiresIn = 3600;
            String mockUrl = "https://s3.example.com/test-bucket/file.txt?signature=abc123";
            when(s3Service.getPresignedUrl(anyString(), eq(bucketName), eq(objectKey), eq(expiresIn)))
                .thenReturn(Mono.just(mockUrl));

            // When
            Mono<String> result = resolver.getPresignedUrl(mockJwtToken, bucketName, objectKey, expiresIn);

            // Then
            result.block();
            verify(s3Service).getPresignedUrl(mockJwtToken, bucketName, objectKey, expiresIn);
        }
    }

    @Nested
    @DisplayName("Mutation: uploadObject")
    class UploadObject {

        @Test
        @DisplayName("Should upload object successfully")
        void shouldUploadObject() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "upload.txt";
            byte[] content = "test content".getBytes();
            String contentType = "text/plain";
            S3Object mockObject = new S3Object(objectKey, bucketName, content.length, Instant.now(), contentType, null);
            when(s3Service.uploadObject(anyString(), eq(bucketName), eq(objectKey), any(), eq(contentType), isNull()))
                .thenReturn(Mono.just(mockObject));

            // When
            Mono<S3Object> result = resolver.uploadObject(mockJwtToken, bucketName, objectKey, content, contentType, null);

            // Then
            result.block();
            verify(s3Service).uploadObject(mockJwtToken, bucketName, objectKey, content, contentType, null);
        }

        @Test
        @DisplayName("Should upload object with metadata")
        void shouldUploadObjectWithMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "upload.txt";
            byte[] content = "test content".getBytes();
            String contentType = "text/plain";
            Map<String, String> metadata = Map.of("author", "testuser");
            S3Object mockObject = new S3Object(objectKey, bucketName, content.length, Instant.now(), contentType, metadata);
            when(s3Service.uploadObject(anyString(), eq(bucketName), eq(objectKey), any(), eq(contentType), eq(metadata)))
                .thenReturn(Mono.just(mockObject));

            // When
            Mono<S3Object> result = resolver.uploadObject(mockJwtToken, bucketName, objectKey, content, contentType, metadata);

            // Then
            result.block();
            verify(s3Service).uploadObject(mockJwtToken, bucketName, objectKey, content, contentType, metadata);
        }
    }

    @Nested
    @DisplayName("Mutation: deleteObject")
    class DeleteObject {

        @Test
        @DisplayName("Should delete object successfully")
        void shouldDeleteObject() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file-to-delete.txt";
            when(s3Service.deleteObject(anyString(), eq(bucketName), eq(objectKey)))
                .thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = resolver.deleteObject(mockJwtToken, bucketName, objectKey);

            // Then
            result.block();
            verify(s3Service).deleteObject(mockJwtToken, bucketName, objectKey);
        }
    }

    @Nested
    @DisplayName("Mutation: setObjectMetadata")
    class SetObjectMetadata {

        @Test
        @DisplayName("Should set object metadata")
        void shouldSetObjectMetadata() {
            // Given
            String bucketName = "test-bucket";
            String objectKey = "file.txt";
            Map<String, String> metadata = Map.of("updated-by", "testuser");
            S3Object mockObject = new S3Object(objectKey, bucketName, 1024L, Instant.now(), "text/plain", metadata);
            when(s3Service.setObjectMetadata(anyString(), eq(bucketName), eq(objectKey), eq(metadata)))
                .thenReturn(Mono.just(mockObject));

            // When
            Mono<S3Object> result = resolver.setObjectMetadata(mockJwtToken, bucketName, objectKey, metadata);

            // Then
            result.block();
            verify(s3Service).setObjectMetadata(mockJwtToken, bucketName, objectKey, metadata);
        }
    }
}
