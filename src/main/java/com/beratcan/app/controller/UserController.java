package com.beratcan.app.controller;

import com.beratcan.app.dto.response.JwtResponse;
import com.beratcan.app.dto.response.UserResponse;
import com.beratcan.app.dto.response.FlaggedResponse;
import com.beratcan.app.dto.request.LoginRequest;
import com.beratcan.app.dto.request.UserRequest;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.City;
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
    public ResponseEntity<List<FlaggedResponse>> getFlaggedQuestions(@PathVariable Long userId) {
        Set<Question> flaggedQuestions = userService.getFlaggedQuestions(userId);
        List<FlaggedResponse> response = flaggedQuestions.stream()
                .map(question -> {
                    FlaggedResponse fr = new FlaggedResponse();
                    fr.setQuestionId(question.getId());
                    fr.setImageUrl(question.getImageUrl());
                    fr.setAnswerNumber(question.getAnswerNumber());
                    fr.setCorrectAnswer(question.getCorrectAnswer().name());
                    fr.setVideoUrl(question.getVideoUrl());

                    // Hints ve explanation
                    fr.setHints(question.getHints() != null
                            ? question.getHints().stream().map(hint -> hint.getText()).toList()
                            : List.of());
                    fr.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");

                    // Navigation ve display bilgileri
                    if (question.getTest() != null) {
                        fr.setTestId(question.getTest().getId());
                        fr.setTestTitle(question.getTest().getTitle());

                        if (question.getTest().getTopic() != null) {
                            fr.setTopicId(question.getTest().getTopic().getId());
                            fr.setTopicTitle(question.getTest().getTopic().getTitle());

                            if (question.getTest().getTopic().getBook() != null) {
                                fr.setBookId(question.getTest().getTopic().getBook().getId());
                                fr.setBookTitle(question.getTest().getTopic().getBook().getTitle());
                            }
                        }
                    }

                    return fr;
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    // Tek soru için flagged bilgileri - navigation için book, topic, test ID'leri
    // dahil
    @GetMapping("/flagged/{questionId}")
    public ResponseEntity<FlaggedResponse> getFlaggedQuestionById(@PathVariable Long questionId) {
        try {
            // Question'ı direkt repository'den al
            Question question = userService.getQuestionById(questionId);

            if (question == null) {
                return ResponseEntity.notFound().build();
            }

            FlaggedResponse fr = new FlaggedResponse();
            fr.setQuestionId(question.getId());
            fr.setImageUrl(question.getImageUrl());
            fr.setAnswerNumber(question.getAnswerNumber());
            fr.setCorrectAnswer(question.getCorrectAnswer().name());
            fr.setVideoUrl(question.getVideoUrl());

            fr.setHints(question.getHints() != null ? question.getHints().stream().map(hint -> hint.getText()).toList()
                    : List.of());
            fr.setExplanation(question.getExplanation() != null ? question.getExplanation().getText() : "");

            if (question.getTest() != null) {
                fr.setTestId(question.getTest().getId());
                fr.setTestTitle(question.getTest().getTitle());

                if (question.getTest().getTopic() != null) {
                    fr.setTopicId(question.getTest().getTopic().getId());
                    fr.setTopicTitle(question.getTest().getTopic().getTitle());

                    if (question.getTest().getTopic().getBook() != null) {
                        fr.setBookId(question.getTest().getTopic().getBook().getId());
                        fr.setBookTitle(question.getTest().getTopic().getBook().getTitle());
                    }
                }
            }

            return ResponseEntity.ok(fr);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Kullanıcı profile bilgileri
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping("/cities")
    public City[] getCities() {
        return City.values();
    }
}
