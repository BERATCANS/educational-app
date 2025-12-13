package com.beratcan.app.exception;

public class BookCodeNotFoundException extends ResourceNotFoundException {
    public BookCodeNotFoundException(Long codeId) {
        super("Book code not found with id: " + codeId);
    }

    public BookCodeNotFoundException(String message) {
        super(message);
    }
}
