package com.beratcan.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Audio işleme hataları için özel exception
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AudioProcessingException extends RuntimeException {
    public AudioProcessingException(String message) {
        super(message);
    }

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
