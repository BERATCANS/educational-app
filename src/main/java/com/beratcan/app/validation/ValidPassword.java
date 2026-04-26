package com.beratcan.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Şifre en az 8 karakter olmalı; en az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 özel karakter içermelidir";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
