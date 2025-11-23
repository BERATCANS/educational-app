package com.beratcan.app.constants;

/**
 * Application-wide constants to avoid magic numbers and string literals
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // ========== Rating Constants ==========
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final double RATING_DECIMAL_MULTIPLIER = 10.0; // For Math.round(average * 10.0) / 10.0

    // ========== Pagination Constants ==========
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int REVIEW_PAGE_SIZE = 10; // Specific page size for reviews

    // ========== Phone Number Validation ==========
    public static final String PHONE_NUMBER_PATTERN = "^\\+?[0-9]{10,13}$";

    // ========== HTTP Timeout Constants (in seconds) ==========
    public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 60;
    public static final int HTTP_WRITE_TIMEOUT_SECONDS = 60;
    public static final int HTTP_READ_TIMEOUT_SECONDS = 60;

    // ========== CORS Constants ==========
    public static final long CORS_MAX_AGE_SECONDS = 3600L;

    // ========== JWT Constants ==========
    // Note: JWT expiration times are in milliseconds and configured in
    // application.properties
    // jwt.expiration=3600000 (1 hour)
    // jwt.refresh-expiration=604800000 (7 days)
    public static final long JWT_EXPIRATION_MS = 3600000L; // 1 hour
    public static final long JWT_REFRESH_EXPIRATION_MS = 604800000L; // 7 days
}
