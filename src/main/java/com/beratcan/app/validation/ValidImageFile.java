package com.beratcan.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for image file uploads
 * Validates:
 * - File is not empty
 * - File size is within limit (default 5MB)
 * - File type is an image (jpeg, jpg, png, gif, webp)
 */
@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFile {
    String message() default "Geçersiz görsel dosyası";

    /**
     * Maximum file size in bytes (default: 5MB)
     */
    long maxSize() default 5 * 1024 * 1024; // 5MB

    /**
     * Allowed MIME types
     */
    String[] allowedTypes() default {
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    };

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
