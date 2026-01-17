package com.example.graphql.ironbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * GraphQL resolver for S3 object operations.
 * Maps GraphQL queries/mutations to IronBucketS3Service calls.
 */
@Controller
public class S3ObjectResolver {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectResolver.class);

    private final IronBucketS3Service s3Service;

    public S3ObjectResolver(IronBucketS3Service s3Service) {
        this.s3Service = s3Service;
    }

    @QueryMapping
    public Flux<S3Object> listObjects(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String prefix
    ) {
        log.debug("GraphQL: listObjects - bucket={}, prefix={}", bucketName, prefix);
        return s3Service.listObjects(jwtToken, bucketName, prefix);
    }

    @QueryMapping
    public Mono<S3Object> getObject(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String objectKey
    ) {
        log.debug("GraphQL: getObject - {}/{}", bucketName, objectKey);
        return s3Service.getObject(jwtToken, bucketName, objectKey);
    }

    @MutationMapping
    public Mono<Boolean> deleteObject(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String objectKey
    ) {
        log.debug("GraphQL: deleteObject - {}/{}", bucketName, objectKey);
        return s3Service.deleteObject(jwtToken, bucketName, objectKey);
    }

    @QueryMapping
    public Mono<String> getPresignedUrl(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String objectKey,
            @Argument int expiresIn
    ) {
        log.debug("GraphQL: getPresignedUrl - {}/{}", bucketName, objectKey);
        return s3Service.getPresignedUrl(jwtToken, bucketName, objectKey, expiresIn);
    }

    @MutationMapping
    public Mono<S3Object> uploadObject(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String objectKey,
            @Argument byte[] content,
            @Argument String contentType,
            @Argument(name = "metadata") Map<String, String> metadata
    ) {
        log.debug("GraphQL: uploadObject - {}/{}", bucketName, objectKey);
        return s3Service.uploadObject(jwtToken, bucketName, objectKey, content, contentType, metadata);
    }

    @MutationMapping
    public Mono<S3Object> setObjectMetadata(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String objectKey,
            @Argument Map<String, String> metadata
    ) {
        log.debug("GraphQL: setObjectMetadata - {}/{}", bucketName, objectKey);
        return s3Service.setObjectMetadata(jwtToken, bucketName, objectKey, metadata);
    }
}
