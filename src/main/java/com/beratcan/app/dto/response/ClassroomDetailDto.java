package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ClassroomDetailDto {
    private Long id;
    private String name;
    private String description;
    private String gradeLevel;
    private String examType;
    private String studentJoinCode;
    private String teacherJoinCode;
    private Boolean isActive;
    private java.time.LocalDateTime createdAt;
    private List<TeacherSummaryDto> teachers;
    private List<StudentSummaryDto> students;
}
