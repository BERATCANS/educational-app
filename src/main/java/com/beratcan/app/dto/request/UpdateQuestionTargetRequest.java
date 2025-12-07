package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateQuestionTargetRequest {

    @NotNull(message = "Hedef (target) boş olamaz")
    @Min(value = 1, message = "Hedef en az 1 olmalıdır")
    @Max(value = 10000, message = "Hedef 10000'den büyük olamaz")
    private Integer target;
}
