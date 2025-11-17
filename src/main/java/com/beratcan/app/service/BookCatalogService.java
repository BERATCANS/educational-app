package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.response.BookDetailResponse;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.BookReviewRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookCatalogService {

    private final BookRepository bookRepository;
    private final BookCodeRepository bookCodeRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookRecommendationService bookRecommendationService;

    public Book addBook(BookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BookRequest cannot be null");
        }

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setDescription(request.getDescription());

        // paid false ise price'ı otomatik 0 yap
        if (!request.isPaid()) {
            book.setPrice(BigDecimal.ZERO);
        } else {
            book.setPrice(request.getPrice());
        }

        Book savedBook = bookRepository.save(book);

        List<String> topics = request.getTopics();
        if (topics != null && !topics.isEmpty()) {
            for (String topicName : topics) {
                Topic topic = new Topic();
                topic.setTitle(topicName);
                topic.setBook(savedBook);
                topic = topicRepository.save(topic);
                savedBook.getTopics().add(topic);
            }

            savedBook = bookRepository.save(savedBook);
        }

        countBookData(savedBook.getId());
        return savedBook;
    }

    public Book updateBook(Long bookId, BookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        book.setTitle(request.getTitle());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setPaid(request.isPaid());
        book.setGradeLevel(request.getGradeLevel());
        book.setExamType(request.getExamType());
        book.setLessonType(request.getLessonType());
        book.setPrice(request.getPrice());
        book.setDescription(request.getDescription());

        return bookRepository.save(book);
    }

    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found");
        }
        bookRepository.deleteById(bookId);
    }

    @Transactional
    public void countBookData(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Kitap bulunamadı: " + bookId));

        int totalTopics = 0;
        int totalTests = 0;
        int totalQuestions = 0;

        // Her topic için sayıları hesapla ve güncelle
        for (Topic topic : book.getTopics()) {
            totalTopics++;

            int topicTestCount = 0;
            int topicQuestionCount = 0;

            // Her test için sayıları hesapla ve güncelle
            if (topic.getTests() != null) {
                for (Test test : topic.getTests()) {
                    topicTestCount++;
                    totalTests++;

                    int testQuestionCount = test.getQuestions() != null ? test.getQuestions().size() : 0;
                    topicQuestionCount += testQuestionCount;
                    totalQuestions += testQuestionCount;

                    // Test'in soru sayısını güncelle ve kaydet
                    test.setNumberOfQuestions(testQuestionCount);
                    testRepository.save(test);
                }
            }

            // Topic'in test ve soru sayılarını güncelle ve kaydet
            topic.setNumberOfTests(topicTestCount);
            topic.setNumberOfQuestions(topicQuestionCount);
            topicRepository.save(topic);
        }

        // Book'un toplam sayılarını güncelle ve kaydet
        book.setNumberOfTopics(totalTopics);
        book.setNumberOfTests(totalTests);
        book.setNumberOfQuestions(totalQuestions);
        bookRepository.save(book);
    }

    public String addCodes(Long bookId, List<String> codes) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Duplicate ve yeni kodları ayır
        List<String> duplicateCodes = codes.stream()
                .filter(codeStr -> bookCodeRepository.existsByCode(codeStr))
                .toList();

        List<String> newCodes = codes.stream()
                .filter(codeStr -> !bookCodeRepository.existsByCode(codeStr))
                .toList();

        // Yeni kodları ekle
        if (!newCodes.isEmpty()) {
            List<BookCode> bookCodes = newCodes.stream().map(codeStr -> {
                BookCode code = new BookCode();
                code.setCode(codeStr);
                code.setBook(book);
                code.setUsed(false);
                return code;
            }).toList();

            book.getCodes().addAll(bookCodes);
            bookCodeRepository.saveAll(bookCodes);
        }

        // Sonuç mesajı oluştur
        StringBuilder result = new StringBuilder();

        if (!newCodes.isEmpty()) {
            result.append(newCodes.size()).append(" kod başarıyla eklendi");
            if (!duplicateCodes.isEmpty()) {
                result.append(". ");
            }
        }

        if (!duplicateCodes.isEmpty()) {
            if (newCodes.isEmpty()) {
                result.append("Hiçbir kod eklenemedi. ");
            }
            result.append("Bu kodlar zaten mevcut: ").append(String.join(", ", duplicateCodes));
        }

        if (newCodes.isEmpty() && duplicateCodes.isEmpty()) {
            result.append("Hiçbir kod belirtilmedi");
        }

        return result.toString();
    }

    public PageResponse<BookStoreResponse> getBookStore(String query,
            List<ExamType> examTypes,
            List<GradeLevel> gradeLevels,
            List<LessonType> lessonTypes,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long userId, int page, int size) {
        List<Book> allBooks;
        List<Long> ownedBookIds;
        User user = null;

        if (userId != null) {
            // Kullanıcıyı bul
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Sahip olunan kitaplar
            ownedBookIds = user.getOwnedBooks().stream()
                    .map(Book::getId)
                    .toList();
        } else {
            ownedBookIds = new ArrayList<>();
        }

        List<ExamType> normalizedExamTypes = normalizeList(examTypes);
        List<GradeLevel> normalizedGradeLevels = normalizeList(gradeLevels);
        List<LessonType> normalizedLessonTypes = normalizeList(lessonTypes);

        // Arama ve filtrelemeyi repository'de yap
        allBooks = bookRepository.searchAndFilterBooks(
                query,
                normalizedExamTypes,
                normalizedGradeLevels,
                normalizedLessonTypes,
                minPrice,
                maxPrice);

        // Kişiselleştirilmiş önerilere göre sırala (kullanıcı varsa)
        if (user != null) {
            allBooks = bookRecommendationService.sortBooksByRecommendation(allBooks, user);
        }

        // Sahip olunan kitapları sona taşı
        List<Book> sortedBooks = new ArrayList<>();
        List<Book> ownedBooks = new ArrayList<>();

        for (Book book : allBooks) {
            if (userId != null && ownedBookIds.contains(book.getId())) {
                ownedBooks.add(book);
            } else {
                sortedBooks.add(book);
            }
        }

        // Sahip olunan kitapları sona ekle
        sortedBooks.addAll(ownedBooks);

        // BookStoreResponse'a çevir (rating bilgileri entity'den okunuyor)
        List<BookStoreResponse> allResponses = sortedBooks.stream()
                .map(book -> toBookStoreResponse(book, ownedBookIds.contains(book.getId())))
                .toList();

        // Pagination uygula
        int totalElements = allResponses.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<BookStoreResponse> pageContent = allResponses.subList(Math.max(0, start), end);

        return PageResponse.of(pageContent, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetail(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

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
        response.setGradeLevel(book.getGradeLevel());
        response.setExamType(book.getExamType());
        response.setLessonType(book.getLessonType());
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
