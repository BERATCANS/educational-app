package com.beratcan.app.exception;

import com.beratcan.app.constants.AppTime;
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

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @Value("${spring.profiles.active:dev}")
        private String activeProfile;

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

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
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
                        details = ex.getMessage();
                } else if (message != null && message.contains("JSON parse error")) {
                        details = "JSON formatı hatalı. Lütfen request body'nin doğru formatta olduğundan emin olun.";
                }

                ErrorDetails errorDetails = buildErrorDetails(
                                "Invalid request format",
                                details,
                                HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().body(errorDetails);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorDetails> handleResourceNotFound(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                log.warn("Resource not found: path={}, error={}", request.getRequestURI(), ex.getMessage());

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "The requested resource was not found.",
                                HttpStatus.NOT_FOUND);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorDetails> handleBadRequest(
                        BadRequestException ex, HttpServletRequest request) {
                log.warn("Bad request: path={}, error={}", request.getRequestURI(), ex.getMessage());

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "Bad request",
                                HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().body(errorDetails);
        }

        @ExceptionHandler(ActiveClassroomMembershipException.class)
        public ResponseEntity<java.util.Map<String, Object>> handleActiveClassroomMembership(
                        ActiveClassroomMembershipException ex, HttpServletRequest request) {
                log.warn("Account deletion blocked - active classroom memberships: path={}, count={}",
                                request.getRequestURI(), ex.getActiveClassrooms().size());

                java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("message", ex.getMessage());
                body.put("activeClassrooms", ex.getActiveClassrooms());
                body.put("status", HttpStatus.BAD_REQUEST.value());
                body.put("timestamp", AppTime.now().toString());
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(DailyLimitExceededException.class)
        public ResponseEntity<ErrorDetails> handleDailyLimitExceeded(
                        DailyLimitExceededException ex, HttpServletRequest request) {
                log.warn("Daily limit exceeded: path={}, error={}", request.getRequestURI(), ex.getMessage());

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "Daily limit exceeded",
                                HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().body(errorDetails);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorDetails> handleConflict(
                        ConflictException ex, HttpServletRequest request) {
                log.warn("Conflict: path={}, error={}", request.getRequestURI(), ex.getMessage());

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "Conflict detected",
                                HttpStatus.CONFLICT);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
        }

        @ExceptionHandler(PriceChangedException.class)
        public ResponseEntity<java.util.Map<String, Object>> handlePriceChanged(
                        PriceChangedException ex, HttpServletRequest request) {
                log.info("Price-change conflict at checkout: path={}, changedCount={}",
                                request.getRequestURI(), ex.getChangedItems().size());

                java.util.List<java.util.Map<String, Object>> changedItems = ex.getChangedItems().stream()
                                .map(c -> {
                                        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                                        m.put("cartItemId", c.getCartItemId());
                                        m.put("bookId", c.getBookId());
                                        m.put("bookTitle", c.getBookTitle());
                                        m.put("oldPrice", c.getOldPrice());
                                        m.put("newPrice", c.getNewPrice());
                                        return m;
                                })
                                .collect(java.util.stream.Collectors.toList());

                java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("error", "PRICE_CHANGED");
                body.put("message", ex.getMessage());
                body.put("changedItems", changedItems);
                body.put("status", HttpStatus.CONFLICT.value());
                body.put("timestamp", AppTime.now().toString());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        @ExceptionHandler(UnauthorizedOperationException.class)
        public ResponseEntity<ErrorDetails> handleUnauthorizedOperation(
                        UnauthorizedOperationException ex, HttpServletRequest request) {
                log.warn("Unauthorized operation: path={}, error={}", request.getRequestURI(), ex.getMessage());

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
        public ResponseEntity<ErrorDetails> handleExternalServiceException(
                        ExternalServiceException ex, HttpServletRequest request) {
                log.error("External service error: path={}, error={}",
                                request.getRequestURI(), ex.getMessage(), ex);

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
                                "External service error",
                                HttpStatus.BAD_GATEWAY);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorDetails);
        }

        @ExceptionHandler(FileStorageException.class)
        public ResponseEntity<ErrorDetails> handleFileStorageException(
                        FileStorageException ex, HttpServletRequest request) {
                log.error("File storage error: path={}, error={}",
                                request.getRequestURI(), ex.getMessage(), ex);

                ErrorDetails errorDetails = buildErrorDetails(
                                ex.getMessage(),
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

                String details = "prod".equals(activeProfile)
                                ? "Internal server error"
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
                                .timestamp(AppTime.now().toString())
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .build();
        }

        private String formatFieldError(FieldError error) {
                return error.getField() + ": " + error.getDefaultMessage();
        }
}
