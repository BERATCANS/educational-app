package com.beratcan.app.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // email veya telefon veya username
    private String password;
}
