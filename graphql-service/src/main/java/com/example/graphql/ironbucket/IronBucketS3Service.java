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
 * Service for interacting with IronBucket S3-compatible storage through Sentinel-Gear.
 * All operations flow through the identity gateway with JWT-based authentication.
 * 
 * Architecture:
 * Client → GraphQL Service → Sentinel-Gear → Claimspindel → S3 Proxy → MinIO
 * 
 * Security: All requests require valid JWT tokens. No direct storage access allowed.
 */
@Service
public class IronBucketS3Service {

    private static final Logger log = LoggerFactory.getLogger(IronBucketS3Service.class);

    private final WebClient webClient;
    private final String sentinelGearBaseUrl;

    public IronBucketS3Service(
            WebClient.Builder webClientBuilder,
            @Value("${ironbucket.sentinel-gear.base-url:http://localhost:8080}") String sentinelGearBaseUrl
    ) {
        this.sentinelGearBaseUrl = sentinelGearBaseUrl;
        this.webClient = webClientBuilder
                .baseUrl(sentinelGearBaseUrl)
                .build();
        log.info("IronBucketS3Service initialized with Sentinel-Gear at {}", sentinelGearBaseUrl);
    }

    /**
     * List all buckets accessible to the authenticated user.
     * 
     * @param jwtToken JWT bearer token for authentication
     * @return Flux of buckets
     */
    public Flux<S3Bucket> listBuckets(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.get()
                .uri("/api/v1/buckets")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(S3BucketDto.class)
                .map(this::toBucket)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to list buckets", e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * List buckets filtered by tenant.
     * 
     * @param jwtToken JWT bearer token
     * @param tenantId Tenant ID to filter by
     * @return Flux of buckets belonging to the tenant
     */
    public Flux<S3Bucket> listBucketsByTenant(String jwtToken, String tenantId) {
        validateJwt(jwtToken);
        validateTenant(tenantId);
        
        return listBuckets(jwtToken)
                .filter(bucket -> tenantId.equals(bucket.ownerTenant()));
    }

    /**
     * Create a new bucket.
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Name of the bucket (must be DNS-compatible)
     * @param ownerTenant Tenant ID that will own this bucket
     * @return Mono of created bucket
     */
    public Mono<S3Bucket> createBucket(String jwtToken, String bucketName, String ownerTenant) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateTenant(ownerTenant);
        
        S3BucketCreateRequest request = new S3BucketCreateRequest(bucketName, ownerTenant);
        
        return webClient.post()
                .uri("/api/v1/buckets")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(S3BucketDto.class)
                .map(this::toBucket)
                .retryWhen(retrySpec())
                .doOnSuccess(bucket -> log.info("Created bucket: {}", bucketName))
                .doOnError(e -> log.error("Failed to create bucket: {}", bucketName, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        return Mono.error(new BucketAlreadyExistsException(bucketName, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Delete a bucket (must be empty).
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Name of bucket to delete
     * @return Mono indicating success
     */
    public Mono<Boolean> deleteBucket(String jwtToken, String bucketName) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        
        return webClient.delete()
                .uri("/api/v1/buckets/{bucketName}", bucketName)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .retryWhen(retrySpec())
                .doOnSuccess(result -> log.info("Deleted bucket: {}", bucketName))
                .doOnError(e -> log.error("Failed to delete bucket: {}", bucketName, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        return Mono.error(new BucketNotEmptyException(bucketName, e));
                    }
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new BucketNotFoundException(bucketName, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Get bucket details.
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Bucket name
     * @return Mono of bucket details
     */
    public Mono<S3Bucket> getBucket(String jwtToken, String bucketName) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        
        return webClient.get()
                .uri("/api/v1/buckets/{bucketName}", bucketName)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(S3BucketDto.class)
                .map(this::toBucket)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get bucket: {}", bucketName, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new BucketNotFoundException(bucketName, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * List objects in a bucket.
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Bucket name
     * @param prefix Optional prefix filter
     * @return Flux of objects
     */
    public Flux<S3Object> listObjects(String jwtToken, String bucketName, String prefix) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        
        String uri = prefix != null && !prefix.isBlank()
                ? "/api/v1/buckets/{bucketName}/objects?prefix={prefix}"
                : "/api/v1/buckets/{bucketName}/objects";
        
        return webClient.get()
                .uri(uri, bucketName, prefix != null ? prefix : "")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(S3ObjectDto.class)
                .map(this::toObject)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to list objects in bucket: {}", bucketName, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get object metadata.
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Bucket name
     * @param objectKey Object key
     * @return Mono of object metadata
     */
    public Mono<S3Object> getObject(String jwtToken, String bucketName, String objectKey) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateObjectKey(objectKey);
        
        return webClient.get()
                .uri("/api/v1/buckets/{bucketName}/objects/{objectKey}", bucketName, objectKey)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(S3ObjectDto.class)
                .map(this::toObject)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get object: {}/{}", bucketName, objectKey, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new ObjectNotFoundException(bucketName, objectKey, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Delete an object.
     * 
     * @param jwtToken JWT bearer token
     * @param bucketName Bucket name
     * @param objectKey Object key
     * @return Mono indicating success
     */
    public Mono<Boolean> deleteObject(String jwtToken, String bucketName, String objectKey) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateObjectKey(objectKey);
        
        return webClient.delete()
                .uri("/api/v1/buckets/{bucketName}/objects/{objectKey}", bucketName, objectKey)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .retryWhen(retrySpec())
                .doOnSuccess(result -> log.info("Deleted object: {}/{}", bucketName, objectKey))
                .doOnError(e -> log.error("Failed to delete object: {}/{}", bucketName, objectKey, e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new ObjectNotFoundException(bucketName, objectKey, e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Upload an object to a bucket.
     */
    public Mono<S3Object> uploadObject(String jwtToken, String bucketName, String objectKey,
                                        byte[] data, String contentType, Map<String, String> metadata) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateObjectKey(objectKey);
        
        return webClient.put()
                .uri("/api/v1/buckets/{bucketName}/objects/{objectKey}", bucketName, objectKey)
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                .bodyValue(new UploadRequest(data, metadata))
                .retrieve()
                .bodyToMono(S3ObjectDto.class)
                .map(this::toObject)
                .retryWhen(retrySpec())
                .doOnSuccess(obj -> log.info("Uploaded object: {}/{}", bucketName, objectKey))
                .doOnError(e -> log.error("Failed to upload object: {}/{}", bucketName, objectKey, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Set metadata on an existing object.
     */
    public Mono<S3Object> setObjectMetadata(String jwtToken, String bucketName, String objectKey,
                                             Map<String, String> metadata) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateObjectKey(objectKey);
        
        return webClient.patch()
                .uri("/api/v1/buckets/{bucketName}/objects/{objectKey}/metadata", bucketName, objectKey)
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(metadata)
                .retrieve()
                .bodyToMono(S3ObjectDto.class)
                .map(this::toObject)
                .retryWhen(retrySpec())
                .doOnSuccess(obj -> log.info("Updated metadata for: {}/{}", bucketName, objectKey))
                .doOnError(e -> log.error("Failed to update metadata: {}/{}", bucketName, objectKey, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Get a presigned URL for temporary access to an object.
     */
    public Mono<String> getPresignedUrl(String jwtToken, String bucketName, String objectKey, int expirationSeconds) {
        validateJwt(jwtToken);
        validateBucketName(bucketName);
        validateObjectKey(objectKey);
        
        return webClient.post()
                .uri("/api/v1/buckets/{bucketName}/objects/{objectKey}/presign?expiration={exp}",
                     bucketName, objectKey, expirationSeconds)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to get presigned URL: {}/{}", bucketName, objectKey, e))
                .onErrorResume(WebClientResponseException.class, this::handleWebClientError);
    }

    /**
     * Extract tenant ID from JWT token.
     */
    public Mono<String> extractTenantFromJwt(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/auth/extract/tenant")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to extract tenant from JWT", e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new UnauthorizedException("Invalid or expired JWT", e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Extract roles from JWT token.
     */
    public Mono<List<String>> extractRolesFromJwt(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/auth/extract/roles")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to extract roles from JWT", e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new UnauthorizedException("Invalid or expired JWT", e));
                    }
                    return handleWebClientError(e);
                });
    }

    /**
     * Extract subject (user ID) from JWT token.
     */
    public Mono<String> extractSubjectFromJwt(String jwtToken) {
        validateJwt(jwtToken);
        
        return webClient.post()
                .uri("/api/v1/auth/extract/subject")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(retrySpec())
                .doOnError(e -> log.error("Failed to extract subject from JWT", e))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new UnauthorizedException("Invalid or expired JWT", e));
                    }
                    return handleWebClientError(e);
                });
    }

    // --- Validation ---

    private void validateJwt(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new IllegalArgumentException("JWT token is required");
        }
    }

    private void validateBucketName(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
    }

    private void validateTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be null or blank");
        }
    }

    // --- Mapping ---

    private S3Bucket toBucket(S3BucketDto dto) {
        return new S3Bucket(dto.name(), dto.creationDate(), dto.ownerTenant());
    }

    private S3Object toObject(S3ObjectDto dto) {
        return new S3Object(
                dto.key(),
                dto.bucketName(),
                dto.size(),
                dto.lastModified(),
                dto.contentType(),
                dto.metadata()
        );
    }

    // --- Error Handling ---

    private <T> Mono<T> handleWebClientError(WebClientResponseException e) {
        log.error("WebClient error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        return Mono.error(new IronBucketServiceException("IronBucket service error: " + e.getMessage(), e));
    }

    private Retry retrySpec() {
        return Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable
                        || throwable instanceof WebClientResponseException.GatewayTimeout)
                .doBeforeRetry(signal -> log.warn("Retrying request, attempt: {}", signal.totalRetries() + 1));
    }

    // --- DTOs for wire format ---

    record S3BucketDto(String name, Instant creationDate, String ownerTenant) {}
    record S3BucketCreateRequest(String name, String ownerTenant) {}
    record S3ObjectDto(String key, String bucketName, Long size, Instant lastModified, 
                       String contentType, Map<String, String> metadata) {}
    record UploadRequest(byte[] data, Map<String, String> metadata) {}
}
