package com.beratcan.app.service;

import com.beratcan.app.dto.response.BookResponse;
import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.BookCode;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.BookNotFoundException;
import com.beratcan.app.exception.ConflictException;
import com.beratcan.app.exception.UserNotFoundException;
import com.beratcan.app.repository.BookCodeRepository;
import com.beratcan.app.repository.BookRepository;
import com.beratcan.app.repository.TestResultRepository;
import com.beratcan.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        BookCode code = bookCodeRepository.findByCodeAndUsedFalse(codeValue)
                .orElseThrow(() -> new BadRequestException("Kod geçersiz veya zaten kullanıldı"));
        Book book = code.getBook();

        // Kullanıcının bu kitabı zaten sahip olup olmadığını kontrol et
        boolean alreadyOwns = userRepository.existsByUserIdAndBookId(userId, book.getId());

        if (alreadyOwns) {
            throw new ConflictException("Bu kitabı zaten sahipsiniz");
        }

        code.setUsed(true);
        code.setUsedBy(user);
        bookCodeRepository.save(code);

        // Kullanıcıya kitabı ekle
        try {
            user.getOwnedBooks().add(book);
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // DB constraint hatası (duplicate entry) yakalandı
            if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                log.warn("Duplicate book ownership attempt prevented by DB constraint: userId={}, bookId={}",
                        userId, book.getId());
                throw new ConflictException("Bu kitabı zaten sahipsiniz");
            }
            throw e; // Diğer constraint hataları için
        }

        return "Kod başarıyla kullanıldı, kitap erişimi açıldı: " + book.getTitle();
    }

    /**
     * Ücretsiz kitap ekleme
     */
    @Transactional
    public String addFreeBookToLibrary(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // Kitap ücretsiz mi kontrol et
        if (book.isPaid()) {
            throw new BadRequestException("Bu kitap ücretsiz değil");
        }

        // Kullanıcının bu kitabı zaten kütüphanesinde var mı kontrol et
        boolean alreadyOwns = user.getOwnedBooks().stream()
                .anyMatch(b -> b.getId().equals(bookId));

        if (alreadyOwns) {
            throw new ConflictException("Bu kitap zaten kütüphanenizde");
        }

        // Kütüphaneye ekle
        try {
            user.getOwnedBooks().add(book);
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // DB constraint hatası (duplicate entry) yakalandı
            if (e.getMessage() != null && e.getMessage().contains("uk_user_book")) {
                log.warn("Duplicate book ownership attempt prevented by DB constraint: userId={}, bookId={}",
                        userId, bookId);
                throw new ConflictException("Bu kitap zaten kütüphanenizde");
            }
            throw e; // Diğer constraint hataları için
        }

        return "Kitap kütüphanenize eklendi: " + book.getTitle();
    }

    /**
     * Kullanıcının sahip olduğu kitapları kullanım skoruna göre sıralayarak döner
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getBooksForUser(Long userId) {
        // JOIN FETCH ile ownedBooks ve publisher ilişkilerini tek query'de yükle
        User user = userRepository.findByIdWithOwnedBooks(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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
        response.setPublisher(book.getPublisher() != null ? book.getPublisher().getName() : null);
        // Database'de filename var, response'da URL oluştur
        // Eğer zaten URL formatındaysa (eski kayıtlar için) olduğu gibi bırak
        String coverImageUrl = book.getCoverImageUrl();
        if (coverImageUrl != null && !coverImageUrl.isEmpty() && !coverImageUrl.startsWith("api/files/")) {
            response.setCoverImageUrl("api/files/" + coverImageUrl);
        } else {
            response.setCoverImageUrl(coverImageUrl);
        }
        response.setPaid(book.isPaid());
        // Hesaplanmış alanları entity'deki getter metodlarından al
        response.setNumberOfTopics(book.getNumberOfTopics());
        response.setNumberOfTests(book.getNumberOfTests());
        response.setNumberOfQuestions(book.getNumberOfQuestions());
        return response;
    }
}
