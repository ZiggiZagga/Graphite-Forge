package com.example.graphql;

import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.stereotype.Component;
import graphql.GraphQLError;
import graphql.schema.DataFetcher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Global exception resolver for GraphQL operations.
 *
 * <p>Converts application exceptions into standardized GraphQL errors with
 * appropriate error codes and messages. Leverages Java 21+ pattern matching.</p>
 */
@Component
public class GlobalGraphQLExceptionResolver implements DataFetcherExceptionResolver {

                @Override
                public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment env) {
                        GraphQLError error = switch (exception) {
                        case ItemNotFoundException ex -> createNotFoundError(ex, env);
                        case ItemOperationDisabledException ex -> createOperationDisabledError(ex, env);
                        case ItemDatabaseException ex -> createDatabaseError(ex, env);
                        case IllegalArgumentException ex -> createInvalidArgumentError(ex, env);
                        case null, default -> createInternalError(exception, env);
                };
                        return Mono.just(List.of(error));
        }

    private GraphQLError createNotFoundError(ItemNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of(
                        "code", "NOT_FOUND",
                        "itemId", ex.getItemId(),
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
    }

    private GraphQLError createOperationDisabledError(ItemOperationDisabledException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.FORBIDDEN)
                .extensions(Map.of(
                        "code", "OPERATION_DISABLED",
                        "operation", ex.getOperation(),
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
    }

    private GraphQLError createDatabaseError(ItemDatabaseException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Database operation failed")
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of(
                        "code", "DATABASE_ERROR",
                        "details", ex.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
    }

    private GraphQLError createInvalidArgumentError(IllegalArgumentException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Invalid argument: " + ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(
                        "code", "INVALID_ARGUMENT",
                        "details", ex.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
    }

    private GraphQLError createInternalError(Throwable exception, DataFetchingEnvironment env) {
        String message = exception != null ? exception.getMessage() : "Unknown error";
        return GraphqlErrorBuilder.newError()
                .message("An unexpected error occurred")
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of(
                        "code", "INTERNAL_ERROR",
                        "details", message,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
    }
}
