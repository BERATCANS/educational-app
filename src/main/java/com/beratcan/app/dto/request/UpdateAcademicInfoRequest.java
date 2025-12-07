package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.FieldType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.SchoolType;
import com.beratcan.app.validation.ValidAcademicInfo;
import lombok.Data;

@Data
@ValidAcademicInfo
public class UpdateAcademicInfoRequest {
    private ExamType examType;
    private GradeLevel gradeLevel;

    // YKS için zorunlu alanlar (ValidAcademicInfo validator ile kontrol edilir)
    private FieldType fieldType;
    private SchoolType schoolType;
}
