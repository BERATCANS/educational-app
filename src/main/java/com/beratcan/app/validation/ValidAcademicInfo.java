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
 * - YKS: fieldType, gradeLevel ve schoolType zorunlu
 * - LGS: sadece gradeLevel zorunlu
 * - KPSS ve DIGER: hiçbir şey zorunlu değil
 */
@Documented
@Constraint(validatedBy = AcademicInfoValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAcademicInfo {
    String message() default "Akademik bilgi validasyon hatası";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
