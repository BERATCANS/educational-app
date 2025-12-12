# N+1 Query ve Lazy Loading Analizi

## 🔴 KRİTİK SORUNLAR (Hemen Düzeltilmeli)

### 1. **CartService - N+1 Query Problemi** ⚠️ YÜKSEK ÖNCELİK

**Dosya:** `src/main/java/com/beratcan/app/service/CartService.java`

**Sorun:**

```java
// mapToResponse() metodunda
cart.getItems().stream()
    .map(this::mapItemToResponse) // Her item için ayrı query!

// mapItemToResponse() içinde
item.getBook().getId()           // Query 1
item.getBook().getTitle()        // Query 2
item.getBook().getPrice()        // Query 3
item.getBook().getPublisher()    // Query 4 (Publisher lazy)
```

**Etki:**

- Sepette 5 item varsa: 1 (Cart) + 5 (CartItem) + 5\*4 (Book + Publisher) = **26 query!**
- Yüksek trafikte database yükü çok artar

**Çözüm:**

```java
// CartRepository'ye JOIN FETCH query ekle
@Query("SELECT c FROM Cart c " +
       "JOIN FETCH c.user u " +
       "JOIN FETCH c.items i " +
       "JOIN FETCH i.book b " +
       "JOIN FETCH b.publisher p " +
       "LEFT JOIN FETCH c.appliedDiscountCode d " +
       "WHERE c.user.id = :userId")
Optional<Cart> findByUserIdWithItemsAndBooks(@Param("userId") Long userId);
```

---

### 2. **BookReviewService - N+1 Query Problemi** ⚠️ YÜKSEK ÖNCELİK

**Dosya:** `src/main/java/com/beratcan/app/service/BookReviewService.java`

**Sorun:**

```java
// getBookReviews() metodunda
reviewPage.getContent().stream()
    .map(this::toBookReviewResponse) // Her review için ayrı query!

// toBookReviewResponse() içinde
review.getUser().getId()            // Query 1
review.getUser().getFirstName()     // Query 2
review.getUser().getLastName()      // Query 3
```

**Etki:**

- 20 yorum varsa: 1 (BookReview listesi) + 20\*3 (User) = **61 query!**

**Çözüm:**

```java
// BookReviewRepository'ye JOIN FETCH query ekle
@Query("SELECT br FROM BookReview br " +
       "JOIN FETCH br.user u " +
       "JOIN FETCH br.book b " +
       "WHERE br.book.id = :bookId " +
       "AND (:excludeUserId IS NULL OR br.user.id != :excludeUserId) " +
       "ORDER BY br.createdAt DESC")
Page<BookReview> findByBookIdExcludingUserIdWithUser(
    @Param("bookId") Long bookId,
    @Param("excludeUserId") Long excludeUserId,
    Pageable pageable);
```

---

### 3. **PurchaseService - getPurchaseById() Metodunda Lazy Loading** ⚠️ ORTA ÖNCELİK

**Dosya:** `src/main/java/com/beratcan/app/service/PurchaseService.java`

**Sorun:**

```java
// getPurchaseById() metodunda
Purchase purchase = purchaseRepository.findById(purchaseId) // Lazy loading
    .orElseThrow(...);

purchase.getUser().getId()         // Lazy loading
purchase.getBook().getId()         // Lazy loading
purchase.getBook().getTitle()      // Lazy loading
purchase.getBook().getPublisher()   // Lazy loading
```

**Not:** `getUserPurchases()` ve `getUserCompletedPurchases()` zaten `@EntityGraph` kullanıyor ✅

**Çözüm:**

```java
// PurchaseRepository'ye JOIN FETCH query ekle
@Query("SELECT p FROM Purchase p " +
       "JOIN FETCH p.user u " +
       "JOIN FETCH p.book b " +
       "LEFT JOIN FETCH b.publisher pub " +
       "LEFT JOIN FETCH p.discountCode d " +
       "WHERE p.id = :purchaseId")
Optional<Purchase> findByIdWithUserAndBook(@Param("purchaseId") Long purchaseId);
```

