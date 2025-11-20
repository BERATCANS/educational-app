package com.beratcan.app.exception;

public class TopicNotFoundException extends ResourceNotFoundException {
    public TopicNotFoundException(Long topicId) {
        super("Topic not found with id: " + topicId);
    }

    public TopicNotFoundException(String message) {
        super(message);
    }
}
