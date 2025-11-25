package com.beratcan.app.dto.request;

import com.beratcan.app.entity.enums.City;
import com.beratcan.app.entity.enums.Gender;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private Gender gender;
    private City city;
}
