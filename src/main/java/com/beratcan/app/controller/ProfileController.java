package com.beratcan.app.controller;

import com.beratcan.app.dto.request.ChangeEmailRequest;
import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.dto.request.UpdatePasswordRequest;
import com.beratcan.app.dto.request.UpdateProfileRequest;
import com.beratcan.app.dto.request.UpdateQuestionTargetRequest;
import com.beratcan.app.dto.response.ProfileAcademicResponse;
import com.beratcan.app.dto.response.ProfileDetailsResponse;
import com.beratcan.app.dto.response.QuestionTargetResponse;
import com.beratcan.app.dto.response.StreakResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.entity.User;
import com.beratcan.app.service.ProfileService;
import com.beratcan.app.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @RateLimit(requestsPerMinute = 30, keyType = "USER", message = "Profil bilgileri sık isteniyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ProfileService.UserWithStreakUpdate result = profileService.getCurrentUserWithStreakUpdate(userId);
        return ResponseEntity.ok(new UserResponse(result.getUser(), result.isStreakUpdated()));
    }

    @GetMapping("/academic")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Akademik bilgiler çok sık isteniyor, lütfen bekleyin.")
    public ResponseEntity<ProfileAcademicResponse> getAcademicInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        User user = profileService.getCurrentUser(userId);
        return ResponseEntity.ok(ProfileAcademicResponse.fromUser(user));
    }

    @PutMapping("/academic")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Akademik bilgileri sık değiştiremezsiniz.")
    public ResponseEntity<ProfileAcademicResponse> updateAcademicInfo(
            @Valid @RequestBody UpdateAcademicInfoRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        User updated = profileService.updateAcademicInfo(userId, request);
        return ResponseEntity.ok(ProfileAcademicResponse.fromUser(updated));
    }

    @GetMapping("/details")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Profil bilgileri sık isteniyor, lütfen daha sonra tekrar deneyin.")
    public ResponseEntity<ProfileDetailsResponse> getProfileDetails(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        User user = profileService.getCurrentUser(userId);
        return ResponseEntity.ok(ProfileDetailsResponse.fromUser(user));
    }

    @PutMapping("/details")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Profil bilgilerini sık değiştiremezsiniz.")
    public ResponseEntity<ProfileDetailsResponse> updateProfileDetails(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        User updated = profileService.updateProfileDetails(userId, request);
        return ResponseEntity.ok(ProfileDetailsResponse.fromUser(updated));
    }

    @PutMapping("/change-email")
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Email değiştirme isteği çok sık gönderildi.")
    public ResponseEntity<Map<String, String>> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        profileService.changeEmail(userId, request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Email değiştirildi. Yeni email adresinize gönderilen doğrulama linkine tıklayarak email'inizi doğrulayın.",
                "emailChanged", "true"));
    }

    @PostMapping("/resend-verification")
    @RateLimit(requestsPerMinute = 3, keyType = "USER", message = "Tekrar email gönderme isteği çok sık gönderildi.")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        profileService.resendVerificationEmail(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Doğrulama email'i gönderildi. Lütfen email kutunuzu kontrol edin.",
                "status", "success"));
    }

    @PutMapping("/password")
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Şifre güncelleme isteği çok sık gönderildi.")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        profileService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Şifreniz başarıyla güncellendi"));
    }

    @GetMapping("/question-target")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Soru hedefi bilgisi sık isteniyor, lütfen daha sonra deneyin.")
    public ResponseEntity<QuestionTargetResponse> getQuestionTarget(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.getQuestionTarget(userId));
    }

    @PutMapping("/question-target")
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Soru hedefi güncelleme isteği çok sık gönderildi.")
    public ResponseEntity<QuestionTargetResponse> updateQuestionTarget(
            @Valid @RequestBody UpdateQuestionTargetRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.updateQuestionTarget(userId, request));
    }

    @GetMapping("/streak")
    @RateLimit(requestsPerMinute = 20, keyType = "USER", message = "Streak bilgisi sık isteniyor, lütfen daha sonra deneyin.")
    public ResponseEntity<StreakResponse> getStreak(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(profileService.getStreak(userId));
    }
}
