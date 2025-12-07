package com.beratcan.app.exception;

public class BookNotFoundException extends ResourceNotFoundException {
    public BookNotFoundException(Long bookId) {
        super("Book not found with id: " + bookId);
    }

    public BookNotFoundException(String message) {
        super(message);
    }
}
