package com.beratcan.app.exception;

public class TestNotFoundException extends ResourceNotFoundException {
    public TestNotFoundException(Long testId) {
        super("Test not found with id: " + testId);
    }

    public TestNotFoundException(String message) {
        super(message);
    }
}
