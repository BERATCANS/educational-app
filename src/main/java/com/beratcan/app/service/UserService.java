package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.enums.*;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public static final Pattern PHONE_PATTERN = Pattern.compile(AppConstants.PHONE_NUMBER_PATTERN);

    public UserService(UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public User registerUser(UserRequest dto) {
        if (dto == null) {
            throw new BadRequestException("User request cannot be null");
        }

        log.info("User registration started: username={}, email={}, role={}",
                dto.getUsername(), dto.getEmail(), dto.getRole());

        // Temel alan kontrolleri
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email boş olamaz");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Kullanıcı adı boş olamaz");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Şifre boş olamaz");
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            throw new BadRequestException("Telefon numarası boş olamaz");
        }

        // Email format doğrulama ve normalize et
        String email = dto.getEmail().trim().toLowerCase();
        if (!isValidEmailFormat(email)) {
            log.warn("Invalid email format: email={}", email);
            throw new BadRequestException("Geçersiz email formatı. Lütfen geçerli bir email adresi giriniz.");
        }

        // Telefon doğrulama
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            log.warn("Invalid phone number format: username={}, phone={}", dto.getUsername(), dto.getPhoneNumber());
            throw new BadRequestException("Geçersiz telefon numarası formatı.");
        }

        // Benzersizlik kontrolleri
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed - email already exists: email={}", dto.getEmail());
            throw new ConflictException("Bu email zaten kayıtlı.");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            log.warn("Registration failed - phone number already exists: phone={}", dto.getPhoneNumber());
            throw new ConflictException("Bu telefon numarası zaten kayıtlı.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.warn("Registration failed - username already exists: username={}", dto.getUsername());
            throw new ConflictException("Bu kullanıcı adı zaten kayıtlı.");
        }

        // User oluştur
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(email); // Lowercase ve trim edilmiş email
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // şifreyi hashle
        user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        // ExamType (zorunlu değil ama eğer gelmişse işleme al)
        if (dto.getExamType() != null) {
            try {
                user.setExamType(ExamType.valueOf(dto.getExamType().toUpperCase()));
            } catch (Exception e) {
                throw new BadRequestException("Geçersiz sınav türü.");
            }

            // GradeLevel (sınav seçildiyse ekle)
            if (dto.getGradeLevel() != null) {
                try {
                    user.setGradeLevel(GradeLevel.valueOf(dto.getGradeLevel().toUpperCase()));
                } catch (Exception e) {
                    throw new BadRequestException("Geçersiz sınıf seviyesi.");
                }
            }

            // YKS ise fieldType ve highSchoolType zorunlu
            if ("YKS".equalsIgnoreCase(dto.getExamType())) {
                if (dto.getFieldType() == null || dto.getSchoolType() == null) {
                    throw new BadRequestException("YKS için alan ve lise türü zorunludur.");
                }
                try {
                    user.setFieldType(FieldType.valueOf(dto.getFieldType().toUpperCase()));
                } catch (Exception e) {
                    throw new BadRequestException("Geçersiz alan türü.");
                }
                try {
                    user.setSchoolType(SchoolType.valueOf(dto.getSchoolType()));
                } catch (Exception e) {
                    throw new BadRequestException("Geçersiz lise türü.");
                }
            }
        }

        user.setGender(dto.getGender() != null ? Gender.valueOf(dto.getGender()) : Gender.DIGER);
        user.setCity(dto.getCity() != null ? City.valueOf(dto.getCity()) : City.Diğer);

        // Email doğrulama - başlangıçta false
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}, username={}, email={}, role={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());

        // Email doğrulama email'i gönder
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
            log.info("Verification email sent to new user: userId={}, email={}",
                    savedUser.getId(), savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email during registration: userId={}, email={}",
                    savedUser.getId(), savedUser.getEmail(), e);
            // Email gönderilemese bile kullanıcı kaydedildi, sonra tekrar isteyebilir
        }

        return savedUser;
    }

    /**
     * Kullanıcı kimlik bilgilerini doğrular (email/username + password)
     * 
     * @param request Login request (identifier, password)
     * @return User entity (doğrulama başarılıysa)
     * @throws BadRequestException   Şifre yanlışsa
     * @throws UserNotFoundException Kullanıcı bulunamazsa
     */
    public User validateUserCredentials(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Login request cannot be null");
        }
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new BadRequestException("Identifier (username or email) cannot be null or empty");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password cannot be null or empty");
        }

        log.info("Login attempt: identifier={}", request.getIdentifier());

        Optional<User> userOpt;

        if (request.getIdentifier().contains("@")) {
            userOpt = userRepository.findByEmail(request.getIdentifier());
        } else {
            userOpt = userRepository.findByUsername(request.getIdentifier());
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("Login failed - user not found: identifier={}", request.getIdentifier());
            return new UserNotFoundException("Kullanıcı bulunamadı.");
        });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password: userId={}, username={}", user.getId(), user.getUsername());
            throw new BadRequestException("Geçersiz şifre.");
        }

        log.info("Login successful: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());

        // Login başarılı → User döndür (token controller'da oluşturulacak)
        return user;
    }

    // Kullanıcı bilgilerini getir
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Email formatını doğrula
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Temel format kontrolü
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return false;
        }
        // Email çok uzun olmamalı
        if (email.length() > 100) {
            return false;
        }
        // @ işaretinden önce ve sonra karakter olmalı
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }
        // Domain kısmında en az bir nokta olmalı
        String domain = email.substring(atIndex + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }
        return true;
    }
}
