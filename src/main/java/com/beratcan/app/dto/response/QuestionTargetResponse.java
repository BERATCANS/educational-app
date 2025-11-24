package com.beratcan.app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionTargetResponse {
    private int target;
    private int solvedToday;
    private int remaining;
    private double completionRate;
    private String date;
}
