package com.beratcan.app.exception;

public class TestResultNotFoundException extends ResourceNotFoundException {
    public TestResultNotFoundException(Long testResultId) {
        super("Test result not found with id: " + testResultId);
    }

    public TestResultNotFoundException(String message) {
        super(message);
    }
}
