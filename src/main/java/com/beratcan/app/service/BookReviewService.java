package com.beratcan.app.service;

import com.beratcan.app.constants.AppConstants;
import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookReview;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.Role;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.ReviewNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.BookReviewRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional
    public String addReview(Long userId, Long bookId, BookReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Yorum bilgisi eksik.");
        }

        int overallRating = request.getOverallRating();
        int contentQualityRating = request.getContentQualityRating();
        int difficultyRating = request.getDifficultyRating();
        int videoQualityRating = request.getVideoQualityRating();

        if (overallRating < AppConstants.MIN_RATING || overallRating > AppConstants.MAX_RATING ||
            contentQualityRating < AppConstants.MIN_RATING || contentQualityRating > AppConstants.MAX_RATING ||
            difficultyRating < AppConstants.MIN_RATING || difficultyRating > AppConstants.MAX_RATING ||
            videoQualityRating < AppConstants.MIN_RATING || videoQualityRating > AppConstants.MAX_RATING) {
            throw new BadRequestException(
                    "Puanlar " + AppConstants.MIN_RATING + " ile " + AppConstants.MAX_RATING + " arasında olmalıdır.");
        }

        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Öğrenci profili bulunamadı. userId=" + userId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Kullanıcı bulunamadı. userId=" + userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Kontrol 1: Öğrenci bu kitaba sahip mi?
        boolean ownsBook = user.getOwnedBooks().stream()
                .anyMatch(ownedBook -> ownedBook.getId().equals(bookId));

        if (!ownsBook) {
            throw new UnauthorizedOperationException("Sadece sahip olduğunuz kitaplar için yorum yapabilirsiniz.");
        }

        // Kontrol 2: Bu öğrencinin bu kitap için yorumu var mı?
        if (bookReviewRepository.findByBookIdAndStudentProfileId(bookId, profile.getId()).isPresent()) {
            throw new ConflictException(
                    "Bu kitap için zaten bir yorumunuz var. Lütfen mevcut yorumunuzu düzenleyin.");
        }

        LocalDateTime now = LocalDateTime.now();
        BookReview review = new BookReview();
        review.setBook(book);
        review.setStudentProfile(profile);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        review.setOverallRating(overallRating);
        review.setContentQualityRating(contentQualityRating);
        review.setDifficultyRating(difficultyRating);
        review.setVideoQualityRating(videoQualityRating);

        bookReviewRepository.save(review);

        updateBookRatingStats(book);

        return "Yorum kaydedildi.";
    }

    @Transactional
    public String updateReview(Long userId, Long bookId, Long reviewId, BookReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Yorum bilgisi eksik.");
        }

        int overallRating = request.getOverallRating();
        int contentQualityRating = request.getContentQualityRating();
        int difficultyRating = request.getDifficultyRating();
        int videoQualityRating = request.getVideoQualityRating();

        if (overallRating < AppConstants.MIN_RATING || overallRating > AppConstants.MAX_RATING ||
            contentQualityRating < AppConstants.MIN_RATING || contentQualityRating > AppConstants.MAX_RATING ||
            difficultyRating < AppConstants.MIN_RATING || difficultyRating > AppConstants.MAX_RATING ||
            videoQualityRating < AppConstants.MIN_RATING || videoQualityRating > AppConstants.MAX_RATING) {
            throw new BadRequestException(
                    "Puanlar " + AppConstants.MIN_RATING + " ile " + AppConstants.MAX_RATING + " arasında olmalıdır.");
        }

        BookReview review = bookReviewRepository.findByIdWithStudentProfileAndBook(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getBook().getId().equals(bookId)) {
            throw new BadRequestException("Bu yorum belirtilen kitaba ait değil.");
        }

        if (!review.getStudentProfile().getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("Yalnızca kendi yorumunuzu düzenleyebilirsiniz.");
        }

        review.setOverallRating(overallRating);
        review.setContentQualityRating(contentQualityRating);
        review.setDifficultyRating(difficultyRating);
        review.setVideoQualityRating(videoQualityRating);
        review.setUpdatedAt(LocalDateTime.now());

        bookReviewRepository.save(review);

        updateBookRatingStats(review.getBook());

        return "Yorum güncellendi.";
    }

    @Transactional(readOnly = true)
    public PageResponse<BookReviewResponse> getBookReviews(Long bookId, Long currentUserId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<BookReview> reviewPage = bookReviewRepository.findByBookIdExcludingUserId(
                bookId, currentUserId, pageable);

        List<BookReviewResponse> pageContent = reviewPage.getContent().stream()
                .map(this::toBookReviewResponse)
                .toList();

        return PageResponse.of(pageContent, page, size, reviewPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookReviewResponse getMyReview(Long bookId, Long userId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        if (userId == null) {
            throw new BadRequestException("Kullanıcı ID gerekli.");
        }

        Optional<BookReview> reviewOpt = bookReviewRepository
                .findByBookIdAndUserIdWithStudentProfileAndBook(bookId, userId);

        if (reviewOpt.isEmpty()) {
            return null;
        }

        return toBookReviewResponse(reviewOpt.get());
    }

    @Transactional
    public String deleteReview(Long userId, Long bookId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        BookReview review = bookReviewRepository.findByIdWithStudentProfileAndBook(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getBook().getId().equals(bookId)) {
            throw new BadRequestException("Bu yorum bu kitaba ait değil");
        }

        boolean isOwner = review.getStudentProfile().getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException(
                    "Bu yorumu silemezsiniz. Sadece yorum sahibi veya admin silebilir.");
        }

        bookReviewRepository.delete(review);

        updateBookRatingStats(book);

        return "Yorum başarıyla silindi.";
    }

    private BookReviewResponse toBookReviewResponse(BookReview review) {
        User reviewUser = review.getStudentProfile() != null ? review.getStudentProfile().getUser() : null;
        String userName = reviewUser != null
                ? (reviewUser.getFirstName() + " " + reviewUser.getLastName()).trim()
                : "Anonim";

        return new BookReviewResponse(
                review.getId(),
                reviewUser != null ? reviewUser.getId() : null,
                userName,
                review.getOverallRating(),
                review.getContentQualityRating(),
                review.getDifficultyRating(),
                review.getVideoQualityRating(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                false);
    }

    private void updateBookRatingStats(Book book) {
        List<BookReview> reviews = bookReviewRepository.findByBookIdOrderByCreatedAtDesc(book.getId());

        if (reviews == null || reviews.isEmpty()) {
            book.setAverageOverallRating(0.0);
            book.setAverageContentQualityRating(0.0);
            book.setAverageDifficultyRating(0.0);
            book.setAverageVideoQualityRating(0.0);
            book.setReviewCount(0L);
        } else {
            double totalOverall = 0;
            double totalContent = 0;
            double totalDifficulty = 0;
            double totalVideo = 0;

            for (BookReview r : reviews) {
                totalOverall += r.getOverallRating();
                totalContent += r.getContentQualityRating();
                totalDifficulty += r.getDifficultyRating();
                totalVideo += r.getVideoQualityRating();
            }

            int s = reviews.size();
            book.setAverageOverallRating(Math.round((totalOverall / s) * AppConstants.RATING_DECIMAL_MULTIPLIER) / (double) AppConstants.RATING_DECIMAL_MULTIPLIER);
            book.setAverageContentQualityRating(Math.round((totalContent / s) * AppConstants.RATING_DECIMAL_MULTIPLIER) / (double) AppConstants.RATING_DECIMAL_MULTIPLIER);
            book.setAverageDifficultyRating(Math.round((totalDifficulty / s) * AppConstants.RATING_DECIMAL_MULTIPLIER) / (double) AppConstants.RATING_DECIMAL_MULTIPLIER);
            book.setAverageVideoQualityRating(Math.round((totalVideo / s) * AppConstants.RATING_DECIMAL_MULTIPLIER) / (double) AppConstants.RATING_DECIMAL_MULTIPLIER);
            book.setReviewCount((long) reviews.size());
        }

        bookRepository.save(book);
    }
}
