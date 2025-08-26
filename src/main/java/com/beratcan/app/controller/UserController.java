package com.beratcan.app.controller;

import com.beratcan.app.dto.JwtResponse;
import com.beratcan.app.dto.LoginRequest;
import com.beratcan.app.dto.UserDTO;
import com.beratcan.app.entity.Question;
import com.beratcan.app.entity.User;
import com.beratcan.app.service.UserService;
import com.beratcan.app.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        try {
            User user = userService.registerUser(userDTO);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request, jwtUtil);
            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new JwtResponse(token, user.getUsername(), user.getRole().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<String> flagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        userService.flagQuestion(userId, questionId);
        return ResponseEntity.ok("Question flagged successfully.");
    }

    // Flag kaldırma
    @DeleteMapping("/{userId}/flag/{questionId}")
    public ResponseEntity<String> unflagQuestion(@PathVariable Long userId, @PathVariable Long questionId) {
        userService.unflagQuestion(userId, questionId);
        return ResponseEntity.ok("Question unflagged successfully.");
    }

    // Kullanıcının çözemediği sorular
    @GetMapping("/{userId}/flagged")
    public ResponseEntity<Set<Question>> getFlaggedQuestions(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getFlaggedQuestions(userId));
    }

}
