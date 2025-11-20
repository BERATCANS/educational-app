package com.beratcan.app.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    private final String message;
    private final String details;
    private final String timestamp;
    private final Integer status;
    private final String error;
}
