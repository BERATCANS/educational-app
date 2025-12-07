package com.beratcan.app.exception;

public class ReviewNotFoundException extends ResourceNotFoundException {
    public ReviewNotFoundException(Long reviewId) {
        super("Review not found with id: " + reviewId);
    }

    public ReviewNotFoundException(String message) {
        super(message);
    }
}
