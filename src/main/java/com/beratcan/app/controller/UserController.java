package com.beratcan.app.controller;

import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.dto.response.QuestionResponse;
import com.beratcan.app.dto.response.TestResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.User;
import com.beratcan.app.service.UserService;
import com.beratcan.app.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        try {
            User user = userService.registerUser(userRequest);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request, jwtUtil);
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            return ResponseEntity.ok(new JwtResponse(token, user.getUsername(), user.getRole().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<?> flagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        try {
            userService.flagQuestion(userId, questionId);
            return ResponseEntity.ok(Map.of("message", "Question flagged successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Flag kaldırma
    @DeleteMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<?> unflagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        try {
            userService.unflagQuestion(userId, questionId);
            return ResponseEntity.ok(Map.of("message", "Question unflagged successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Kullanıcının çözemediği sorular
    @GetMapping("/{userId}/flagged")
    public ResponseEntity<List<QuestionResponse>> getFlaggedQuestions(@PathVariable Long userId) {
        Set<Question> flaggedQuestions = userService.getFlaggedQuestions(userId);
        List<QuestionResponse> response = flaggedQuestions.stream()
                .map(question -> {
                    QuestionResponse qr = new QuestionResponse();
                    qr.setId(question.getId());
                    qr.setImageUrl(question.getImageUrl());
                    qr.setAnswerNumber(question.getAnswerNumber());
                    qr.setCorrectAnswer(question.getCorrectAnswer());
                    qr.setVideoUrl(question.getVideoUrl());
                    
                    // Test bilgisini ekle
                    if (question.getTest() != null) {
                        TestResponse testResponse = new TestResponse();
                        testResponse.setId(question.getTest().getId());
                        testResponse.setTitle(question.getTest().getTitle());
                        qr.setTest(testResponse);
                    }
                    
                    // Hints ve explanation
                    qr.setHints(question.getHints() != null ? 
                        question.getHints().stream().map(hint -> hint.getText()).toList() : 
                        List.of());
                    qr.setExplanation(question.getExplanation() != null ? 
                        question.getExplanation().getText() : "");
                    
                    return qr;
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    // Kullanıcı profile bilgileri
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(new UserResponse(user));
    }
}
