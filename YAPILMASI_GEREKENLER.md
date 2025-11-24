# 🎯 YAPILMASI GEREKEN SORUNLAR

**Tarih:** 2025-01-15  
**Durum:** Yapılmamış Sorunlar Listesi  
**Son Güncelleme:** 2025-01-15

---

## ✅ ÇÖZÜLEN SORUNLAR

1. ✅ **TestResultService Pagination** - `getUserTestResultsByBookId()` ve `getUserTestResults()` pagination ile güncellendi
2. ✅ **Exception Handling Güvenliği** - Production'da generic mesajlar, dev'de detaylı mesajlar
3. ✅ **Caching** - Filter options endpoint'i cache'leniyor
4. ✅ **AuthService** - Authentication logic service katmanına taşındı
5. ✅ **TestResultRepository Temizliği** - Kullanılmayan metodlar kaldırıldı

### 2. **Daily Question Modülünde File Operations Güvenlik Sorunları** ⚠️ YÜKSEK

**Konum:** `DailyQuestionService.createDailyQuestion()`

**Problem:**

- File upload validation eksik
- Antivirus/scanning yok
- Path traversal riski
- File deletion handling yok (record silinince file kalıyor)

**Mevcut Durum:**

```java
// File direkt uploads/ klasörüne kaydediliyor
// Validation minimal
// Silme işlemi yok
```

**Çözüm:**

1. `FileValidationUtil` oluştur (size, content type, extension kontrolü)
2. `FileStorageService` oluştur (güvenli kaydetme + silme)
3. Path traversal koruması ekle
4. File cleanup ekle (entity silindiğinde dosya silinsin)

**Etki:** Yüksek - Security ve storage management için kritik

---

### 3. **AIQuestionAnalyzer - Retry/Resilience Eksikliği** ⚠️ YÜKSEK

**Konum:** `AIQuestionAnalyzer.java`

**Problem:**

- Retry/backoff mekanizması yok
- Timeout handling eksik
- Circuit breaker yok
- External service hataları yönetilemiyor

**Çözüm:**

1. Resilience4j veya Spring Retry ekle
2. Retry mekanizması ekle:

```java
@Retry(name = "openai-api", fallbackMethod = "fallbackAnalyze")
@CircuitBreaker(name = "openai-api")
public QuestionAnalysisResult analyzeQuestion(File imageFile) {
    // ...
}
```

3. Timeout handling ekle
4. Fallback mechanism ekle

**Etki:** Yüksek - External service reliability için kritik

---

### 4. **Validation Eksikliği** ⚠️ YÜKSEK

**Problem:**

