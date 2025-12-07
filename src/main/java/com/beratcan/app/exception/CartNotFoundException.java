package com.beratcan.app.exception;

public class CartNotFoundException extends ResourceNotFoundException {
    public CartNotFoundException(Long userId) {
        super("Cart not found for user id: " + userId);
    }

    public CartNotFoundException(String message) {
        super(message);
    }
}
