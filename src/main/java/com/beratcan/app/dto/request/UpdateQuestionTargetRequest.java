package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuestionTargetRequest {

    @Min(value = 1, message = "Hedef en az 1 olmalıdır")
    @Max(value = 10000, message = "Hedef 10000'den büyük olamaz")
    private int target;
}
