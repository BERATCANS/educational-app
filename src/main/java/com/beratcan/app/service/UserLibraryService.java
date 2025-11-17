package com.beratcan.app.service;

import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.User;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestResultRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserLibraryService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCodeRepository bookCodeRepository;
    private final TestResultRepository testResultRepository;

    /**
     * Kullanıcı kod kullanımı
     */
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

    /**
     * Ücretsiz kitap ekleme
     */
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

    /**
     * Kullanıcının sahip olduğu kitapları kullanım skoruna göre sıralayarak döner
     */
    @Transactional(readOnly = true)
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

        return sortedOwnedBooks.stream()
                .map(this::toBookResponse)
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
}
