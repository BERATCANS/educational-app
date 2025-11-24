package com.beratcan.app.dto.response;

import com.beratcan.app.entity.User;
import lombok.Data;

@Data
public class ProfileDetailsResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String username;
    private String gender;
    private String city;

    public static ProfileDetailsResponse fromUser(User user) {
        ProfileDetailsResponse response = new ProfileDetailsResponse();
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setUsername(user.getUsername());
        response.setGender(user.getGender() != null ? user.getGender().name() : null);
        response.setCity(user.getCity() != null ? user.getCity().name() : null);
        return response;
    }
}
