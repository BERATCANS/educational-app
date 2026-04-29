package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSummaryDto {
    private Long teacherProfileId;
    private String firstName;
    private String lastName;
    private String username;
    private Boolean isCreator;
}
