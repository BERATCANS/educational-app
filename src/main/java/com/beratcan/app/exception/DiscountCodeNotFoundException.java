package com.beratcan.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DiscountCodeNotFoundException extends RuntimeException {
    public DiscountCodeNotFoundException(Long id) {
        super("Discount code not found with ID: " + id);
    }

    public DiscountCodeNotFoundException(String code) {
        super("Discount code not found with code: " + code);
    }
}
