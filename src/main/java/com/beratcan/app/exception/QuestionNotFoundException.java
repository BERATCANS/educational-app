package com.beratcan.app.exception;

public class QuestionNotFoundException extends ResourceNotFoundException {
    public QuestionNotFoundException(Long questionId) {
        super("Question not found with id: " + questionId);
    }

    public QuestionNotFoundException(String message) {
        super(message);
    }
}
