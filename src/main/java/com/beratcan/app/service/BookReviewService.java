package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.BookReviewsPageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookReview;
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
            throw new BadRequestException("Yorum bilgisi eksik.");
        }

        int rating = request.getRating();
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Puan 1 ile 5 arasında olmalıdır.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Kontrol 1: Kullanıcı bu kitaba sahip mi?
        boolean ownsBook = user.getOwnedBooks().stream()
                .anyMatch(ownedBook -> ownedBook.getId().equals(bookId));

        if (!ownsBook) {
            throw new UnauthorizedOperationException("Sadece sahip olduğunuz kitaplar için yorum yapabilirsiniz.");
        }

        // Kontrol 2: Bu kullanıcının bu kitap için yorumu var mı?
        if (bookReviewRepository.findByBookIdAndUserId(bookId, userId).isPresent()) {
            throw new ConflictException(
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
            throw new BadRequestException("Yorum bilgisi eksik.");
        }

        int rating = request.getRating();
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Puan 1 ile 5 arasında olmalıdır.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getBook().getId().equals(bookId)) {
            throw new BadRequestException("Bu yorum belirtilen kitaba ait değil.");
        }

        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("Yalnızca kendi yorumunuzu düzenleyebilirsiniz.");
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
            throw new BookNotFoundException(bookId);
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

        return new BookReviewsPageResponse(
                pageContent,
                page,
                size,
                reviewPage.getTotalElements());
    }

    /**
     * Kullanıcının kendi yorumunu getir (varsa)
     * 
     * @param bookId Kitap ID
     * @param userId Kullanıcı ID
     * @return Kullanıcının yorumu (varsa), yoksa null
     */
    @Transactional(readOnly = true)
    public BookReviewResponse getMyReview(Long bookId, Long userId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        if (userId == null) {
            throw new BadRequestException("Kullanıcı ID gerekli.");
        }

        Optional<BookReview> reviewOpt = bookReviewRepository.findByBookIdAndUserId(bookId, userId);

        if (reviewOpt.isEmpty()) {
            return null; // Kullanıcının yorumu yok
        }

        return toBookReviewResponse(reviewOpt.get());
    }

    /**
     * Yorum silme işlemi
     * Sadece yorum sahibi veya admin silebilir
     */
    @Transactional
    public String deleteReview(Long userId, Long bookId, Long reviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // Yorumun bu kitaba ait olduğunu kontrol et
        if (!review.getBook().getId().equals(bookId)) {
            throw new BadRequestException("Bu yorum bu kitaba ait değil");
        }

        // Kullanıcı yorum sahibi mi veya admin mi kontrol et
        boolean isOwner = review.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException(
                    "Bu yorumu silemezsiniz. Sadece yorum sahibi veya admin silebilir.");
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
