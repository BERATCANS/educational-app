package com.beratcan.app.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAnalysisRequest {
    private List<String> hints; // Hints listesi (opsiyonel)
    private String explanation; // Explanation (opsiyonel)
}
