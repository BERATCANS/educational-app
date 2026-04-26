package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomSummaryDto {
    private Long id;
    private String name;
    private String gradeLevel;
    private String examType;
    private List<String> teacherNames;
}
