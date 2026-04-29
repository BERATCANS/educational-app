package com.beratcan.app.exception;

import com.beratcan.app.dto.response.ActiveClassroomMembershipDto;
import lombok.Getter;

import java.util.List;

@Getter
public class ActiveClassroomMembershipException extends RuntimeException {

    private final List<ActiveClassroomMembershipDto> activeClassrooms;

    public ActiveClassroomMembershipException(String message, List<ActiveClassroomMembershipDto> activeClassrooms) {
        super(message);
        this.activeClassrooms = activeClassrooms;
    }
}
