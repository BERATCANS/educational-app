package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomDto {
    private Long id;
    private String name;
    private String description;
    private String gradeLevel;
    private String examType;
    private String studentJoinCode;
    private String teacherJoinCode;
    private Boolean isActive;
    private Integer studentCount;
    private Integer teacherCount;
    private LocalDateTime createdAt;
}
