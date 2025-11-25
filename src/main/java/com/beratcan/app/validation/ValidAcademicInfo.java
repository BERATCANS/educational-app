package com.beratcan.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for academic info
 * YKS seçildiğinde fieldType ve schoolType zorunlu
 * LGS veya diğer sınav türleri seçildiğinde fieldType ve schoolType null olmalı
 */
@Documented
@Constraint(validatedBy = AcademicInfoValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAcademicInfo {
    String message() default "YKS için alan ve okul türü zorunludur. Diğer sınav türleri için bu alanlar boş olmalıdır.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