---

## 🟡 ORTA ÖNCELİKLİ SORUNLAR

### 4. **TestResultService - getUserTestResultsByBookId()** ⚠️ ORTA ÖNCELİK

**Dosya:** `src/main/java/com/beratcan/app/service/TestResultService.java:231-245\*\*

**Durum:**

- `findAllByUserIdAndBookId()` zaten JOIN FETCH kullanıyor ✅
- Ama `mapToResponse()` içinde `testResult.getTest().getId()` çağrılıyor
- Test zaten JOIN FETCH ile yüklü, sorun yok ✅

**Öneri:** Kod çalışıyor ama kontrol edilebilir.

---

### 5. **CartService - calculateCartTotal() ve revalidateAndUpdateDiscountCode()** ⚠️ ORTA ÖNCELİK

**Dosya:** `src/main/java/com/beratcan/app/service/CartService.java:312-317`

**Sorun:**

```java
// calculateCartTotal() içinde
cart.getItems().stream()
    .map(item -> item.getBook().getPrice()) // Her item için ayrı query!
```

**Çözüm:** Cart zaten JOIN FETCH ile yüklenmeli (Yukarıdaki #1 çözümü bunu da çözer).

---

## 🟢 DÜŞÜK ÖNCELİK (Zaten İyi Durumda)

### 6. **PurchaseService - getUserPurchases() ve getUserCompletedPurchases()** ✅

**Durum:** `@EntityGraph` kullanılıyor, JOIN FETCH var ✅

---

### 7. **TestResultService - getUserTestResults()** ✅

**Durum:** JOIN FETCH kullanılıyor ✅

---

## 📊 ÖNCELİK SIRASI

| #   | Servis            | Metod                  | Sorun                                   | Öncelik   | Etki                 |
| --- | ----------------- | ---------------------- | --------------------------------------- | --------- | -------------------- |
| 1   | CartService       | `mapToResponse()`      | N+1 Query (CartItem → Book → Publisher) | 🔴 YÜKSEK | 5 item = 26 query    |
| 2   | BookReviewService | `getBookReviews()`     | N+1 Query (BookReview → User)           | 🔴 YÜKSEK | 20 review = 61 query |
| 3   | PurchaseService   | `getPurchaseById()`    | Lazy Loading                            | 🟡 ORTA   | 1 purchase = 4 query |
| 4   | CartService       | `calculateCartTotal()` | N+1 Query                               | 🟡 ORTA   | #1 ile çözülür       |

---

## 🛠️ UYGULAMA PLANI

### Adım 1: CartService (En Kritik)

1. `CartRepository`'ye JOIN FETCH query ekle
2. `CartService.getCart()` metodunu güncelle
3. `CartService.getOrCreateCart()` metodunu güncelle

### Adım 2: BookReviewService

1. `BookReviewRepository`'ye JOIN FETCH query ekle
2. `BookReviewService.getBookReviews()` metodunu güncelle
3. `BookReviewService.getMyReview()` metodunu güncelle

### Adım 3: PurchaseService

1. `PurchaseRepository`'ye JOIN FETCH query ekle
2. `PurchaseService.getPurchaseById()` metodunu güncelle

---

## 📈 BEKLENEN İYİLEŞTİRME

| Senaryo                     | Önceki Query Sayısı | Sonraki Query Sayısı | İyileştirme    |
| --------------------------- | ------------------- | -------------------- | -------------- |
| Sepet görüntüleme (5 item)  | 26                  | 1                    | **96% azalma** |
| Kitap yorumları (20 review) | 61                  | 1                    | **98% azalma** |
| Purchase detay              | 4                   | 1                    | **75% azalma** |

---

## ✅ SONUÇ

**Toplam 3 kritik sorun tespit edildi:**

1. CartService - N+1 Query (En kritik)
2. BookReviewService - N+1 Query (Kritik)
3. PurchaseService - Lazy Loading (Orta)

**Önerilen sıra:** CartService → BookReviewService → PurchaseService
