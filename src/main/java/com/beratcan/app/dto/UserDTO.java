package com.beratcan.app.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private String password;
    private String role; // "STUDENT" veya "TEACHER"
}
