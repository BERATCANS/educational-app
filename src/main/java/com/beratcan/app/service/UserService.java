package com.beratcan.app.service;

import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.enums.*;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final QuestionRepository questionRepository;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,13}$");

    public UserService(UserRepository userRepository,
            QuestionRepository questionRepository,
            BCryptPasswordEncoder passwordEncoder) { // ✅ Inject edildi
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; // ✅ new yapma
        this.questionRepository = questionRepository;
    }

    public User registerUser(UserRequest dto) {
        log.info("User registration started: username={}, email={}, role={}",
                dto.getUsername(), dto.getEmail(), dto.getRole());

        // Telefon doğrulama
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            log.warn("Invalid phone number format: username={}, phone={}", dto.getUsername(), dto.getPhoneNumber());
            throw new IllegalArgumentException("Geçersiz telefon numarası.");
        }

        // Benzersizlik kontrolleri
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed - email already exists: email={}", dto.getEmail());
            throw new IllegalArgumentException("Bu email zaten kayıtlı.");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            log.warn("Registration failed - phone number already exists: phone={}", dto.getPhoneNumber());
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.warn("Registration failed - username already exists: username={}", dto.getUsername());
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kayıtlı.");
        }

        // User oluştur
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // şifreyi hashle
        user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        // ExamType (zorunlu değil ama eğer gelmişse işleme al)
        if (dto.getExamType() != null) {
            try {
                user.setExamType(ExamType.valueOf(dto.getExamType().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Geçersiz sınav türü.");
            }

            // GradeLevel (sınav seçildiyse ekle)
            if (dto.getGradeLevel() != null) {
                try {
                    user.setGradeLevel(GradeLevel.valueOf(dto.getGradeLevel().toUpperCase()));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Geçersiz sınıf seviyesi.");
                }
            }

            // YKS ise fieldType ve highSchoolType zorunlu
            if ("YKS".equalsIgnoreCase(dto.getExamType())) {
                if (dto.getFieldType() == null || dto.getSchoolType() == null) {
                    throw new IllegalArgumentException("YKS için alan ve lise türü zorunludur.");
                }
                try {
                    user.setFieldType(FieldType.valueOf(dto.getFieldType().toUpperCase()));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Geçersiz alan türü.");
                }
                user.setSchoolType(SchoolType.valueOf(dto.getSchoolType()));
            }
        }

        user.setGender(dto.getGender() != null ? Gender.valueOf(dto.getGender()) : Gender.DIGER);
        user.setCity(dto.getCity() != null ? City.valueOf(dto.getCity()) : City.Diğer);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}, username={}, email={}, role={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());

        return savedUser;
    }

    public User login(LoginRequest request, JwtUtil jwtUtil) {
        log.info("Login attempt: identifier={}", request.getIdentifier());

        Optional<User> userOpt;

        if (request.getIdentifier().contains("@")) {
            userOpt = userRepository.findByEmail(request.getIdentifier());
        } else {
            userOpt = userRepository.findByUsername(request.getIdentifier());
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("Login failed - user not found: identifier={}", request.getIdentifier());
            return new IllegalArgumentException("Kullanıcı bulunamadı.");
        });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password: userId={}, username={}", user.getId(), user.getUsername());
            throw new IllegalArgumentException("Geçersiz şifre.");
        }

        // JWT token üret
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        log.info("Login successful: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());

        // Login başarılı → JWT döndür
        return user;
    }

    // Soru flagleme
    public void flagQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("User not found");
                    log.error("User not found for flagging question: userId={}, questionId={}", userId, questionId, ex);
                    return ex;
                });

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Question not found");
                    log.error("Question not found for flagging: questionId={}, userId={}", questionId, userId, ex);
                    return ex;
                });

        user.getFlaggedQuestions().add(question);
        userRepository.save(user);

        log.info("Question flagged: userId={}, questionId={}, totalFlagged={}",
                userId, questionId, user.getFlaggedQuestions().size());
    }

    // Flag kaldırma
    public void unflagQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("User not found");
                    log.error("User not found for unflagging question: userId={}, questionId={}", userId, questionId,
                            ex);
                    return ex;
                });

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Question not found");
                    log.error("Question not found for unflagging: questionId={}, userId={}", questionId, userId, ex);
                    return ex;
                });

        user.getFlaggedQuestions().remove(question);
        userRepository.save(user);

        log.info("Question unflagged: userId={}, questionId={}, totalFlagged={}",
                userId, questionId, user.getFlaggedQuestions().size());
    }

    // Kullanıcının flagged questions listesi
    public Set<Question> getFlaggedQuestions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("User not found");
                    log.error("User not found for getting flagged questions: userId={}", userId, ex);
                    return ex;
                });

        return user.getFlaggedQuestions();
    }

    // Kullanıcı bilgilerini getir
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("User not found");
                    log.error("User not found: userId={}", userId, ex);
                    return ex;
                });
    }

    // Question bilgisini getir (FlaggedResponse için)
    public Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    RuntimeException ex = new RuntimeException("Question not found");
                    log.error("Question not found: questionId={}", questionId, ex);
                    return ex;
                });
    }

}
