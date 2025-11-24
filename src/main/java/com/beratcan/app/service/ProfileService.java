package com.beratcan.app.service;

import com.beratcan.app.dto.request.UpdateAcademicInfoRequest;
import com.beratcan.app.dto.request.UpdatePasswordRequest;
import com.beratcan.app.dto.request.UpdateProfileRequest;
import com.beratcan.app.dto.request.UpdateQuestionTargetRequest;
import com.beratcan.app.dto.response.QuestionTargetResponse;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.FieldType;
import com.beratcan.app.entity.enums.Gender;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.SchoolType;
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

        if (StringUtils.hasText(request.getExamType())) {
            ExamType examType = parseEnum(ExamType.class, request.getExamType(), "examType");
            user.setExamType(examType);
        } else {
            user.setExamType(null);
        }

        if (StringUtils.hasText(request.getGradeLevel())) {
            GradeLevel gradeLevel = parseEnum(GradeLevel.class, request.getGradeLevel(), "gradeLevel");
            user.setGradeLevel(gradeLevel);
        } else {
            user.setGradeLevel(null);
        }

        if (user.getExamType() == ExamType.YKS) {
            if (!StringUtils.hasText(request.getFieldType()) || !StringUtils.hasText(request.getSchoolType())) {
                throw new BadRequestException("YKS için alan ve okul türü zorunludur");
            }
            FieldType fieldType = parseEnum(FieldType.class, request.getFieldType(), "fieldType");
            SchoolType schoolType = parseEnum(SchoolType.class, request.getSchoolType(), "schoolType");
            user.setFieldType(fieldType);
            user.setSchoolType(schoolType);
        } else {
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

        if (StringUtils.hasText(request.getGender())) {
            user.setGender(parseEnum(Gender.class, request.getGender(), "gender"));
            updated = true;
        }

        if (StringUtils.hasText(request.getCity())) {
            user.setCity(parseEnum(City.class, request.getCity(), "city"));
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

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Geçersiz " + fieldName + " değeri: " + value);
        }
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
}
