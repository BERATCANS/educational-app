package com.beratcan.app.exception;

/**
 * Thrown when two concurrent checkout requests with the same idempotency key
 * race past the pre-existence check and the second INSERT trips the partial
 * unique index uk_purchases_student_idempotency. The handler refetches the
 * winning Purchase and returns its response.
 */
public class IdempotencyConflictException extends RuntimeException {

    private final Long userId;
    private final String idempotencyKey;

    public IdempotencyConflictException(Long userId, String idempotencyKey) {
        super("Concurrent checkout with same idempotency key: userId=" + userId
                + ", key=" + idempotencyKey);
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
