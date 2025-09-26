package com.beratcan.app.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // email veya username
    private String password;
}
