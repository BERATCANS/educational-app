package com.beratcan.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates image file uploads
 * - File is not empty
 * - File size is within limit
 * - File type is an allowed image type
 */
public class ImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private long maxSize;
    private String[] allowedTypes;

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        // Minimum 5MB garantisi - annotation'da belirtilen değer 5MB'tan küçükse 5MB
        // kullan
        long annotationMaxSize = constraintAnnotation.maxSize();
        long minSize = 5 * 1024 * 1024; // 5MB
        this.maxSize = Math.max(annotationMaxSize, minSize);
        this.allowedTypes = constraintAnnotation.allowedTypes();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // Null kontrolü - opsiyonel alanlar için (UpdateQuestionRequest gibi)
        if (file == null || file.isEmpty()) {
            return true; // Null/empty kontrolü @NotNull ile yapılır veya opsiyonel alanlar için geçerli
        }

        context.disableDefaultConstraintViolation();

        // Dosya boyutu kontrolü
        if (file.getSize() > maxSize) {
            long maxSizeMB = maxSize / (1024 * 1024);
            context.buildConstraintViolationWithTemplate(
                    String.format("Görsel dosyası en fazla %d MB olabilir", maxSizeMB))
                    .addConstraintViolation();
            return false;
        }

        // MIME type kontrolü
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            context.buildConstraintViolationWithTemplate("Görsel dosyası tipi belirlenemedi")
                    .addConstraintViolation();
            return false;
        }

        boolean isAllowedType = false;
        for (String allowedType : allowedTypes) {
            if (contentType.equalsIgnoreCase(allowedType)) {
                isAllowedType = true;
                break;
            }
        }

        if (!isAllowedType) {
            context.buildConstraintViolationWithTemplate(
                    String.format("Görsel dosyası tipi desteklenmiyor. İzin verilen tipler: %s",
                            String.join(", ", allowedTypes)))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
