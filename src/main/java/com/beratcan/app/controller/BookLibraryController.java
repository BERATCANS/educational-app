package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.service.UserLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
public class BookLibraryController {

    private final UserLibraryService userLibraryService;

    @GetMapping("/for-user")
    public List<BookResponse> getBooksForUser(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return userLibraryService.getBooksForUser(userId);
    }

    @PostMapping("/use-code")
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Too many code attempts. Please try again later.")
    public ResponseEntity<?> useCode(
            @RequestParam String code,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        String result = userLibraryService.useCode(userId, code);
        return ResponseEntity.ok().body(Map.of("message", result));
    }

    @PostMapping("/add-free-book/{bookId}")
    public ResponseEntity<?> addFreeBookToLibrary(
            @PathVariable Long bookId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        String result = userLibraryService.addFreeBookToLibrary(userId, bookId);
        return ResponseEntity.ok().body(Map.of("message", result));
    }
}
