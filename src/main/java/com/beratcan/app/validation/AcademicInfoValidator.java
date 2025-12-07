package com.beratcan.app.validation;

import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.entity.enums.ExamType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates academic info based on exam type
 * - YKS: fieldType, gradeLevel ve schoolType zorunlu
 * - LGS: sadece gradeLevel zorunlu
 * - KPSS ve DIGER: hiçbir şey zorunlu değil
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

        context.disableDefaultConstraintViolation();

        // YKS: fieldType, gradeLevel ve schoolType zorunlu
        if (examType == ExamType.YKS) {
            boolean hasError = false;

            if (request.getFieldType() == null) {
                context.buildConstraintViolationWithTemplate("YKS için alan türü (fieldType) zorunludur")
                        .addPropertyNode("fieldType")
                        .addConstraintViolation();
                hasError = true;
            }

            if (request.getGradeLevel() == null) {
                context.buildConstraintViolationWithTemplate("YKS için sınıf seviyesi (gradeLevel) zorunludur")
                        .addPropertyNode("gradeLevel")
                        .addConstraintViolation();
                hasError = true;
            }

            if (request.getSchoolType() == null) {
                context.buildConstraintViolationWithTemplate("YKS için okul türü (schoolType) zorunludur")
                        .addPropertyNode("schoolType")
                        .addConstraintViolation();
                hasError = true;
            }

            return !hasError;
        }

        // LGS: sadece gradeLevel zorunlu
        if (examType == ExamType.LGS) {
            if (request.getGradeLevel() == null) {
                context.buildConstraintViolationWithTemplate("LGS için sınıf seviyesi (gradeLevel) zorunludur")
                        .addPropertyNode("gradeLevel")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

        // KPSS ve DIGER: hiçbir şey zorunlu değil
        // Validasyon geçer, zorunlu olmayan alanlar service katmanında temizlenecek
        return true;
    }
}
