package com.example.graphql.ironbucket;

import com.example.graphql.CrudFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GraphQL resolver for S3 bucket operations.
 * Maps GraphQL queries/mutations to IronBucketS3Service calls.
 */
@Controller
public class S3BucketResolver {

    private static final Logger log = LoggerFactory.getLogger(S3BucketResolver.class);

    private final IronBucketS3Service s3Service;
    private final CrudFeatures features;

    public S3BucketResolver(IronBucketS3Service s3Service, CrudFeatures features) {
        this.s3Service = s3Service;
        this.features = features;
    }

    @QueryMapping
    public Flux<S3Bucket> listBuckets(@Argument String jwtToken) {
        log.debug("GraphQL: listBuckets");
        return s3Service.listBuckets(jwtToken);
    }

    @QueryMapping
    public Mono<S3Bucket> getBucket(@Argument String jwtToken, @Argument String bucketName) {
        log.debug("GraphQL: getBucket - {}", bucketName);
        return s3Service.getBucket(jwtToken, bucketName);
    }

    @MutationMapping
    public Mono<S3Bucket> createBucket(
            @Argument String jwtToken,
            @Argument String bucketName,
            @Argument String ownerTenant
    ) {
        log.debug("GraphQL: createBucket - {}", bucketName);
        if (!features.isCreateEnabled()) {
            return Mono.error(new IllegalStateException("Create operation is disabled"));
        }
        return s3Service.createBucket(jwtToken, bucketName, ownerTenant);
    }

    @MutationMapping
    public Mono<Boolean> deleteBucket(@Argument String jwtToken, @Argument String bucketName) {
        log.debug("GraphQL: deleteBucket - {}", bucketName);
        if (!features.isDeleteEnabled()) {
            return Mono.error(new IllegalStateException("Delete operation is disabled"));
        }
        return s3Service.deleteBucket(jwtToken, bucketName);
    }
}
