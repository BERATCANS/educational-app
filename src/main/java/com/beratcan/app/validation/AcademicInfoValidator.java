package com.beratcan.app.validation;

import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.entity.enums.ExamType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates academic info based on exam type
 * - YKS: fieldType ve schoolType zorunlu
 * - LGS veya diğerleri: fieldType ve schoolType null olmalı
 */
public class AcademicInfoValidator implements ConstraintValidator<ValidAcademicInfo, UpdateAcademicInfoRequest> {

    @Override
    public void initialize(ValidAcademicInfo constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(UpdateAcademicInfoRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Null kontrolü başka validator'da yapılır
        }

        ExamType examType = request.getExamType();

        // ExamType null ise validation geçer (opsiyonel alan)
        if (examType == null) {
            return true;
        }

        // YKS seçildiyse fieldType ve schoolType zorunlu
        if (examType == ExamType.YKS) {
            if (request.getFieldType() == null || request.getSchoolType() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "YKS için alan türü (fieldType) ve okul türü (schoolType) zorunludur")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

        // YKS dışındaki sınav türleri için fieldType ve schoolType null olmalı
        if (request.getFieldType() != null || request.getSchoolType() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    examType.name() + " için alan türü (fieldType) ve okul türü (schoolType) boş olmalıdır")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