- DTO'larda `@Valid` annotation'ı eksik
- `@NotNull`, `@NotBlank`, `@Size`, `@Email` gibi validation annotation'ları yok
- Manual validation yapılıyor (UserService'te)
- Tutarsız validation yaklaşımı

**Çözüm:**

1. DTO'lara validation annotation'ları ekle:

```java
public class UserRequest {
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Telefon numarası zorunludur")
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Geçersiz telefon numarası")
    private String phoneNumber;
}
```

2. Controller'larda `@Valid` kullan
3. Custom validators oluştur (gerekirse)

**Etki:** Yüksek - Veri bütünlüğü ve güvenlik için önemli

---

## ⚡ YÜKSEK ÖNCELİKLİ SORUNLAR (2-4 Hafta İçinde)

### 5. **N+1 Query Problemleri (Kalanlar)** ⚠️ YÜKSEK

**Konum:** Birçok Service ve Repository

**Problem:**

- Lazy loading ile ilişkiler fetch edilirken N+1 query oluşuyor
- `@EntityGraph` veya `JOIN FETCH` kullanılmıyor (bazı yerlerde)
- Döngü içinde repository çağrıları

**Örnek Sorunlar:**

```java
// CartService.mapToResponse()
cart.getItems().stream()
    .map(item -> item.getBook().getPrice()) // ❌ Her item için ayrı query

// PurchaseService.mapToResponse()
purchase.getBook().getTitle() // ❌ Lazy load - N+1 query
```

**Çözüm:**

1. `@EntityGraph` kullan:

```java
@EntityGraph(attributePaths = {"items", "items.book"})
Optional<Cart> findByUserId(Long userId);
```

2. `JOIN FETCH` kullan (JPQL query'lerde)
3. Batch query'ler kullan

**Etki:** Yüksek - Performans sorunlarına yol açar

---

### 6. **Logging Eksiklikleri** ⚠️ YÜKSEK

**Problem:**

- `TestResultService` - HİÇ LOG YOK! (Core business logic)
- Bazı controller'larda loglama eksik
- AI cost tracking eksik

**Eksik Loglar:**

**TestResultService:**

```java
// ❌ HİÇBİR LOG YOK!
// Eklenecekler:
log.info("Test submission started: userId={}, testId={}, answerCount={}", ...);
log.info("Score calculated: correctCount={}, totalQuestions={}, successRate={}", ...);
log.error("Test submission failed: userId={}, testId={}, error={}", ..., exception);
```

**AIQuestionAnalyzer:**

```java
// Eksikler:
log.info("OpenAI API call: duration={}ms, cost=~${}, model={}", duration, cost, model);
log.warn("Slow AI response: duration={}ms, threshold=30000ms", duration);
```

**Çözüm:**

- Tüm kritik service'lere `@Slf4j` ekle
- Business logic kararlarını logla
- Error'ları stack trace ile logla
- Cost tracking ekle (AI calls için)

**Etki:** Yüksek - Production monitoring ve debugging için kritik

---

### 7. **Input Sanitization Eksikliği** ⚠️ YÜKSEK

**Problem:**

- SQL injection koruması var (JPA) ama
- XSS koruması yok
- File upload validation eksik
- Path traversal riski

**Çözüm:**

1. Input sanitization library kullan
2. File upload validation ekle (FileValidationUtil ile)
3. Path validation yap
4. HTML sanitization ekle (user input için)

**Etki:** Orta-Yüksek - Güvenlik için önemli

---

### 8. **CORS Konfigürasyonu Geniş** ⚠️ ORTA

**Konum:** `SecurityConfig.java`

**Problem:**

- `allowedHeaders("*")` çok geniş
- Production'da daha spesifik olmalı

**Mevcut Durum:**

```java
configuration.setAllowedHeaders(Arrays.asList("*")); // ❌ Çok geniş
```

**Çözüm:**

```java
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "X-Requested-With"
)); // ✅ Sadece gerekli header'lar
```

**Etki:** Orta - Güvenlik best practice

---

## 🟡 ORTA ÖNCELİKLİ İYİLEŞTİRMELER (1-2 Ay İçinde)

### 9. **Repository-Layer Validation**

**Problem:**

- Uniqueness check'ler (email/username/phone) 3 ayrı query ile yapılıyor
- Race condition riski var
- DB seviyesinde unique constraint yok

**Mevcut Durum:**

```java
if (userRepository.existsByEmail(dto.getEmail())) { // Query 1
if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) { // Query 2
if (userRepository.existsByUsername(dto.getUsername())) { // Query 3
```

**Çözüm:**

1. DB seviyesinde unique constraint ekle:

```sql
ALTER TABLE users ADD CONSTRAINT uk_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT uk_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_phone UNIQUE (phone_number);
```

2. Composite query kullan (opsiyonel)

**Etki:** Orta - Performans ve data integrity

---

### 10. **Index Eksikliği**

**Problem:**

- Foreign key'lerde index yok (otomatik eklenir ama)
- Sık sorgulanan alanlarda index yok
- Performance sorunları olabilir

**Eklenecek Index'ler:**

```sql
-- Books tablosu
CREATE INDEX idx_book_exam_type ON books(exam_type);
CREATE INDEX idx_book_grade_level ON books(grade_level);
CREATE INDEX idx_book_paid ON books(paid);
CREATE INDEX idx_book_price ON books(price);

-- Users tablosu
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);

-- TestResults tablosu
CREATE INDEX idx_test_result_user_id ON test_results(user_id);
CREATE INDEX idx_test_result_test_id ON test_results(test_id);
CREATE INDEX idx_test_result_completed_at ON test_results(completed_at);

-- BookReviews tablosu
CREATE INDEX idx_book_review_book_id ON book_reviews(book_id);
CREATE INDEX idx_book_review_user_id ON book_reviews(user_id);
```

**Etki:** Orta - Büyük veri setlerinde performans için önemli

---

### 11. **Unique Constraint Eksikliği**

**Problem:**

- Email, username, phone için unique constraint yok
- Duplicate data riski
- Race condition riski

**Eklenecek Constraint'ler:**

```sql
ALTER TABLE users ADD CONSTRAINT uk_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT uk_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_phone UNIQUE (phone_number);

ALTER TABLE user_owned_books ADD CONSTRAINT uk_user_book UNIQUE (user_id, book_id);
ALTER TABLE book_reviews ADD CONSTRAINT uk_book_user_review UNIQUE (book_id, user_id);
```

**Etki:** Yüksek - Veri bütünlüğü için kritik

---

### 12. **Payment Simulation**

**Problem:**

- `PurchaseService.processPayment()` her zaman `true` döndürüyor
- Gerçek payment gateway entegrasyonu için hazır değil
- Strategy pattern yok

**Mevcut Kod:**

```java
// TODO: Gerçek ödeme gateway'i (Stripe, iyzico, vb.) ile entegrasyon
boolean success = true; // ❌ Her zaman true!
```

**Çözüm:**

1. Payment strategy interface oluştur:

```java
public interface PaymentStrategy {
    boolean processPayment(Purchase purchase, PurchaseRequest request);
}

@Service
public class MockPaymentStrategy implements PaymentStrategy {
    // Development için
}

@Service
public class StripePaymentStrategy implements PaymentStrategy {
    // Production için
}
```

**Etki:** Orta - Future-proofing

---

### 13. **Logging'de Hassas Bilgi Riski**

**Problem:**

- Phone/email plaintext loglanıyor (registration failures)
- PII (Personal Identifiable Information) loglanıyor
- GDPR compliance riski

**Mevcut Durum:**

```java
log.warn("Registration failed - email already exists: email={}", dto.getEmail());
// ❌ Email plaintext loglanıyor
```

**Çözüm:**

1. Log sanitization utility oluştur:

```java
public class LogSanitizer {
    public static String sanitizeEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, Math.min(2, atIndex)) + "***@" + email.substring(atIndex + 1);
        }
        return "***";
    }

    public static String sanitizePhone(String phone) {
        if (phone == null) return null;
        return phone.length() > 4 ? "***" + phone.substring(phone.length() - 4) : "***";
    }
}
```

**Etki:** Orta - GDPR compliance ve security

---

## 🟢 DÜŞÜK ÖNCELİKLİ İYİLEŞTİRMELER (İleride)

### 15. **Entity Callback'leri Eksik**

**Problem:**

- `@PrePersist`, `@PreUpdate` eksik (bazı entity'lerde)
- Default değerler atanmıyor
- Timestamp'ler otomatik güncellenmiyor

**Çözüm:**

```java
@PrePersist
@PreUpdate
public void initializeRatingFields() {
    if (this.averageRating == null) {
        this.averageRating = 0.0;
    }
    if (this.reviewCount == null) {
        this.reviewCount = 0L;
    }
}
```

**Etki:** Düşük - Veri tutarlılığı

---

### 16. **Audit Trail Eksikliği**

**Problem:**

- `createdAt`, `updatedAt` bazı entity'lerde yok
- `createdBy`, `updatedBy` hiçbirinde yok
- Kim ne zaman değiştirdi bilinmiyor

**Çözüm:**

1. BaseEntity oluştur:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private Long createdBy;

    @Column
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**Etki:** Orta - Audit ve compliance

---

### 17. **Gereksiz Kod**

**Problem:**

- Kullanılmayan repository metodları var (temizlendi ama kontrol edilmeli)
- Dead code var
- TODO comment'leri kodda kalıyor

**Temizlenecekler:**

- `PurchaseService.processPayment` içindeki TODO
- Kullanılmayan helper metodlar

**Etki:** Düşük - Code quality

---

## 🧪 TEST EKSİKLİKLERİ

### Mevcut Durum

- ❌ JUnit/Integration test yok (sadece boş `AppApplicationTests`)
- ❌ Unit test yok
- ❌ Integration test yok
- ❌ End-to-end test yok

### Test Edilmesi Gerekenler

**Yüksek Öncelik:**

1. `BookCatalogService.getBookStore` (filter/sort/pagination)
2. `TestResultService.saveTestResult` (sequence generation, stats)
3. `QuestionFlagService` (auth ownership + duplicate handling)
4. Authentication flows (register/login/refresh/logout)

**Orta Öncelik:** 5. `PurchaseService.checkoutCart` (transaction consistency) 6. `BookReviewService` (rating calculation) 7. `CartService` (add/remove items)

### Test Stratejisi

1. **Unit Tests:**

   - Service layer logic
   - Business rule validation
   - Exception handling

2. **Integration Tests:**

   - Repository queries
   - Transaction management
   - Endpoint testing (MockMvc)

3. **Test Data:**
   - Testcontainers/Postgres
   - Data builders
   - Fixtures

**Tahmini Süre:** 2-3 hafta (comprehensive test suite)

---

## 📊 ÖNCELİK SIRASI VE UYGULAMA PLANI

### 🔥 FAZ 1: KRİTİK DÜZELTMELER (1-2 Hafta)

**Hedef:** Production-ready hale getirme

**Yapılacaklar:**

1. ⚠️ Rate limiting ekle (login, register, AI endpoints)
2. ⚠️ File validation ve security (DailyQuestionService, FileController)
3. ⚠️ AIQuestionAnalyzer retry/resilience ekle
4. ⚠️ Validation annotation'ları ekle (DTO'lara)
5. ⚠️ TestResultService loglama ekle
6. ⚠️ Unique constraint'ler ekle (DB)

**Süre:** 1-2 hafta  
**Etki:** Production deploy için gerekli

---

### 🔴 FAZ 2: PERFORMANS VE GÜVENLİK (2-4 Hafta)

**Hedef:** Performance ve security iyileştirmeleri

**Yapılacaklar:**

1. ⚠️ N+1 query problemini çöz (kalanlar)
2. ⚠️ Input sanitization ekle
3. ⚠️ CORS konfigürasyonunu daralt
4. ⚠️ Log sanitization ekle (PII masking)
5. ⚠️ Index optimizasyonu

**Süre:** 2-4 hafta  
**Etki:** Production performance ve güvenlik

---

### 🟡 FAZ 3: CODE QUALITY VE MAINTAINABILITY (2 Hafta)

**Hedef:** Uzun vadeli maintainability

**Yapılacaklar:**

1. ⚠️ Audit trail ekle (BaseEntity)
2. ⚠️ Dead code temizliği
3. ⚠️ Magic number'ları constant'a çevir
4. ⚠️ DTO mapping iyileştirmesi (MapStruct)
5. ⚠️ Payment strategy pattern ekle

**Süre:** 2 hafta  
**Etki:** Kod kalitesi ve bakım kolaylığı

---

### 🟢 FAZ 4: TEST SUITE (2-3 Hafta)

**Hedef:** Comprehensive test coverage

**Yapılacaklar:**

1. ⚠️ Unit tests (service layer)
2. ⚠️ Integration tests (repository, endpoints)
3. ⚠️ Testcontainers setup
4. ⚠️ Data builders oluştur
5. ⚠️ CI/CD pipeline'a test ekle

**Süre:** 2-3 hafta  
**Etki:** Code reliability ve regression prevention

---

## 📊 ÖZET TABLO

| Kategori             | Sorun Sayısı | Öncelik   | Tahmini Süre   |
| -------------------- | ------------ | --------- | -------------- |
| **Kritik Sorunlar**  | 4            | 🔥 ACİL   | 1-2 hafta      |
| **Yüksek Öncelikli** | 4            | ⚡ YÜKSEK | 2-4 hafta      |
| **Orta Öncelikli**   | 5            | 🟡 ORTA   | 1-2 ay         |
| **Düşük Öncelikli**  | 4            | 🟢 DÜŞÜK  | İleride        |
| **Test**             | 1            | 🟡 ORTA   | 2-3 hafta      |
| **TOPLAM**           | **18**       | -         | **8-12 hafta** |

---

## 🎓 SONUÇ

Bu rapor, backend kod tabanındaki **yapılmamış sorunlar** ve çözüm önerilerini içermektedir. Sorunlar öncelik sırasına göre kategorize edilmiş ve detaylı çözüm önerileri sunulmuştur.

**Önerilen Yaklaşım:**

1. **Faz 1** (1-2 hafta): Kritik sorunları çöz, production'a hazır hale getir
2. **Faz 2** (2-4 hafta): Performance ve security iyileştirmeleri
3. **Faz 3** (2 hafta): Code quality ve maintainability
4. **Faz 4** (2-3 hafta): Comprehensive test suite

**Toplam Tahmini Süre:** 8-12 hafta  
**Toplam Tahmini Etki:** %200+ code quality iyileştirmesi

---

**Rapor Oluşturulma Tarihi:** 2025-01-15  
**Son Güncelleme:** 2025-01-15  
**Hazırlayan:** AI Assistant  
**Durum:** Yapılması Gereken Sorunlar Listesi
