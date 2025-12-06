package com.beratcan.app.service;

import com.beratcan.app.dto.request.ChangeEmailRequest;
import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.dto.request.UpdatePasswordRequest;
import com.beratcan.app.dto.request.UpdateProfileRequest;
import com.beratcan.app.dto.request.UpdateQuestionTargetRequest;
import com.beratcan.app.dto.response.QuestionTargetResponse;
import com.beratcan.app.dto.response.StreakResponse;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userService.getUserById(userId);
    }

    @Transactional
    public User updateAcademicInfo(Long userId, UpdateAcademicInfoRequest request) {
        if (request == null) {
            throw new BadRequestException("Güncelleme isteği boş olamaz");
        }
        User user = userService.getUserById(userId);

        ExamType examType = request.getExamType();

        // ExamType güncelle
        user.setExamType(examType);

        // YKS: fieldType, gradeLevel ve schoolType zorunlu (validator kontrol etti)
        if (examType == ExamType.YKS) {
            user.setGradeLevel(request.getGradeLevel());
            user.setFieldType(request.getFieldType());
            user.setSchoolType(request.getSchoolType());
        }
        // LGS: sadece gradeLevel zorunlu (validator kontrol etti)
        else if (examType == ExamType.LGS) {
            user.setGradeLevel(request.getGradeLevel());
            // LGS için fieldType ve schoolType null olmalı
            user.setFieldType(null);
            user.setSchoolType(null);
        }
        // KPSS ve DIGER: hiçbir şey zorunlu değil
        else if (examType == ExamType.KPSS || examType == ExamType.DIGER) {
            // GradeLevel opsiyonel, gönderilmişse set et
            user.setGradeLevel(request.getGradeLevel());
            // KPSS ve DIGER için fieldType ve schoolType null olmalı
            user.setFieldType(null);
            user.setSchoolType(null);
        }
        // ExamType null ise tüm alanları temizle
        else {
            user.setGradeLevel(null);
            user.setFieldType(null);
            user.setSchoolType(null);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileDetails(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new BadRequestException("Güncelleme isteği boş olamaz");
        }
        User user = userService.getUserById(userId);
        boolean updated = false;

        if (StringUtils.hasText(request.getFirstName()) && !request.getFirstName().trim().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
            updated = true;
        }

        if (StringUtils.hasText(request.getLastName()) && !request.getLastName().trim().equals(user.getLastName())) {
            user.setLastName(request.getLastName().trim());
            updated = true;
        }

        // Email değiştirme artık ayrı endpoint'te: changeEmailForVerifiedUser()

        if (StringUtils.hasText(request.getPhoneNumber()) && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (!UserService.PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
                throw new BadRequestException("Geçersiz telefon numarası formatı.");
            }
            if (userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), userId)) {
                throw new ConflictException("Bu telefon numarası başka bir kullanıcı tarafından kullanılıyor.");
            }
            user.setPhoneNumber(request.getPhoneNumber().trim());
            updated = true;
        }

        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(request.getUsername(), userId)) {
                throw new ConflictException("Bu kullanıcı adı başka bir kullanıcı tarafından kullanılıyor.");
            }
            user.setUsername(request.getUsername().trim());
            updated = true;
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
            updated = true;
        }

        if (request.getCity() != null) {
            user.setCity(request.getCity());
            updated = true;
        }

        if (!updated) {
            throw new BadRequestException("Güncellenecek alan bulunamadı.");
        }

        return userRepository.save(user);
    }

    /**
     * Authenticated kullanıcı için email değiştirme
     * Email değiştirildiğinde:
     * - emailVerified → false yapılır
     * - emailVerificationToken → null yapılır (yeni token oluşturulacak)
     * - emailVerifiedAt → null yapılır
     * - Yeni email'e verification email gönderilir
     * - Logout yapılmaz (kullanıcı oturumda kalır)
     */
    @Transactional
    public void changeEmail(Long userId, ChangeEmailRequest request) {
        if (request == null || request.getNewEmail() == null || request.getNewEmail().trim().isEmpty()) {
            throw new BadRequestException("Yeni email adresi boş olamaz");
        }

        User user = userService.getUserById(userId);

        // Email format kontrolü
        String normalizedNewEmail = request.getNewEmail().trim().toLowerCase();
        if (!normalizedNewEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("Geçersiz email formatı");
        }

        // Yeni email mevcut email ile aynı mı?
        String normalizedCurrentEmail = user.getEmail().trim().toLowerCase();
        if (normalizedCurrentEmail.equals(normalizedNewEmail)) {
            throw new BadRequestException("Yeni email mevcut email ile aynı olamaz.");
        }

        // Yeni email başka bir kullanıcı tarafından kullanılıyor mu?
        if (userRepository.existsByEmailAndIdNot(normalizedNewEmail, userId)) {
            throw new ConflictException("Bu email başka bir kullanıcı tarafından kullanılıyor.");
        }

        // Email değiştir
        String oldEmail = user.getEmail();
        user.setEmail(normalizedNewEmail);

        // Email verification durumunu sıfırla
        user.setEmailVerified(false);
        user.setEmailVerificationToken(null);
        user.setEmailVerifiedAt(null);

        userRepository.save(user);

        // Yeni email'e verification email gönder
        emailVerificationService.sendVerificationEmail(user);

        // Logout yapılmaz - kullanıcı oturumda kalır

        log.info(
                "Email changed: userId={}, oldEmail={}, newEmail={}. Verification email sent. User remains logged in.",
                userId, oldEmail, normalizedNewEmail);
    }

    /**
     * Authenticated kullanıcı için doğrulama email'ini tekrar gönder
     */
    @Transactional
    public void resendVerificationEmail(Long userId) {
        emailVerificationService.resendVerificationEmail(userId);
    }

    @Transactional
    public void changePassword(Long userId, UpdatePasswordRequest request) {
        if (request == null) {
            throw new BadRequestException("Şifre güncelleme isteği boş olamaz");
        }
        User user = userService.getUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mevcut şifre hatalı");
        }
        if (!StringUtils.hasText(request.getNewPassword()) || request.getNewPassword().length() < 6) {
            throw new BadRequestException("Yeni şifre en az 6 karakter olmalıdır");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Yeni şifre ile doğrulama şifresi eşleşmiyor");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Yeni şifre mevcut şifre ile aynı olamaz");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public QuestionTargetResponse getQuestionTarget(Long userId) {
        User user = userService.getUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        Long solved = testResultRepository.sumSolvedQuestionsBetween(userId, start, end);
        int target = user.getDailyQuestionTarget() != null ? user.getDailyQuestionTarget() : 0;
        int solvedToday = solved != null ? solved.intValue() : 0;
        int remaining = Math.max(target - solvedToday, 0);
        double completion = target > 0 ? Math.min((solvedToday * 100.0) / target, 100.0) : 0.0;

        return QuestionTargetResponse.builder()
                .target(target)
                .solvedToday(solvedToday)
                .remaining(remaining)
                .completionRate(completion)
                .date(today.toString())
                .build();
    }

    @Transactional
    public QuestionTargetResponse updateQuestionTarget(Long userId, UpdateQuestionTargetRequest request) {
        if (request == null || request.getTarget() <= 0) {
            throw new BadRequestException("Hedef en az 1 olmalıdır");
        }
        User user = userService.getUserById(userId);
        user.setDailyQuestionTarget(request.getTarget());
        userRepository.save(user);
        return getQuestionTarget(userId);
    }

    /**
     * Kullanıcının streak (günlük giriş serisi) bilgisini döndürür
     * 
     * @param userId Kullanıcı ID
     * @return StreakResponse (currentStreak, longestStreak, lastLoginDate,
     *         loggedInToday)
     */
    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long userId) {
        User user = userService.getUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = user.getLastLoginDate();

        boolean loggedInToday = lastLogin != null && lastLogin.equals(today);

        return StreakResponse.builder()
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .longestStreak(user.getLongestStreak() != null ? user.getLongestStreak() : 0)
                .lastLoginDate(lastLogin != null ? lastLogin.toString() : null)
                .loggedInToday(loggedInToday)
                .build();
    }
}
