package com.beratcan.app.service;

import com.beratcan.app.dto.request.ChangeEmailRequest;
import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.dto.request.UpdatePasswordRequest;
import com.beratcan.app.dto.request.UpdateProfileRequest;
import com.beratcan.app.dto.request.UpdateQuestionTargetRequest;
import com.beratcan.app.dto.response.QuestionTargetResponse;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.repository.StudentProfileRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.constants.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public User getCurrentUser(Long userId) {
        User user = userService.getUserById(userId);
        // Her profile/me çağrısında streak güncelle (uygulama açma = aktivite, sadece öğrenciler)
        studentProfileRepository.findByUserId(userId).ifPresent(this::updateStreak);
        return user;
    }

    /**
     * Kullanıcı bilgilerini getirir ve streak güncellemesi yapar
     * Streak güncellenip güncellenmediğini döndürür
     *
     * @param userId Kullanıcı ID
     * @return User ve streak güncellenip güncellenmediği bilgisi
     */
    @Transactional
    public UserWithStreakUpdate getCurrentUserWithStreakUpdate(Long userId) {
        User user = userService.getUserById(userId);
        // Streak güncelle (sadece öğrenciler)
        boolean streakUpdated = studentProfileRepository.findByUserId(userId)
                .map(this::updateStreak)
                .orElse(false);
        return new UserWithStreakUpdate(user, streakUpdated);
    }

    /**
     * Wrapper class for User with streak update information
     */
    public static class UserWithStreakUpdate {
        private final User user;
        private final boolean streakUpdated;

        public UserWithStreakUpdate(User user, boolean streakUpdated) {
            this.user = user;
            this.streakUpdated = streakUpdated;
        }

        public User getUser() {
            return user;
        }

        public boolean isStreakUpdated() {
            return streakUpdated;
        }
    }

    @Transactional
    public StudentProfile updateAcademicInfo(Long userId, UpdateAcademicInfoRequest request) {
        if (request == null) {
            throw new BadRequestException("Güncelleme isteği boş olamaz");
        }
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Öğrenci profili bulunamadı: userId=" + userId));

        ExamType examType = request.getExamType();

        // ExamType güncelle
        profile.setExamType(examType);

        // YKS: fieldType, gradeLevel ve schoolType zorunlu (validator kontrol etti)
        if (examType == ExamType.YKS) {
            profile.setGradeLevel(request.getGradeLevel());
            profile.setFieldType(request.getFieldType());
            profile.setSchoolType(request.getSchoolType());
        }
        // LGS: sadece gradeLevel zorunlu (validator kontrol etti)
        else if (examType == ExamType.LGS) {
            profile.setGradeLevel(request.getGradeLevel());
            profile.setFieldType(null);
            profile.setSchoolType(null);
        }
        // KPSS ve DIGER: hiçbir şey zorunlu değil
        else if (examType == ExamType.KPSS || examType == ExamType.DIGER) {
            profile.setGradeLevel(request.getGradeLevel());
            profile.setFieldType(null);
            profile.setSchoolType(null);
        }
        // ExamType null ise tüm alanları temizle
        else {
            profile.setGradeLevel(null);
            profile.setFieldType(null);
            profile.setSchoolType(null);
        }

        return studentProfileRepository.save(profile);
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
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Öğrenci profili bulunamadı: userId=" + userId));
        LocalDate today = LocalDate.now(AppTime.ZONE);
        int target = profile.getDailyQuestionTarget() != null ? profile.getDailyQuestionTarget() : 0;
        int solvedToday = profile.getDailySolvedQuestions() != null ? profile.getDailySolvedQuestions() : 0;
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
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Öğrenci profili bulunamadı: userId=" + userId));
        profile.setDailyQuestionTarget(request.getTarget());
        studentProfileRepository.save(profile);
        return getQuestionTarget(userId);
    }

    /**
     * Kullanıcının streak (günlük giriş serisi) bilgisini günceller
     * - İlk aktivite: streak = 1
     * - Bugün zaten aktivite yapılmışsa: streak değişmez
     * - Dün aktivite yapılmışsa: streak += 1
     * - Dünden önce aktivite yapılmışsa: streak = 1 (reset)
     * 
     * @param user Kullanıcı entity
     * @return true eğer streak güncellendiyse, false eğer değişmediyse
     */
    private boolean updateStreak(StudentProfile profile) {
        LocalDate today = LocalDate.now(AppTime.ZONE);
        LocalDate lastActivity = profile.getLastActivityDate();
        String username = profile.getUser().getUsername();

        // İlk aktivite veya lastActivity null ise
        if (lastActivity == null) {
            profile.setCurrentStreak(1);
            profile.setLastActivityDate(today);
            if (profile.getLongestStreak() < 1) {
                profile.setLongestStreak(1);
            }
            studentProfileRepository.save(profile);
            log.debug("First activity for user {}: streak set to 1", username);
            return true;
        }

        // Bugün zaten aktivite yapılmışsa streak değişmez
        if (lastActivity.equals(today)) {
            log.debug("User {} already had activity today, streak unchanged: {}",
                    username, profile.getCurrentStreak());
            return false;
        }

        // Dün aktivite yapılmışsa streak artır
        LocalDate yesterday = today.minusDays(1);
        if (lastActivity.equals(yesterday)) {
            int newStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(newStreak);
            profile.setLastActivityDate(today);

            if (newStreak > profile.getLongestStreak()) {
                profile.setLongestStreak(newStreak);
            }

            studentProfileRepository.save(profile);
            log.info("User {} streak increased: {} -> {}",
                    username, profile.getCurrentStreak() - 1, newStreak);
            return true;
        }

        // Dünden önce aktivite yapılmışsa streak reset
        profile.setCurrentStreak(1);
        profile.setLastActivityDate(today);
        if (profile.getLongestStreak() < 1) {
            profile.setLongestStreak(1);
        }
        studentProfileRepository.save(profile);
        log.info("User {} streak reset to 1 (last activity: {})",
                username, lastActivity);
        return true;
    }

}
