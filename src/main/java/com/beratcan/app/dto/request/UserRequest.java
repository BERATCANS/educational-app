package com.beratcan.app.dto.request;

import lombok.Data;

@Data
public class UserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private String password;
    private String role; // "STUDENT" veya "TEACHER"
}
