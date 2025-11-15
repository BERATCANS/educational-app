package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.dto.request.BookReviewRequest;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.dto.response.BookReviewResponse;
import com.beratcan.app.dto.response.BookStoreResponse;
import com.beratcan.app.dto.response.PageResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.BookReview;
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
import com.beratcan.app.repository.TestResultRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCodeRepository bookCodeRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final TestResultRepository testResultRepository;
    private final BookReviewRepository bookReviewRepository;

    public Book addBook(Book book, List<String> topics) {

        // book’u önce kaydet
        Book savedBook = bookRepository.save(book);

        if (topics != null) {
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

    // Kullanıcı kod kullanımı
    @Transactional
    public String useCode(Long userId, String codeValue) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty())
            return "Kullanıcı bulunamadı";

        User user = userOpt.get();

        Optional<BookCode> codeOpt = bookCodeRepository.findByCodeAndUsedFalse(codeValue);
        if (codeOpt.isEmpty())
            return "Kod geçersiz veya zaten kullanıldı";

        BookCode code = codeOpt.get();
        Book book = code.getBook();

        // Kullanıcının bu kitabı zaten sahip olup olmadığını kontrol et
        boolean alreadyOwns = user.getOwnedBooks().stream()
                .anyMatch(ownedBook -> ownedBook.getId().equals(book.getId()));

        if (alreadyOwns) {
            return "Bu kitabı zaten sahipsiniz";
        }

        code.setUsed(true);
        code.setUsedBy(user);
        bookCodeRepository.save(code);

        // Kullanıcıya kitabı ekle
        user.getOwnedBooks().add(book);
        userRepository.save(user);

        return "Kod başarıyla kullanıldı, kitap erişimi açıldı: " + book.getTitle();
    }

    // Ücretsiz kitap ekleme
    @Transactional
    public String addFreeBookToLibrary(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Kitap ücretsiz mi kontrol et
        if (book.isPaid()) {
            return "Bu kitap ücretsiz değil";
        }

        // Kullanıcının bu kitabı zaten kütüphanesinde var mı kontrol et
        boolean alreadyOwns = user.getOwnedBooks().stream()
                .anyMatch(b -> b.getId().equals(bookId));

        if (alreadyOwns) {
            return "Bu kitap zaten kütüphanenizde";
        }

        // Kütüphaneye ekle
        user.getOwnedBooks().add(book);
        userRepository.save(user);

        return "Kitap kütüphanenize eklendi: " + book.getTitle();
    }

    public List<BookResponse> getBooksForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Book> sortedOwnedBooks = user.getOwnedBooks().stream()
                .sorted((b1, b2) -> {
                    double score1 = calculateBookUsageScore(userId, b1.getId());
                    double score2 = calculateBookUsageScore(userId, b2.getId());
                    return Double.compare(score2, score1);
                })
                .toList();

        Map<Long, RatingStats> ratingStats = getRatingStats(sortedOwnedBooks);

        return sortedOwnedBooks.stream()
                .map(book -> toBookResponse(book, ratingStats.get(book.getId())))
                .toList();
    }

    /**
     * Kitap kullanım skorunu hesaplar
     * Skor = Toplam Test Sayısı / Kaç Gün Önce Çözüldü
     * Hiç test çözülmemişse skor = 0
     */
    private double calculateBookUsageScore(Long userId, Long bookId) {
        // Çözülen test sayısı
        Long testCount = testResultRepository.countTestsByUserIdAndBookId(userId, bookId);

        if (testCount == null || testCount == 0) {
            return 0.0; // Hiç test çözülmemiş
        }

        // En son test çözme tarihi
        LocalDateTime lastCompleted = testResultRepository.findLastCompletedDateByUserIdAndBookId(userId, bookId);

        if (lastCompleted == null) {
            return 0.0; // Tarihi bulunamadı
        }

        // Kaç gün önce çözüldü
        long daysAgo = ChronoUnit.DAYS.between(lastCompleted, LocalDateTime.now());

        // 0 gün önce (bugün) çözüldüyse, 1 gün olarak hesapla (bölme hatası önleme)
        if (daysAgo == 0) {
            daysAgo = 1;
        }

        // Skor hesapla
        double score = (double) testCount / daysAgo;

        return score;
    }

    // 📌 Kitap güncelleme
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

    // 📌 Kitap silme
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found");
        }
        bookRepository.deleteById(bookId);
    }

    private BookResponse toBookResponse(Book book, RatingStats ratingStats) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setPublisher(book.getPublisher());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setPaid(book.isPaid());
        // Hesaplanmış alanları entity'deki getter metodlarından al
        response.setNumberOfTopics(book.getNumberOfTopics());
        response.setNumberOfTests(book.getNumberOfTests());
        response.setNumberOfQuestions(book.getNumberOfQuestions());
        // Enum alanları
        response.setGradeLevel(book.getGradeLevel());
        response.setExamType(book.getExamType());
        response.setLessonType(book.getLessonType());
        return response;
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
            allBooks = sortBooksByRecommendation(allBooks, user);
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

        Map<Long, RatingStats> ratingStats = getRatingStats(sortedBooks);

        // BookStoreResponse'a çevir
        List<BookStoreResponse> allResponses = sortedBooks.stream()
                .map(book -> toBookStoreResponse(book, ownedBookIds.contains(book.getId()),
                        ratingStats.get(book.getId())))
                .toList();

        // Pagination uygula
        int totalElements = allResponses.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<BookStoreResponse> pageContent = allResponses.subList(Math.max(0, start), end);

        return PageResponse.of(pageContent, page, size, totalElements);
    }

    @Transactional
    public String addOrUpdateReview(Long userId, Long bookId, BookReviewRequest request) {
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

        boolean ownsBook = user.getOwnedBooks().stream()
                .anyMatch(ownedBook -> ownedBook.getId().equals(bookId));

        if (!ownsBook) {
            throw new IllegalArgumentException("Sadece sahip olduğunuz kitaplar için yorum yapabilirsiniz.");
        }

        Optional<BookReview> existingReviewOpt = bookReviewRepository.findByBookIdAndUserId(bookId, userId);
        BookReview review = existingReviewOpt.orElseGet(BookReview::new);
        boolean isNew = review.getId() == null;
        LocalDateTime now = LocalDateTime.now();

        review.setBook(book);
        review.setUser(user);
        if (isNew) {
            review.setCreatedAt(now);
        }
        review.setRating(rating);
        review.setComment(request.getComment());
        review.setUpdatedAt(now);

        bookReviewRepository.save(review);

        return isNew ? "Yorum kaydedildi." : "Yorum güncellendi.";
    }

    @Transactional(readOnly = true)
    public List<BookReviewResponse> getBookReviews(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new IllegalArgumentException("Kitap bulunamadı: " + bookId);
        }

        return bookReviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
                .map(this::toBookReviewResponse)
                .toList();
    }

    /**
     * Kullanıcının sınav tipine, sınıfına ve alanına göre kitapları sıralar
     */
    private List<Book> sortBooksByRecommendation(List<Book> books, User user) {
        // Sınav tipi veya sınıf yoksa, tüm kitapları olduğu gibi dön
        if (user.getExamType() == null && user.getGradeLevel() == null) {
            return books;
        }

        List<Book> recommendedBooks = new ArrayList<>();
        List<Book> otherBooks = new ArrayList<>();

        for (Book book : books) {
            boolean isRecommended = false;

            // LGS için özel mantık
            if (user.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
                isRecommended = isRecommendedForLGS(book, user.getGradeLevel());
            }
            // YKS için özel mantık
            else if (user.getExamType() == com.beratcan.app.entity.enums.ExamType.YKS) {
                isRecommended = isRecommendedForYKS(book, user.getGradeLevel());
            }
            // KPSS veya diğer sınavlar için
            else if (user.getExamType() == com.beratcan.app.entity.enums.ExamType.KPSS ||
                    user.getExamType() == com.beratcan.app.entity.enums.ExamType.DIGER) {
                isRecommended = book.getExamType() == user.getExamType();
            }

            if (isRecommended) {
                recommendedBooks.add(book);
            } else {
                otherBooks.add(book);
            }
        }

        // YKS için sınıf ve sınav tipine göre özel sıralama
        if (user.getExamType() == com.beratcan.app.entity.enums.ExamType.YKS) {
            recommendedBooks = sortYKSRecommendedBooks(recommendedBooks, user.getGradeLevel());
        }
        // LGS için sınıf ve sınav tipine göre özel sıralama
        else if (user.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
            recommendedBooks = sortLGSRecommendedBooks(recommendedBooks, user.getGradeLevel());
        }

        // Önce önerilen kitaplar, sonra diğerleri
        recommendedBooks.addAll(otherBooks);
        return recommendedBooks;
    }

    /**
     * YKS kitaplarını sınıf ve sınav tipine göre sıralar
     * Sıralama: Sınıfa uygun kitaplar -> Sınav tipine uygun kitaplar -> Diğerleri
     */
    private List<Book> sortYKSRecommendedBooks(List<Book> books,
            com.beratcan.app.entity.enums.GradeLevel userGradeLevel) {

        List<Book> matchingGradeBooks = new ArrayList<>();
        List<Book> otherYKSBooks = new ArrayList<>();

        for (Book book : books) {
            // Sınıfa uygun kitaplar
            if (userGradeLevel != null && book.getGradeLevel() == userGradeLevel) {
                matchingGradeBooks.add(book);
            }
            // Diğer YKS kitapları
            else {
                otherYKSBooks.add(book);
            }
        }

        // Sıralamayı birleştir: sınıfa uygun -> diğer YKS kitapları
        List<Book> sortedBooks = new ArrayList<>();
        sortedBooks.addAll(matchingGradeBooks);
        sortedBooks.addAll(otherYKSBooks);
        return sortedBooks;
    }

    /**
     * LGS kitaplarını sınıf ve sınav tipine göre sıralar
     * 5-6-7. sınıf için: Sınıfa uygun kitaplar -> LGS kitapları
     * 8. sınıf için: LGS kitapları -> 5-6-7. sınıf kitapları
     */
    private List<Book> sortLGSRecommendedBooks(List<Book> books,
            com.beratcan.app.entity.enums.GradeLevel userGradeLevel) {

        // 5-6-7. sınıf için sıralama
        if (userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G5 ||
                userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G6 ||
                userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G7) {

            List<Book> matchingGradeBooks = new ArrayList<>();
            List<Book> lgsBooks = new ArrayList<>();

            for (Book book : books) {
                // Sınıfa uygun kitaplar
                if (book.getGradeLevel() == userGradeLevel) {
                    matchingGradeBooks.add(book);
                }
                // LGS kitapları
                else if (book.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
                    lgsBooks.add(book);
                }
            }

            // Sıralamayı birleştir: sınıfa uygun -> LGS
            List<Book> sortedBooks = new ArrayList<>();
            sortedBooks.addAll(matchingGradeBooks);
            sortedBooks.addAll(lgsBooks);
            return sortedBooks;
        }

        // 8. sınıf için sıralama
        if (userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G8) {

            List<Book> lgsBooks = new ArrayList<>();
            List<Book> grade5To7Books = new ArrayList<>();

            for (Book book : books) {
                // LGS kitapları
                if (book.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
                    lgsBooks.add(book);
                }
                // 5-6-7. sınıf kitapları
                else if (book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G5 ||
                        book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G6 ||
                        book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G7) {
                    grade5To7Books.add(book);
                }
            }

            // Sıralamayı birleştir: LGS -> 5-6-7. sınıf
            List<Book> sortedBooks = new ArrayList<>();
            sortedBooks.addAll(lgsBooks);
            sortedBooks.addAll(grade5To7Books);
            return sortedBooks;
        }

        // Diğer durumlar için orijinal sırayı koru
        return books;
    }

    /**
     * LGS için öneri mantığı
     * - 5-6-7. sınıf: Sınıfına uygun kitaplar ve LGS kitapları
     * - 8. sınıf: LGS kitapları ve 5-6-7. sınıf kitapları
     */
    private boolean isRecommendedForLGS(Book book, com.beratcan.app.entity.enums.GradeLevel userGradeLevel) {
        if (userGradeLevel == null) {
            return book.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS;
        }

        // 5-6-7. sınıf: Sınıfına uygun kitaplar ve LGS kitapları
        if (userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G5 ||
                userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G6 ||
                userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G7) {
            // Sınıfına uygun kitaplar
            if (book.getGradeLevel() == userGradeLevel) {
                return true;
            }
            // LGS kitapları
            if (book.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
                return true;
            }
        }

        // 8. sınıf: LGS kitapları öncelikli
        if (userGradeLevel == com.beratcan.app.entity.enums.GradeLevel.G8) {
            // LGS kitapları
            if (book.getExamType() == com.beratcan.app.entity.enums.ExamType.LGS) {
                return true;
            }
            // 5-6-7. sınıf kitapları da göster
            if (book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G5 ||
                    book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G6 ||
                    book.getGradeLevel() == com.beratcan.app.entity.enums.GradeLevel.G7) {
                return true;
            }
        }

        return false;
    }

    /**
     * YKS için öneri mantığı
     * YKS kitapları her zaman önerilir, ayrıca sınıfına uygun kitaplar da önerilir
     */
    private boolean isRecommendedForYKS(Book book, com.beratcan.app.entity.enums.GradeLevel userGradeLevel) {
        // YKS kitapları her zaman önerilir
        if (book.getExamType() == com.beratcan.app.entity.enums.ExamType.YKS) {
            return true;
        }

        if (userGradeLevel == null) {
            return false;
        }

        // Sınıfına uygun kitaplar
        if (book.getGradeLevel() == userGradeLevel) {
            return true;
        }

        return false;
    }

    private BookStoreResponse toBookStoreResponse(Book book, Boolean isOwned, RatingStats ratingStats) {
        BookStoreResponse response = new BookStoreResponse();
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
        response.setOwned(isOwned != null && isOwned);
        if (ratingStats != null) {
            response.setAverageRating(ratingStats.averageRating());
            response.setReviewCount(ratingStats.reviewCount());
        } else {
            response.setAverageRating(0.0);
            response.setReviewCount(0L);
        }
        return response;
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
                review.getUpdatedAt());
    }

    private Map<Long, RatingStats> getRatingStats(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> bookIds = books.stream()
                .map(Book::getId)
                .toList();

        Map<Long, RatingStats> statsMap = new HashMap<>();

        bookReviewRepository.findRatingStatsByBookIds(bookIds)
                .forEach(stat -> statsMap.put(stat.getBookId(),
                        new RatingStats(
                                stat.getAverageRating() != null ? stat.getAverageRating() : 0.0,
                                stat.getReviewCount() != null ? stat.getReviewCount() : 0L)));

        return statsMap;
    }

    private record RatingStats(double averageRating, long reviewCount) {
    }

    private <T> List<T> normalizeList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }
}
