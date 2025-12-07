package com.beratcan.app.exception;

public class CartItemNotFoundException extends ResourceNotFoundException {
    public CartItemNotFoundException(Long cartItemId) {
        super("Cart item not found with id: " + cartItemId);
    }
}
