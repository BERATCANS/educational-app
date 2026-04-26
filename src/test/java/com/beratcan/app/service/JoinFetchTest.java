package com.beratcan.app.service;

import com.beratcan.app.entity.BookReview;
import com.beratcan.app.repository.BookReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JOIN FETCH ve Page kullanımı testleri
 * 
 * Test 1: Page ile JOIN FETCH - Duplicate sonuç kontrolü
 * Test 2: ownedBooks lazy loading - Performans kontrolü
 */
@SpringBootTest
@ActiveProfiles("test")
public class JoinFetchTest {

    @Autowired
    private BookReviewRepository bookReviewRepository;

    /**
     * Test 1: Page ile JOIN FETCH - Duplicate sonuç var mı?
     * 
     * Bu test, BookReviewRepository.findByBookIdExcludingUserId() metodunun
     * JOIN FETCH ile Page döndürürken duplicate sonuç döndürüp döndürmediğini
     * kontrol eder.
     */
    @Test
    @Transactional(readOnly = true)
    public void testPageWithJoinFetch_NoDuplicates() {
        // Test için bir kitap ID'si kullan (gerçek bir kitap ID'si olmalı)
        Long bookId = 1L; // Test için uygun bir ID seçin
        Long excludeUserId = null; // Tüm yorumları getir
        Pageable pageable = PageRequest.of(0, 10);

        // Query çalıştır
        Page<BookReview> reviewPage = bookReviewRepository.findByBookIdExcludingUserId(
                bookId, excludeUserId, pageable);

        // Test 1.1: Duplicate kontrolü
        Set<Long> reviewIds = reviewPage.getContent().stream()
                .map(BookReview::getId)
                .collect(Collectors.toSet());

        assertEquals(
                reviewPage.getContent().size(),
                reviewIds.size(),
                "Duplicate BookReview sonuçları var! Her review ID'si sadece bir kez olmalı.");

        // Test 1.2: COUNT doğruluğu
        long actualCount = bookReviewRepository.countByBookIdExcludingUserId(bookId, excludeUserId);

        assertEquals(
                reviewPage.getTotalElements(),
                actualCount,
                "Page.getTotalElements() gerçek COUNT ile eşleşmiyor!");

        // Test 1.3: Page size kontrolü
        assertTrue(
                reviewPage.getContent().size() <= reviewPage.getTotalElements(),
                "Page content size, total elements'ten büyük olamaz!");

        // Test 1.4: StudentProfile ve Book ilişkileri yüklü mü?
        reviewPage.getContent().forEach(review -> {
            assertNotNull(review.getStudentProfile(), "StudentProfile lazy loading hatası! StudentProfile yüklenmemiş.");
            assertNotNull(review.getBook(), "Book lazy loading hatası! Book yüklenmemiş.");

            // LazyInitializationException olmadan erişilebilmeli
            assertNotNull(review.getStudentProfile().getId(), "StudentProfile ID erişilemiyor!");
            assertNotNull(review.getBook().getId(), "Book ID erişilemiyor!");
        });

        System.out.println("✅ Test 1 Başarılı:");
        System.out.println("   - Duplicate yok");
        System.out.println("   - COUNT doğru: " + reviewPage.getTotalElements());
        System.out.println("   - Content size: " + reviewPage.getContent().size());
    }

    /**
     * Test 2: ownedBooks lazy loading - Performans kontrolü
     * 
     * Bu test, user.getOwnedBooks() çağrıldığında kaç query çalıştığını kontrol
     * eder.
     * 
     * NOT: Bu test için SQL log'larını manuel olarak kontrol etmeniz gerekiyor.
     * spring.jpa.show-sql=true olmalı.
     */
    @Test
    @Transactional(readOnly = true)
    public void testOwnedBooksLazyLoading_PerformanceCheck() {
        // Bu test için SQL log'larını manuel kontrol edin
        // Kullanıcının kitap sayısı kadar query çalışıp çalışmadığını gözlemleyin

        System.out.println("⚠️ Test 2: ownedBooks Lazy Loading");
        System.out.println("   SQL log'larını kontrol edin:");
        System.out.println("   - user.getOwnedBooks() çağrıldığında kaç query çalışıyor?");
        System.out.println("   - Kullanıcının kitap sayısı kadar query mi çalışıyor?");
        System.out.println("   - Eğer N query çalışıyorsa, Repository exists metodu eklenmeli!");
    }
}
