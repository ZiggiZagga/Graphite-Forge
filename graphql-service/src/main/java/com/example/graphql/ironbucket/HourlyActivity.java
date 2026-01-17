package com.example.graphql.ironbucket;

/**
 * Activity count by hour.
 */
public record HourlyActivity(
    int hour,
    long operationCount
) {
    public HourlyActivity {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }
    }
}
