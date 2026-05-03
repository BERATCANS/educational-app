package com.beratcan.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveClassroomMembershipDto {
    private Long id;
    private String name;
    private Boolean isLastTeacher;
}
