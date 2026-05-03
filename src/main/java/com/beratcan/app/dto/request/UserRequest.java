package com.beratcan.app.dto.request;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 100, message = "Email en fazla 100 karakter olabilir")
    private String email;

    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = AppConstants.PHONE_NUMBER_PATTERN, message = "Geçersiz telefon numarası formatı")
    private String phoneNumber;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 30, message = "Kullanıcı adı 3-30 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Kullanıcı adı sadece harf, rakam ve alt çizgi içerebilir")
    private String username;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(max = 100, message = "Şifre en fazla 100 karakter olabilir")
    @ValidPassword
    private String password;

    @NotBlank(message = "Rol boş olamaz")
    private String role; // "STUDENT" veya "TEACHER"

    private String gradeLevel; // "1" - "12" veya "MEZUN"
    private String examType; // "LGS", "YKS", "KPSS", "DİĞER"
    private String fieldType; // "SAY", "EA", "SÖZ", "DİĞER" (sadece YKS için geçerli)
    private String gender; // "KADIN", "ERKEK", "DİĞER"
    private String city; // 81 il + "DİĞER"
    private String schoolType; // eğer sınavType YKS ise zorunlu olabilir
}
