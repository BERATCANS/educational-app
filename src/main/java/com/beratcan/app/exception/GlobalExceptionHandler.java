package com.beratcan.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class GlobalExceptionHandler {

        @Value("${spring.profiles.active:prod}")
        private String activeProfile;

        private boolean isProduction() {
                return "prod".equals(activeProfile);
        }

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
        public ResponseEntity<ErrorDetails> handleIllegalArgumentException(
                        IllegalArgumentException ex, HttpServletRequest request) {
                log.warn("Illegal argument: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = isProduction()
                                ? "Invalid request parameters"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "Validation error",
                                HttpStatus.BAD_REQUEST);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorDetails> handleJsonParseError(HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                log.warn("JSON parse error: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = ex.getMessage();
                String details = "Malformed JSON request";

                // Enum parse hatası kontrolü
                if (message != null && message.contains("No enum constant")) {
                        // "No enum constant com.beratcan.app.entity.enums.ExamType.INVALID" gibi bir
                        // mesaj
                        details = "Geçersiz enum değeri. Lütfen geçerli bir değer gönderin.";
                        if (!isProduction()) {
                                details = message; // Dev modda detaylı mesaj
                        }
                } else if (message != null && message.contains("JSON parse error")) {
                        details = "JSON formatı hatalı. Lütfen request body'nin doğru formatta olduğundan emin olun.";
                }

                ErrorDetails errorDetails = buildErrorDetails(
                                isProduction() ? "Invalid request format" : message,
                                details,
                                HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().body(errorDetails);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorDetails> handleResourceNotFound(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                log.warn("Resource not found: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = isProduction()
                                ? "Resource not found"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "The requested resource was not found.",
                                HttpStatus.NOT_FOUND);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorDetails> handleBadRequest(
                        BadRequestException ex, HttpServletRequest request) {
                log.warn("Bad request: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = isProduction()
                                ? "Invalid request"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "Bad request",
                                HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().body(errorDetails);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorDetails> handleConflict(
                        ConflictException ex, HttpServletRequest request) {
                log.warn("Conflict: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = isProduction()
                                ? "Conflict detected"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "Conflict detected",
                                HttpStatus.CONFLICT);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
        }

        @ExceptionHandler(UnauthorizedOperationException.class)
        public ResponseEntity<ErrorDetails> handleUnauthorizedOperation(
                        UnauthorizedOperationException ex, HttpServletRequest request) {
                log.warn("Unauthorized operation: path={}, error={}", request.getRequestURI(), ex.getMessage());

                String message = isProduction()
                                ? "Access denied"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
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
        public ResponseEntity<ErrorDetails> handleExternalServiceException(
                        ExternalServiceException ex, HttpServletRequest request) {
                log.error("External service error: path={}, error={}",
                                request.getRequestURI(), ex.getMessage(), ex);

                String message = isProduction()
                                ? "External service temporarily unavailable"
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "External service error",
                                HttpStatus.BAD_GATEWAY);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorDetails);
        }

        @ExceptionHandler(FileStorageException.class)
        public ResponseEntity<ErrorDetails> handleFileStorageException(
                        FileStorageException ex, HttpServletRequest request) {
                log.error("File storage error: path={}, error={}",
                                request.getRequestURI(), ex.getMessage(), ex);

                String message = isProduction()
                                ? "File operation failed. Please try again later."
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                message,
                                "File storage error",
                                HttpStatus.INTERNAL_SERVER_ERROR);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<ErrorDetails> handleTooManyRequests(
                        TooManyRequestsException ex, HttpServletRequest request) {
                log.warn("Rate limit exceeded: path={}, retryAfter={}s",
                                request.getRequestURI(), ex.getRetryAfterSeconds());

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "Rate limit exceeded. Please try again later.",
                                HttpStatus.TOO_MANY_REQUESTS);

                // Response header'larına retry-after ekle
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header("X-Rate-Limit-Retry-After-Seconds", String.valueOf(ex.getRetryAfterSeconds()))
                                .body(errorDetails);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorDetails> handleGenericException(
                        Exception ex, HttpServletRequest request) {
                // DETAYLI LOG (her zaman - sadece server'da)
                log.error("Unexpected error: path={}, method={}, error={}",
                                request.getRequestURI(),
                                request.getMethod(),
                                ex.getMessage(),
                                ex);

                // PRODUCTION'DA GENERIC MESAJ, DEV'DE DETAYLI
                String details = isProduction()
                                ? "An unexpected error occurred. Please try again later."
                                : ex.getMessage();

                ErrorDetails errorDetails = buildErrorDetails(
                                "An unexpected error occurred",
                                details,
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
