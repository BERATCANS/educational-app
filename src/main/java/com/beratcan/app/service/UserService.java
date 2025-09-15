package com.beratcan.app.service;

import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.Role;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.QuestionRepository;
import com.beratcan.app.repository.UserRepository;
import com.beratcan.app.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final QuestionRepository questionRepository;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,13}$");

    public UserService(UserRepository userRepository, QuestionRepository questionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.questionRepository = questionRepository;
    }

    public User registerUser(UserRequest dto) {
        // Telefon doğrulama
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw new IllegalArgumentException("Geçersiz telefon numarası.");
        }

        // Benzersizlik kontrolleri
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Bu email zaten kayıtlı.");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
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

        return userRepository.save(user);
    }

    public User login(LoginRequest request, JwtUtil jwtUtil) {
        Optional<User> userOpt;

        if (request.getIdentifier().contains("@")) {
            userOpt = userRepository.findByEmail(request.getIdentifier());
        } else if (request.getIdentifier().matches("^\\+?[0-9]{10,13}$")) {
            userOpt = userRepository.findByPhoneNumber(request.getIdentifier());
        } else {
            userOpt = userRepository.findByUsername(request.getIdentifier());
        }

        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Geçersiz şifre.");
        }

        // JWT token üret
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        // Login başarılı → JWT döndür
        return user;
    }

    // Soru flagleme
    public void flagQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        user.getFlaggedQuestions().add(question);
        userRepository.save(user);
    }

    // Flag kaldırma
    public void unflagQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        user.getFlaggedQuestions().remove(question);
        userRepository.save(user);
    }

    // Kullanıcının flagged questions listesi
    public Set<Question> getFlaggedQuestions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getFlaggedQuestions();
    }

    // Kullanıcı bilgilerini getir
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
