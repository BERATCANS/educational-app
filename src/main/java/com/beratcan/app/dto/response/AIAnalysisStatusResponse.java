package com.beratcan.app.dto.response;

import com.beratcan.app.entity.enums.AIAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisStatusResponse {
    private Long questionId;
    private AIAnalysisStatus status;
    private String message;
}
