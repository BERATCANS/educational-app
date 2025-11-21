package com.beratcan.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> formatFieldError(error))
                .collect(Collectors.joining("; "));

        ErrorDetails errorDetails = buildErrorDetails(
                "Validation failed",
                details,
                HttpStatus.BAD_REQUEST);

        return ResponseEntity.badRequest().body(errorDetails);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "Validation error",
                HttpStatus.BAD_REQUEST);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> handleJsonParseError(HttpMessageNotReadableException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                "Malformed JSON request",
                "Please ensure that the request body is correctly formatted.",
                HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest().body(errorDetails);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "The requested resource was not found.",
                HttpStatus.NOT_FOUND);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequest(BadRequestException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "Bad request",
                HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest().body(errorDetails);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDetails> handleConflict(ConflictException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "Conflict detected",
                HttpStatus.CONFLICT);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorDetails> handleUnauthorizedOperation(UnauthorizedOperationException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "You are not allowed to perform this operation.",
                HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                "Access Denied",
                "You do not have permission to access this resource.",
                HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorDetails> handleExternalServiceException(ExternalServiceException ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                ex.getMessage(),
                "External service error",
                HttpStatus.BAD_GATEWAY);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorDetails);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGenericException(Exception ex) {
        ErrorDetails errorDetails = buildErrorDetails(
                "An unexpected error occurred",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    private ErrorDetails buildErrorDetails(String message, String details, HttpStatus status) {
        return ErrorDetails.builder()
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .build();
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
