package com.beratcan.app.service;

import com.beratcan.app.dto.request.BookRequest;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.Test;
import com.beratcan.app.entity.Topic;
import com.beratcan.app.entity.User;
import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestRepository;
import com.beratcan.app.repository.TestResultRepository;
import com.beratcan.app.repository.TopicRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final BookCodeRepository bookCodeRepository;
    private final TestResultRepository testResultRepository;

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

        Optional<BookCode> codeOpt = codeRepository.findByCodeAndUsedFalse(codeValue);
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
        codeRepository.save(code);

        // Kullanıcıya kitabı ekle
        user.getOwnedBooks().add(book);
        userRepository.save(user);

        return "Kod başarıyla kullanıldı, kitap erişimi açıldı: " + book.getTitle();
    }

    public List<BookResponse> getBooksForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Paralı kitaplar (sahip olduğu) - skorlama ile sıralı
        List<Book> paidBooks = user.getOwnedBooks().stream()
                .filter(Book::isPaid)
                .sorted((b1, b2) -> {
                    double score1 = calculateBookUsageScore(userId, b1.getId());
                    double score2 = calculateBookUsageScore(userId, b2.getId());
                    return Double.compare(score2, score1); // Yüksekten düşüğe
                })
                .toList();

        // Ücretsiz kitaplar - skorlama ile sıralı
        List<Book> freeBooks = bookRepository.findByPaidFalse().stream()
                .sorted((b1, b2) -> {
                    double score1 = calculateBookUsageScore(userId, b1.getId());
                    double score2 = calculateBookUsageScore(userId, b2.getId());
                    return Double.compare(score2, score1); // Yüksekten düşüğe
                })
                .toList();

        // Önce paralı sonra ücretsiz kitaplar
        List<BookResponse> allBooks = new ArrayList<>();
        allBooks.addAll(paidBooks.stream().map(this::toBookResponse).toList());
        allBooks.addAll(freeBooks.stream().map(this::toBookResponse).toList());

        return allBooks;
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

        // Kod güncellemesi gerekiyorsa buraya eklenebilir
        return bookRepository.save(book);
    }

    // 📌 Kitap silme
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new RuntimeException("Book not found");
        }
        bookRepository.deleteById(bookId);
    }

    private BookResponse toBookResponse(Book book) {
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
}
