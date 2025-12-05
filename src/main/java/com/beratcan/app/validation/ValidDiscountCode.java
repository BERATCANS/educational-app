package com.beratcan.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for discount code
 * Validates:
 * - PERCENTAGE: discountValue must be between 1-100
 * - FIXED_AMOUNT: discountValue must be >= 1
 * - validUntil must be after validFrom
 * - validUntil must be in the future
 * - minCartTotal must be <= maxCartTotal (if both are provided)
 */
@Documented
@Constraint(validatedBy = DiscountCodeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDiscountCode {
    String message() default "İndirim kodu validasyon hatası";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
