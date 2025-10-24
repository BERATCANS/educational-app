# 📋 LOGLAMA STRATEJİSİ

**Tarih:** 24 Ekim 2025  
**Amaç:** Detaylı, anlamlı ve production-ready loglama sistemi

---

## 📊 MEVCUT DURUM ANALİZİ

### ✅ @Slf4j Kullanan Sınıflar (8 adet)

- `JwtUtil.java`
- `AuthEntryPointJwt.java`
- `JwtAuthenticationFilter.java`
- `DailyQuestionScheduler.java`
- `FileController.java`
- `UserController.java`
- `AIQuestionAnalyzer.java`
- `TokenBlacklistService.java`

### ❌ Loglama Eksik Olan Sınıflar

**Service Katmanı (8 adet):**

- `BookService.java`
- `CartService.java`
- `DailyQuestionService.java`
- `PurchaseService.java` ⚠️ (System.out.println kullanıyor)
- `QuestionService.java`
- `TestResultService.java`
- `TestService.java`
- `TopicService.java`
- `UserService.java` (az loglama var)

**Controller Katmanı (9 adet):**

- `BookController.java`
- `CartController.java`
- `DailyQuestionController.java`
- `PurchaseController.java`
- `QuestionController.java`
- `TestController.java`
- `TestResultController.java`
- `TopicController.java`
- `UserController.java` (iyileştirilebilir)

---

## 🎯 LOGLAMA PRENSİPLERİ

### Log Seviyeleri (Kullanım Rehberi)

#### 🔴 ERROR - Ciddi Hatalar

**Ne zaman:** Uygulama çalışmasını etkileyen hatalar
**Örnekler:**

- Database bağlantı hatası
- Ödeme işlemi başarısız
- External API çağrısı hata verdi
- Beklenmeyen exception'lar

```java
log.error("Payment processing failed: userId={}, amount={}, error={}",
    userId, amount, e.getMessage(), e);
```

#### 🟠 WARN - Dikkat Gerektiren Durumlar

**Ne zaman:** Hata değil ama dikkat edilmesi gereken durumlar
**Örnekler:**

- Deprecated API kullanımı
- Rate limit aşımı
- Geçersiz input (business logic hatası)
- Performans problemi (yavaş query)

```java
log.warn("Book already in cart: userId={}, bookId={}", userId, bookId);
log.warn("Slow database query detected: query took {}ms", duration);
```

#### 🟢 INFO - Önemli İş Akışları

**Ne zaman:** Normal ama önemli işlemler
**Örnekler:**

- Kullanıcı login/logout
- Satın alma işlemi tamamlandı
- Veri oluşturma/güncelleme/silme
- Scheduled job çalıştı

```java
log.info("User logged in: username={}, userId={}", username, userId);
log.info("Purchase completed: userId={}, bookId={}, transactionId={}",
    userId, bookId, transactionId);
```

#### 🔵 DEBUG - Detaylı Akış Bilgisi

**Ne zaman:** Development/troubleshooting için
**Örnekler:**

- Method girişi/çıkışı
- Ara hesaplama sonuçları
- Validasyon kontrolü detayları
- Cache hit/miss

```java
log.debug("Checking cart for user: userId={}", userId);
log.debug("Validation result: book={}, isPaid={}, hasPrice={}",
    bookId, book.isPaid(), book.getPrice() != null);
```

#### ⚪ TRACE - En Detaylı Bilgi

**Ne zaman:** Nadiren, çok detaylı debugging için
**Örnekler:**

- Loop içindeki her iterasyon
- Her bir validation step

```java
log.trace("Processing cart item: cartItemId={}, bookId={}", item.getId(), item.getBook().getId());
```

---

## 📁 KATMAN BAZINDA LOGLAMA STRATEJİSİ

### 1️⃣ CONTROLLER KATMANI

**Loglanması Gerekenler:**

