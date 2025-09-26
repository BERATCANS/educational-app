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
    private String gradeLevel; // "1" - "12" veya "MEZUN"
    private String examType; // "LGS", "YKS", "KPSS", "DİĞER"
    private String fieldType; // "SAY", "EA", "SÖZ", "DİĞER" (sadece YKS için geçerli)
    private String gender; // "KADIN", "ERKEK", "DİĞER"
    private String city; // 81 il + "DİĞER"
    private String schoolType; // eğer sınavType YKS ise zorunlu olabilir
}
