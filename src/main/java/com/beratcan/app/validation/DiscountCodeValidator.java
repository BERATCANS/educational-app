package com.beratcan.app.validation;

import com.beratcan.app.dto.request.DiscountCodeRequest;
import com.beratcan.app.entity.enums.DiscountType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Validates discount code request
 * - PERCENTAGE: discountValue must be between 1-100
 * - FIXED_AMOUNT: discountValue must be >= 1
 * - validUntil must be after validFrom
 * - validUntil must be in the future
 * - minCartTotal must be <= maxCartTotal (if both are provided)
 */
public class DiscountCodeValidator implements ConstraintValidator<ValidDiscountCode, DiscountCodeRequest> {

    private static final BigDecimal MIN_PERCENTAGE = BigDecimal.ONE;
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");
    private static final BigDecimal MIN_FIXED_AMOUNT = BigDecimal.ONE;

    @Override
    public void initialize(ValidDiscountCode constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(DiscountCodeRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Null kontrolü başka validator'da yapılır
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Discount value validasyonu (discountType'a göre)
        if (request.getDiscountType() != null && request.getDiscountValue() != null) {
            if (request.getDiscountType() == DiscountType.PERCENTAGE) {
                // Percentage: 1-100 arası olmalı
                if (request.getDiscountValue().compareTo(MIN_PERCENTAGE) < 0 ||
                        request.getDiscountValue().compareTo(MAX_PERCENTAGE) > 0) {
                    context.buildConstraintViolationWithTemplate(
                            "Yüzde indirim değeri 1 ile 100 arasında olmalıdır")
                            .addPropertyNode("discountValue")
                            .addConstraintViolation();
                    isValid = false;
                }
            } else if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                // Fixed amount: en az 1 olmalı
                if (request.getDiscountValue().compareTo(MIN_FIXED_AMOUNT) < 0) {
                    context.buildConstraintViolationWithTemplate(
                            "Sabit tutar indirimi en az 1 TL olmalıdır")
                            .addPropertyNode("discountValue")
                            .addConstraintViolation();
                    isValid = false;
                }
            }
        }

        // Tarih validasyonları
        if (request.getValidFrom() != null && request.getValidUntil() != null) {
            // validUntil must be after validFrom
            if (request.getValidUntil().isBefore(request.getValidFrom()) ||
                    request.getValidUntil().isEqual(request.getValidFrom())) {
                context.buildConstraintViolationWithTemplate(
                        "Geçerlilik bitiş tarihi, başlangıç tarihinden sonra olmalıdır")
                        .addPropertyNode("validUntil")
                        .addConstraintViolation();
                isValid = false;
            }

            // validUntil must be in the future
            if (request.getValidUntil().isBefore(LocalDateTime.now())) {
                context.buildConstraintViolationWithTemplate(
                        "Geçerlilik bitiş tarihi geçmiş bir tarih olamaz")
                        .addPropertyNode("validUntil")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Min-max cart total validasyonu
        if (request.getMinCartTotal() != null && request.getMaxCartTotal() != null) {
            if (request.getMinCartTotal().compareTo(request.getMaxCartTotal()) > 0) {
                context.buildConstraintViolationWithTemplate(
                        "Minimum sepet tutarı, maksimum sepet tutarından büyük olamaz")
                        .addPropertyNode("minCartTotal")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        // Publisher ve Books aynı anda dolu olamaz (OR mantığı — ya biri ya öteki)
        if (request.getApplicablePublisherId() != null &&
                request.getApplicableBookIds() != null && !request.getApplicableBookIds().isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                    "Yayınevi ve kitap filtreleri aynı anda kullanılamaz. Ya yayınevi ya da kitap filtresi seçin.")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
