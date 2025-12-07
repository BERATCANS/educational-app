package com.beratcan.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {

    @NotNull(message = "bookId boş olamaz")
    @Min(value = 1, message = "bookId en az 1 olmalıdır")
    private Long bookId;
}
