package com.beratcan.app.dto.request;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.entity.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    // Tüm alanlar opsiyonel - sadece set edilirse validasyon yapılır
    // Service katmanında StringUtils.hasText kontrolü var

    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;

    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;

    // Email artık burada değil - ayrı endpoint: PUT /api/profile/change-email

    @Pattern(regexp = AppConstants.PHONE_NUMBER_PATTERN, message = "Geçersiz telefon numarası formatı")
    private String phoneNumber;

    @Size(min = 3, max = 30, message = "Kullanıcı adı 3-30 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Kullanıcı adı sadece harf, rakam ve alt çizgi içerebilir")
    private String username;

    private Gender gender;

    private City city;
}
