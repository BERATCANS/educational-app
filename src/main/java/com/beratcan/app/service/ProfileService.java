package com.beratcan.app.service;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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

        // ExamType güncelle
        if (request.getExamType() != null) {
            user.setExamType(request.getExamType());
        } else {
            user.setExamType(null);
        }

        // GradeLevel güncelle
        if (request.getGradeLevel() != null) {
            user.setGradeLevel(request.getGradeLevel());
        } else {
            user.setGradeLevel(null);
        }

        // YKS için özel kontroller (validator zaten kontrol ediyor, burada sadece set
        // ediyoruz)
        if (user.getExamType() == ExamType.YKS) {
            // Validator zaten null kontrolü yaptı, burada direkt set edebiliriz
            user.setFieldType(request.getFieldType());
            user.setSchoolType(request.getSchoolType());
        } else {
            // YKS değilse (LGS, KPSS, DIGER) bu alanları temizle
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

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
                throw new ConflictException("Bu email başka bir kullanıcı tarafından kullanılıyor.");
            }
            user.setEmail(request.getEmail().trim());
            updated = true;
        }

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