- ✅ Method girişi (endpoint + userId)
- ✅ Gelen request parametreleri (hassas bilgi hariç!)
- ✅ Başarılı response
- ✅ Exception durumları (GlobalExceptionHandler'da)

**Log Seviyesi:** INFO (başarılı işlemler), WARN (validation hataları)

**Örnek Pattern:**

```java
@GetMapping("/{bookId}")
public ResponseEntity<BookResponse> getBook(@PathVariable Long bookId, Authentication auth) {
    Long userId = Long.parseLong(auth.getName());
    log.info("GET /api/books/{}: userId={}", bookId, userId);

    BookResponse book = bookService.getBookById(bookId);

    log.info("Book retrieved successfully: bookId={}, title={}", bookId, book.getTitle());
    return ResponseEntity.ok(book);
}
```

**ÖNEMLİ:** Controller'da exception handling YAPMA! GlobalExceptionHandler'da merkezi loglama yap.

---

### 2️⃣ SERVICE KATMANI

**Loglanması Gerekenler:**

- ✅ Method girişi (kritik metodlar için)
- ✅ Business logic kararları
- ✅ Transactional işlemler (başlangıç/bitiş)
- ✅ External API çağrıları
- ✅ Database işlemleri (önemli olanlar)
- ✅ Exception'lar (stack trace ile)

**Log Seviyesi:** INFO (iş akışı), DEBUG (detay), ERROR (hatalar)

**Örnek Pattern:**

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    @Transactional
    public CartResponse addToCart(Long userId, Long bookId) {
        log.debug("Adding book to cart: userId={}, bookId={}", userId, bookId);

        Cart cart = getOrCreateCart(userId);
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> {
                log.error("Book not found: bookId={}", bookId);
                return new RuntimeException("Kitap bulunamadı");
            });

        // Validations
        if (!book.isPaid()) {
            log.warn("Attempted to add free book to cart: userId={}, bookId={}", userId, bookId);
            return createInfoResponse(OperationStatus.FREE_BOOK, "Ücretsiz kitap");
        }

        // Success
        log.info("Book added to cart: userId={}, bookId={}, cartItemCount={}",
            userId, bookId, cart.getItems().size());

        return mapToResponse(cart);
    }
}
```

---

### 3️⃣ REPOSITORY KATMANI

**Loglanması Gerekenler:**

- ❌ Genelde LOGLAMAYA GEREK YOK (Spring JPA kendi logluyor)
- ✅ Sadece custom complex query'ler için DEBUG seviyesinde

---

### 4️⃣ SECURITY KATMANI

**Loglanması Gerekenler:**

- ✅ Authentication başarılı/başarısız
- ✅ Token generation/validation
- ✅ Blacklist işlemleri
- ✅ Unauthorized access denemeleri

**Log Seviyesi:** INFO (normal flow), WARN (failed attempts), ERROR (kritik güvenlik)

---

## 🔒 GÜVENLİK: LOGLAMAMASI GEREKEN BİLGİLER

### ❌ ASLA LOGLAMA:

- Şifreler (plain text)
- Credit card numaraları
- JWT token'ın kendisi (sadece ID log)
- API keys
- Private keys

### ✅ GÜVENLİ LOGLAMA:

```java
// ❌ YANLIŞ
log.info("User login: password={}", password);
log.info("JWT created: token={}", jwtToken);

