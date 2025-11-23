package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.PageResponse;
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
    @RateLimit(requestsPerMinute = 5, keyType = "USER", message = "Too many review submissions. Please try again later.")
    public ResponseEntity<?> addReview(
            @PathVariable Long bookId,
            @RequestBody BookReviewRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Yorum eklemek için giriş yapmalısınız."));
        }

        Long userId = Long.parseLong(authentication.getName());
        String message = bookReviewService.addReview(userId, bookId, request);
        return ResponseEntity.ok(Map.of("message", message));
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

        Long userId = Long.parseLong(authentication.getName());
        String message = bookReviewService.updateReview(userId, bookId, reviewId, request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/{bookId}/reviews")
    public PageResponse<BookReviewResponse> getBookReviews(
            @PathVariable Long bookId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstants.REVIEW_PAGE_SIZE) int size) {
        Long userId = (authentication != null && authentication.isAuthenticated())
                ? Long.parseLong(authentication.getName())
                : null;
        return bookReviewService.getBookReviews(bookId, userId, page, size);
    }

    /**
     * Kullanıcının kendi yorumunu getir (varsa)
     * GET /api/books/{bookId}/reviews/my
     */
    @GetMapping("/{bookId}/reviews/my")
    public ResponseEntity<?> getMyReview(
            @PathVariable Long bookId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Yorumu görmek için giriş yapmalısınız."));
        }

        Long userId = Long.parseLong(authentication.getName());
        BookReviewResponse myReview = bookReviewService.getMyReview(bookId, userId);

        if (myReview == null) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(myReview);
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
