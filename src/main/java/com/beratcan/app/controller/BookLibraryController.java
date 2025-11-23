package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.service.UserLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookLibraryController {

    private final UserLibraryService userLibraryService;

    @GetMapping("/for-user/{userId}")
    public List<BookResponse> getBooksForUser(@PathVariable Long userId) {
        return userLibraryService.getBooksForUser(userId);
    }

    @PostMapping("/use-code")
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Too many code attempts. Please try again later.")
    public ResponseEntity<?> useCode(
            @RequestParam String code,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Kod kullanmak için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        String result = userLibraryService.useCode(userId, code);
        return ResponseEntity.ok().body(Map.of("message", result));
    }

    @PostMapping("/add-free-book/{bookId}")
    public ResponseEntity<?> addFreeBookToLibrary(
            @PathVariable Long bookId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Ücretsiz kitap eklemek için giriş yapmalısınız."));
        }
        Long userId = Long.parseLong(authentication.getName());
        String result = userLibraryService.addFreeBookToLibrary(userId, bookId);
        return ResponseEntity.ok().body(Map.of("message", result));
    }
}