// ✅ DOĞRU
log.info("User login: username={}", username);
log.info("JWT created: userId={}, expiresIn={}", userId, expiresIn);
```

---

## 📋 UYGULAMA PLANI

### ADIM 1: PurchaseService - System.out.println Temizliği

**Öncelik:** ⚠️ ACİL  
**Süre:** 10 dakika  
**Dosya:** `PurchaseService.java`

**Yapılacaklar:**

- `System.out.println` → `log.warn`
- `@Slf4j` annotation ekle
- Tüm critical işlemlere log ekle

---

### ADIM 2: Service Katmanı - Kritik Servisler

**Öncelik:** 🔴 YÜKSEK  
**Süre:** 2 saat  
**Dosyalar:**

1. `PurchaseService.java` (en kritik)
2. `CartService.java` (kritik)
3. `UserService.java` (kritik)
4. `BookService.java`
5. `TestResultService.java`

**Her service için:**

- `@Slf4j` ekle
- Method başında INFO log
- Business logic kararlarında DEBUG/INFO log
- Exception'larda ERROR log
- Başarılı işlemlerde INFO log

---

### ADIM 3: Controller Katmanı - Ana Endpoint'ler

**Öncelik:** 🟡 ORTA  
**Süre:** 1.5 saat  
**Dosyalar:**

1. `PurchaseController.java`
2. `CartController.java`
3. `UserController.java` (iyileştir)
4. `BookController.java`

**Her controller için:**

- `@Slf4j` ekle (yoksa)
- Endpoint girişinde INFO log
- Başarılı response'da INFO log

---

### ADIM 4: GlobalExceptionHandler İyileştirme

**Öncelik:** 🟡 ORTA  
**Süre:** 30 dakika  
**Dosya:** `GlobalExceptionHandler.java`

**Yapılacaklar:**

- Her exception handler'da detaylı log
- Stack trace loglama (ERROR seviyesinde)
- Request bilgilerini log (path, method, userId)

---

### ADIM 5: Diğer Service'ler

**Öncelik:** 🟢 DÜŞÜK  
**Süre:** 1 saat  
**Dosyalar:**

- `DailyQuestionService.java`
- `QuestionService.java`
- `TestService.java`
- `TopicService.java`

---

### ADIM 6: Diğer Controller'lar

**Öncelik:** 🟢 DÜŞÜK  
**Süre:** 45 dakika  
**Dosyalar:**

- `TestResultController.java`
- `TopicController.java`
- `TestController.java`
- `QuestionController.java`
- `DailyQuestionController.java`

---

## 📊 ÖNCELIK SIRASI (Tavsiye)

### 🔥 ŞİMDİ (15 dakika)

1. ✅ `PurchaseService.java` - System.out.println temizliği

### 🔴 BU SEANS (2 saat)

2. ✅ `CartService.java` - Kritik business logic
3. ✅ `UserService.java` - Authentication işlemleri
4. ✅ `PurchaseController.java` - Endpoint loglama

### 🟡 SONRA (2 saat)

5. ✅ `BookService.java`
6. ✅ `TestResultService.java`
7. ✅ `GlobalExceptionHandler.java`
8. ✅ Diğer controller'lar

### 🟢 İLERDE (1 saat)

9. ✅ Kalan tüm service'ler
10. ✅ Kalan tüm controller'lar

---

## 🎯 BAŞARILI LOGLAMA KRİTERLERİ

### Bir servis "iyi loglanmış" sayılır eğer:

- [ ] `@Slf4j` annotation'ı var
- [ ] Her public method'da en az 1 log var
- [ ] Kritik business logic kararları loglanmış
- [ ] Exception'lar düzgün loglanmış (stack trace ile)
- [ ] Başarılı işlemler INFO seviyesinde loglanmış
- [ ] Hassas bilgiler loglanmamış

### Bir controller "iyi loglanmış" sayılır eğer:

- [ ] `@Slf4j` annotation'ı var
- [ ] Her endpoint'de giriş logu var
- [ ] UserId loglanmış (authentication varsa)
- [ ] Başarılı response loglanmış
- [ ] Hassas bilgiler loglanmamış

---

## 🧪 TEST SENARYOSU

Log'ları test etmek için:

```bash
# Backend'i başlat ve log'ları izle
tail -f backend.log

# Test senaryoları:
1. Kullanıcı login → INFO log görmeli
2. Sepete kitap ekle → INFO log görmeli
3. Satın alma yap → INFO log görmeli
4. Hatalı request at → ERROR/WARN log görmeli
```

---

## 📈 SONUÇ

Bu stratejiyi uyguladığımızda:

- ✅ Production'da sorun tespiti kolay
- ✅ Monitoring yapılabilir
- ✅ Debugging hızlı
- ✅ Audit trail oluşur
- ✅ Performance tracking yapılabilir

**Toplam Süre:** ~6-7 saat  
**Kapsam:** 20+ dosya  
**Etki:** Code quality 🚀 %200 artar

---

**Hazırlayan:** AI Assistant  
**Tarih:** 24 Ekim 2025  
**Durum:** Plan aşaması, uygulamaya hazır
