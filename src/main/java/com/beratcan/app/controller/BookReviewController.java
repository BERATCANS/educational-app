package com.beratcan.app.controller;

import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookReviewsPageResponse;
import com.beratcan.app.service.BookReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookReviewController {

    private final BookReviewService bookReviewService;

    @PostMapping("/{bookId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long bookId,
            @RequestBody BookReviewRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Yorum eklemek için giriş yapmalısınız."));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            String message = bookReviewService.addReview(userId, bookId, request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long bookId,
            @PathVariable Long reviewId,
            @RequestBody BookReviewRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Yorum düzenlemek için giriş yapmalısınız."));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            String message = bookReviewService.updateReview(userId, bookId, reviewId, request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{bookId}/reviews")
    public BookReviewsPageResponse getBookReviews(
            @PathVariable Long bookId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                userId = Long.parseLong(authentication.getName());
            } catch (Exception e) {
                // Kullanıcı ID'si parse edilemezse null bırak
            }
        }
        return bookReviewService.getBookReviews(bookId, userId, page, size);
    }

    @DeleteMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long bookId,
            @PathVariable Long reviewId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Yorum silmek için giriş yapmalısınız."));
        }

        Long userId = Long.parseLong(authentication.getName());
        String message = bookReviewService.deleteReview(userId, bookId, reviewId);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
