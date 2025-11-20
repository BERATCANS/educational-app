package com.beratcan.app.service;

import com.beratcan.app.dto.response.BookDetailResponse;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import com.beratcan.app.exception.BookNotFoundException;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookCatalogService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookReviewRepository bookReviewRepository;
    private final FilterLabelService filterLabelService;

    public PageResponse<BookStoreResponse> getBookStore(String query,
            List<ExamType> examTypes,
            List<GradeLevel> gradeLevels,
            List<LessonType> lessonTypes,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long userId, int page, int size) {
        // Kullanıcı bilgilerini al
        final Set<Long> ownedBookIds;
        ExamType userExamType = null;

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Sahip olunan kitaplar (Set kullanarak O(1) lookup)
            ownedBookIds = user.getOwnedBooks().stream()
                    .map(Book::getId)
                    .collect(java.util.stream.Collectors.toSet());

            // Kullanıcının examType'ı
            userExamType = user.getExamType();
        } else {
            ownedBookIds = new HashSet<>();
        }

        // Filtreleri normalize et
        List<ExamType> normalizedExamTypes = normalizeList(examTypes);
        List<GradeLevel> normalizedGradeLevels = normalizeList(gradeLevels);
        List<LessonType> normalizedLessonTypes = normalizeList(lessonTypes);

        // DB'den direkt sıralanmış kitapları pagination ile al
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage;

        if (userExamType != null) {
            // Kullanıcının examType'ına göre sıralama yap
            // Önce kullanıcının examType'ına uygun kitaplar, sonra diğerleri
            bookPage = bookRepository.searchAndFilterBooksWithPaginationAndSorting(
                    query,
                    normalizedExamTypes,
                    normalizedGradeLevels,
                    normalizedLessonTypes,
                    minPrice,
                    maxPrice,
                    userExamType,
                    pageable);
        } else {
            // Sıralama yok, normal pagination
            bookPage = bookRepository.searchAndFilterBooksWithPagination(
                    query,
                    normalizedExamTypes,
                    normalizedGradeLevels,
                    normalizedLessonTypes,
                    minPrice,
                    maxPrice,
                    pageable);
        }

        // DB'den gelen sıralamayı olduğu gibi kullan, direkt DTO'ya çevir
        List<BookStoreResponse> pageContent = bookPage.getContent().stream()
                .map(book -> toBookStoreResponse(book, ownedBookIds.contains(book.getId())))
                .toList();

        return PageResponse.of(pageContent, page, size, bookPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetail(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Kullanıcının kitabı sahip olup olmadığını kontrol et
        boolean isOwned = false;
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElse(null);
            if (user != null) {
                isOwned = user.getOwnedBooks().stream()
                        .anyMatch(ownedBook -> ownedBook.getId().equals(bookId));
            }
        }

        // Yorumları lazy fetch ile al (sadece bu metod çağrıldığında DB'den çekilir)
        // BookDetailResponse için tüm yorumları gösteriyoruz (pagination yok)
        List<BookReviewResponse> reviews = bookReviewRepository.findByBookIdOrderByCreatedAtDesc(bookId)
                .stream()
                .map(this::toBookReviewResponse)
                .toList();

        BookDetailResponse response = new BookDetailResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setPublisher(book.getPublisher());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setPaid(book.isPaid());
        response.setNumberOfTopics(book.getNumberOfTopics());
        response.setNumberOfTests(book.getNumberOfTests());
        response.setNumberOfQuestions(book.getNumberOfQuestions());
        response.setGradeLevelLabel(book.getGradeLevel() != null
                ? filterLabelService.getGradeLabel(book.getGradeLevel())
                : null);
        response.setExamTypeLabel(book.getExamType() != null
                ? filterLabelService.getExamLabel(book.getExamType())
                : null);
        response.setLessonTypeLabel(book.getLessonType() != null
                ? filterLabelService.getLessonLabel(book.getLessonType())
                : null);
        response.setPrice(book.getPrice());
        response.setDescription(book.getDescription());
        response.setOwned(isOwned);
        response.setAverageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0);
        response.setReviewCount(book.getReviewCount() != null ? book.getReviewCount() : 0L);
        response.setReviews(reviews);

        return response;
    }

    private BookStoreResponse toBookStoreResponse(Book book, Boolean isOwned) {
        BookStoreResponse response = new BookStoreResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setPublisher(book.getPublisher());
        response.setPaid(book.isPaid());
        response.setPrice(book.getPrice());
        response.setOwned(isOwned != null && isOwned);
        response.setAverageRating(book.getAverageRating() != null ? book.getAverageRating() : 0.0);
        response.setReviewCount(book.getReviewCount() != null ? book.getReviewCount() : 0L);
        return response;
    }

    private BookReviewResponse toBookReviewResponse(com.beratcan.app.entity.BookReview review) {
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
                false); // isMyReview artık kullanılmıyor
    }

    private <T> List<T> normalizeList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }
}
