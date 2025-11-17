package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.BookReviewsPageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookReview;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.Role;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.BookReviewRepository;
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

    @Transactional
    public String addReview(Long userId, Long bookId, BookReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yorum bilgisi eksik.");
        }

        int rating = request.getRating();
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Puan 1 ile 5 arasında olmalıdır.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Kontrol 1: Kullanıcı bu kitaba sahip mi?
        boolean ownsBook = user.getOwnedBooks().stream()
                .anyMatch(ownedBook -> ownedBook.getId().equals(bookId));

        if (!ownsBook) {
            throw new IllegalArgumentException("Sadece sahip olduğunuz kitaplar için yorum yapabilirsiniz.");
        }

        // Kontrol 2: Bu kullanıcının bu kitap için yorumu var mı?
        if (bookReviewRepository.findByBookIdAndUserId(bookId, userId).isPresent()) {
            throw new IllegalArgumentException(
                    "Bu kitap için zaten bir yorumunuz var. Lütfen mevcut yorumunuzu düzenleyin.");
        }

        LocalDateTime now = LocalDateTime.now();
        BookReview review = new BookReview();
        review.setBook(book);
        review.setUser(user);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        review.setRating(rating);
        review.setComment(request.getComment());

        bookReviewRepository.save(review);

        // Rating istatistiklerini güncelle
        updateBookRatingStats(book);

        return "Yorum kaydedildi.";
    }

    @Transactional
    public String updateReview(Long userId, Long bookId, Long reviewId, BookReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yorum bilgisi eksik.");
        }

        int rating = request.getRating();
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Puan 1 ile 5 arasında olmalıdır.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        if (!review.getBook().getId().equals(bookId)) {
            throw new IllegalArgumentException("Bu yorum belirtilen kitaba ait değil.");
        }

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Yalnızca kendi yorumunuzu düzenleyebilirsiniz.");
        }

        review.setRating(rating);
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        bookReviewRepository.save(review);

        // Rating istatistiklerini güncelle
        updateBookRatingStats(review.getBook());

        return "Yorum güncellendi.";
    }

    @Transactional(readOnly = true)
    public BookReviewsPageResponse getBookReviews(Long bookId, Long currentUserId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw new IllegalArgumentException("Kitap bulunamadı: " + bookId);
        }

        // Kullanıcının kendi yorumunu bul (sadece ilk sayfada göstermek için)
        BookReviewResponse ownedReview = null;
        if (currentUserId != null && page == 0) {
            Optional<BookReview> ownedReviewOpt = bookReviewRepository.findByBookIdAndUserId(bookId, currentUserId);
            if (ownedReviewOpt.isPresent()) {
                ownedReview = toBookReviewResponse(ownedReviewOpt.get());
            }
        }

        // Repository'den direkt pagination ile yorumları al (kullanıcının yorumunu
        // hariç tutarak)
        Pageable pageable = PageRequest.of(page, size);
        Page<BookReview> reviewPage = bookReviewRepository.findByBookIdExcludingUserId(
                bookId, currentUserId, pageable);

        // Response'a çevir
        List<BookReviewResponse> pageContent = reviewPage.getContent().stream()
                .map(this::toBookReviewResponse)
                .toList();

        // Sadece ilk sayfada (page == 0) owned review göster
        BookReviewResponse ownedReviewToShow = (page == 0) ? ownedReview : null;

        return new BookReviewsPageResponse(
                ownedReviewToShow,
                pageContent,
                page,
                size,
                reviewPage.getTotalElements());
    }

    /**
     * Yorum silme işlemi
     * Sadece yorum sahibi veya admin silebilir
     */
    @Transactional
    public String deleteReview(Long userId, Long bookId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        // Yorumun bu kitaba ait olduğunu kontrol et
        if (!review.getBook().getId().equals(bookId)) {
            throw new IllegalArgumentException("Bu yorum bu kitaba ait değil");
        }

        // Kullanıcı yorum sahibi mi veya admin mi kontrol et
        boolean isOwner = review.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Bu yorumu silemezsiniz. Sadece yorum sahibi veya admin silebilir.");
        }

        // Yorumu sil
        bookReviewRepository.delete(review);

        // Rating istatistiklerini güncelle
        updateBookRatingStats(book);

        return "Yorum başarıyla silindi.";
    }

    private BookReviewResponse toBookReviewResponse(BookReview review) {
        User reviewUser = review.getUser();
        String userName = reviewUser != null
                ? (reviewUser.getFirstName() + " " + reviewUser.getLastName()).trim()
                : "Anonim";

        return new BookReviewResponse(
                review.getId(),
                reviewUser != null ? reviewUser.getId() : null,
                userName,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                false); // isMyReview artık kullanılmıyor, false olarak bırakıyoruz
    }

    /**
     * Kitabın rating istatistiklerini hesaplar ve Book entity'sine kaydeder
     * Bu metod yorum eklendiğinde/güncellendiğinde çağrılır
     */
    private void updateBookRatingStats(Book book) {
        List<BookReview> reviews = bookReviewRepository.findByBookIdOrderByCreatedAtDesc(book.getId());

        if (reviews == null || reviews.isEmpty()) {
            book.setAverageRating(0.0);
            book.setReviewCount(0L);
        } else {
            double totalRating = reviews.stream()
                    .mapToInt(BookReview::getRating)
                    .sum();
            double average = totalRating / reviews.size();
            book.setAverageRating(Math.round(average * 10.0) / 10.0); // 1 ondalık basamak
            book.setReviewCount((long) reviews.size());
        }

        bookRepository.save(book);
    }
}
