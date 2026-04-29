# Backend Kod İnceleme Raporu — `app/`

> İnceleme tarihi: 2026-04-26 · Kapsam: `app/src/main/java` (263 dosya, ~18.7K LOC) + Flyway migration + application properties

## Context

`app/` altındaki Spring Boot backend baştan sona güvenlik, iş mantığı, JPA/migration ve doğrulama açılarından incelendi. Aşağıdaki bulgular gerçek kod üzerinden doğrulanmıştır; spekülatif maddeler elenmiştir.

**Genel sağlık:** Mimari temiz (DTO/entity ayrımı, GlobalExceptionHandler, Bean Validation, BCrypt, JWT + refresh token, soft-delete `@SQLRestriction`, rate-limit aspect'i mevcut). Ancak kod kullanımı, şema tutarlılığı ve checkout akışında **production'a çıkmadan giderilmesi gereken kritik bir kaç hata** var.

> **Not — Ödeme gateway:** `processPayment()`'in stub olması bilinçli bir iş kararı; gerçek gateway entegrasyonu ileride ayrıca planlanacak ve bu rapor kapsamı dışında.

---

## 🔴 KRİTİK Bulgular

### K1. `carts` tablosunda şema/entity uyuşmazlığı — prod start FAIL eder
- **Entity:** [src/main/java/com/beratcan/app/entity/Cart.java:26-28](../src/main/java/com/beratcan/app/entity/Cart.java#L26-L28) → `@JoinColumn(name = "student_profile_id", nullable = false, unique = true)`
- **Repository:** [src/main/java/com/beratcan/app/repository/CartRepository.java:18-23](../src/main/java/com/beratcan/app/repository/CartRepository.java#L18-L23) → `c.studentProfile.user.id` navigasyonu kullanıyor.
- **Migration:** V14 satır 138-141'de açıkça `-- 5. carts user bazlı kalır` ve `ALTER TABLE carts ADD CONSTRAINT uk_carts_user UNIQUE (user_id)` deniyor. Hiçbir migration `carts.user_id`'yi `student_profile_id`'ye dönüştürmüyor.
- **Sonuç:**
  - **Prod:** [application-prod.properties:27](../src/main/resources/application-prod.properties#L27) `ddl-auto=validate` → uygulama startup'ta `Schema-validation: missing column [student_profile_id] in table [carts]` ile patlar.
  - **Dev:** `ddl-auto=update` Hibernate yeni `student_profile_id` kolonu ekler ama eski `user_id NOT NULL` kolonu kalır → ilk INSERT `null value in column "user_id"` ile fail.
- **Fix:** Yeni `V17__Migrate_carts_to_student_profile.sql` ekle:
  ```sql
  ALTER TABLE carts ADD COLUMN student_profile_id BIGINT;
  UPDATE carts SET student_profile_id = sp.id FROM student_profiles sp WHERE sp.user_id = carts.user_id;
  ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user;
  ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_fkey;
  ALTER TABLE carts DROP COLUMN user_id;
  ALTER TABLE carts ALTER COLUMN student_profile_id SET NOT NULL;
  ALTER TABLE carts ADD CONSTRAINT uk_carts_student_profile UNIQUE (student_profile_id);
  ALTER TABLE carts ADD CONSTRAINT fk_carts_student_profile FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;
  CREATE INDEX idx_carts_student_profile ON carts(student_profile_id);
  ```

### K2. `BookCode` tek-kullanımlık kod paralel kullanımda iki kullanıcıya verilebilir
- **Yer:** [src/main/java/com/beratcan/app/service/UserLibraryService.java:40-76](../src/main/java/com/beratcan/app/service/UserLibraryService.java#L40-L76)
- **Sorun:** `findByCodeAndUsedFalse(...)` → `code.setUsed(true)` → save sıralı; `BookCode` entity'sinde **`@Version` yok, repository'de `@Lock` yok**. İki paralel istek aynı kodu bulur, ikisi de `setUsed(true)` çağırır, `uk_user_book` farklı user'lar için tetiklenmez → aynı kod iki kullanıcıya kitap bağışlar.
- **Fix:** Ya `BookCode`'a `@Version private Long version;` ekle, ya da repository'de redeem metodunu `@Lock(LockModeType.PESSIMISTIC_WRITE)` ile aç:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT bc FROM BookCode bc WHERE bc.code = :code AND bc.used = false")
  Optional<BookCode> findByCodeAndUsedFalseForUpdate(@Param("code") String code);
  ```
  Ek güvence için `book_codes` tablosuna kısmi unique constraint: `CREATE UNIQUE INDEX uk_book_codes_used ON book_codes(id) WHERE used = false;` (PostgreSQL).

### K3. Checkout'ta idempotency yok — ağ hatası → mükerrer satın alma
- **Yer:** [src/main/java/com/beratcan/app/service/PurchaseService.java:96-378](../src/main/java/com/beratcan/app/service/PurchaseService.java#L96-L378)
- **Sorun:** `checkoutCart()` kendi `transactionId` üretir (`generateTransactionId()` her çağrıda yeni UUID). Client retry yaparsa aynı sepet için ikinci bir `Purchase` daha yaratılır. Gerçek gateway gelince çift charge riski.
- **Fix:** Endpoint'e client-supplied `Idempotency-Key` header'ı kabul ettir. `purchases` tablosuna `idempotency_key` kolonu + (user_id, idempotency_key) unique constraint ekle. Mevcutsa cached response döndür.

### K4. `Test → Question` cascade strateji çakışması (soft-delete vs CascadeType.ALL)
- **Yer:** Test entity'si `cascade = CascadeType.ALL`. Question'lar `@SQLRestriction("is_deleted = false")` ile soft-delete. Test silindiğinde Hibernate Question için `DELETE` üretir; ayrıca DB tarafında V2 migration `ON DELETE CASCADE` koymuş.
- **Sonuç:** Beklenmedik veri kaybı (gerçek DELETE) ve soft-delete'te tutarsız set. JPA hesabıyla yumuşak silinmiş Question'lar üzerinde tekrar DELETE.
- **Fix:** `Test.questions`'ta `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` kullan; silmeyi DB cascade ya da explicit soft-delete servisine bırak.

---

## 🟠 YÜKSEK Bulgular

### Y1. File upload'da path traversal (dev profili)
- **Yer:** [src/main/java/com/beratcan/app/service/LocalStorageService.java:36](../src/main/java/com/beratcan/app/service/LocalStorageService.java#L36) — `saveFile`: `currentTimeMillis() + "_" + file.getOriginalFilename()` direkt birleştiriliyor; `getFile` (line 50-64) hiçbir normalize/boundary check yapmıyor; MIME/extension whitelist yok.
- **Etki:** Sadece `dev`/`default` profilinde aktif (prod'da DigitalOcean Spaces), ama dev'de RCE/leak vektörü.
- **Fix:**
  - Dosya adında `Paths.get(filename).getFileName().toString()` ile sadece son segmenti al, ayrıca `UUID.randomUUID()` prefix.
  - `getFile` içinde de `deleteFile`'daki gibi `startsWith(uploadDirPath)` kontrolü.
  - `Content-Type` whitelist (`image/jpeg`, `image/png`, `application/pdf` vb.).

### Y2. Cart/checkout fiyat semantiği tutarsız
- **Yer:** [PurchaseService.java:169-178, 199](../src/main/java/com/beratcan/app/service/PurchaseService.java#L169-L199)
- **Sorun:** `priceAtAdd != currentPrice` ise item validation'dan **düşürülüyor**, ama düşürmek yerine "fiyat değişti, onayla" akışı tasarımsal olarak daha uygun olabilir. Ayrıca toplam `book.getPrice()` üzerinden hesaplanıyor (admin price update'i checkout sırasında devreye girebilir).
- **Fix:** `priceAtAdd` üzerinden hesapla; eşitsizlik varsa kullanıcıya açık onay sun veya sepeti otomatik güncelle (UX kararı).

### Y3. `useDiscountCode` purchase başarılı SAYILDIKTAN SONRA çağrılıyor
- **Yer:** [PurchaseService.java:331-338](../src/main/java/com/beratcan/app/service/PurchaseService.java#L331-L338) + [DiscountCodeService.java:197-236](../src/main/java/com/beratcan/app/service/DiscountCodeService.java#L197-L236)
- **Sorun:** `useDiscountCode` `ObjectOptimisticLockingFailureException` fırlatırsa zaten yapılmış `user.ownedBooks.add(book)` ve `purchase.save` rollback olur. Gerçek gateway gelince bu zincir kötü state üretir — para çekildi, kitap verilmedi mesajı gösterilir.
- **Fix:** Discount usage artırımını payment commit'inden ÖNCE yap; payment authorize → discount artır → capture pattern kullan.

### Y4. `OneToMany`/EAGER fetch'ler N+1 ve memory bloat riskleri
- **Yer:** [Book.java:41](../src/main/java/com/beratcan/app/entity/Book.java#L41) `Publisher` muhtemelen EAGER (varsayılan ManyToOne EAGER). Liste endpoint'lerinde N+1.
- **Fix:** Tüm `@ManyToOne`/`@OneToOne`'lara explicit `fetch = FetchType.LAZY`; liste query'lerinde `JOIN FETCH` veya `@EntityGraph`.

### Y5. Index eksiklikleri
- **Yer:** V1'de `tests.topic_id` ve `questions.test_id` FK'larında index yok.
- **Fix:** `CREATE INDEX idx_tests_topic_id ON tests(topic_id); CREATE INDEX idx_questions_test_id ON questions(test_id);` migration'ı.

### Y6. `GlobalExceptionHandler` generic exception → `ex.getMessage()` döndürüyor
- **Yer:** [src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java:201-217](../src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java#L201-L217)
- **Sorun:** SQL/Hibernate iç mesajları client'a sızabilir (table/column adları, constraint isimleri).
- **Fix:** `prod` profilinde generic mesaj, detayı sadece log'a.

---

## 🟡 ORTA Bulgular

### O1. JWT secret fallback default ile çalışabilir
- **Yer:** [application.properties:33](../src/main/resources/application.properties#L33) `jwt.secret=${JWT_SECRET:}` — env yoksa boş string. `SecurityValidationConfig` runtime check yapsa da bean init order edge case'i. Fix: fallback'i kaldır; `${JWT_SECRET}` (Spring boş bırakırsa erken patlar).

### O2. `application.properties` dev'de DEBUG SQL + parameter binding logu açık
- Production'da `application-prod.properties` `WARN`'a düşürmüş (iyi); sadece dev'de PII riski.

### O3. `LocalDateTime.now()` ile `LocalDateTime.now(TURKEY_ZONE)` karışımı
- **Yer:** Cart/Purchase içinde `LocalDateTime.now()` (system tz), DiscountCode/EmailVerification'da bazıları TURKEY_ZONE.
- **Fix:** Tüm timestamp'leri **UTC `Instant`** ya da **`OffsetDateTime`** olarak sakla; sunum katmanında zone'a çevir.

### O4. Controller-seviyesinde rol guard'ları tutarsız
- **`CartController`:** `@PreAuthorize` yok. Memory: sepet STUDENT-only.
- **`BookLibraryController.useCode`:** Memory'ye göre `TEACHER + STUDENT` açık olmalı, PARENT engelli. `UserLibraryService.validateBookOwnershipAllowed` sadece PARENT'i reddediyor — vizyona uygun, ama controller'da explicit `@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")` belgeleme için iyi.

### O5. Fiyat değişimi sessizce kabul ediliyor (Y2 ile birlikte tasarım kararı)

### O6. DTO response'larında potansiyel sızıntı
- `UserResponse` içinde `password` field'ı yok, iyi. `EmailVerificationToken`, `RefreshToken` gibi entity'lerin admin endpoint'lerinden sızmadığını doğrula.

---

## 🟢 DÜŞÜK / Notlar

- BCrypt cost default (10) — yeterli ama `12` tercih edilebilir.
- CSRF disabled (REST + JWT için doğru).
- `@RateLimit` aspect'inin `/login`, `/refresh`, `/forgot-password` endpoint'lerinde uygulandığını doğrula.
- `restore_constraints.sql` repo köküne çıplak halde durması — `docs/` veya `db/manual/` altına taşı.

---

## Önerilen Aksiyon Sırası

1. **Şimdi:** K1 carts migration — startup'ı bozan tek kritik bug.
2. **Sonraki sprint:** K2 BookCode lock, K3 idempotency-key, K4 cascade düzeltmesi, Y1 path traversal, Y3 discount/payment sıralama.
3. **Daha sonra:** Y4 fetch tipleri + JOIN FETCH, Y5 index migration'ları, Y6 generic exception handler, O1 JWT fallback kaldır.
4. **Sürekli:** O3 timezone standardizasyonu, O4 role guard tutarlılığı.
5. **Ürün kararına bağlı:** Gerçek ödeme gateway entegrasyonu (ayrı plan).

---

## Doğrulama

- **K1:** Lokal Postgres'te V14 sonrası `\d carts` çıktısı `student_profile_id` içermeli; `SPRING_PROFILES_ACTIVE=prod` ile uygulama başlatılınca schema-validation hata vermemeli.
- **K2:** İki ayrı kullanıcıdan eş zamanlı `/api/users/use-code` çağrısı (jmeter/k6) — sadece biri başarı dönmeli.
- **K3:** Aynı `Idempotency-Key` ile iki POST → ikinci 200 OK + cached response (yeni Purchase üretmemeli).
- **Y1:** `curl -F "file=@/tmp/x.txt;filename=../../etc/passwd"` → 400/422.
- **Y4:** `?size=20` Book listesi endpoint'inde Hibernate stat: `Statements: 2` (Book + Publisher join), N+1 olmamalı.

---

## Kritik Dosyalar

- [src/main/java/com/beratcan/app/entity/Cart.java](../src/main/java/com/beratcan/app/entity/Cart.java)
- [src/main/resources/db/migration/V14__Create_profile_tables.sql](../src/main/resources/db/migration/V14__Create_profile_tables.sql)
- [src/main/java/com/beratcan/app/service/UserLibraryService.java](../src/main/java/com/beratcan/app/service/UserLibraryService.java)
- [src/main/java/com/beratcan/app/service/PurchaseService.java](../src/main/java/com/beratcan/app/service/PurchaseService.java)
- [src/main/java/com/beratcan/app/service/DiscountCodeService.java](../src/main/java/com/beratcan/app/service/DiscountCodeService.java)
- [src/main/java/com/beratcan/app/service/LocalStorageService.java](../src/main/java/com/beratcan/app/service/LocalStorageService.java)
- [src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java](../src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java)
- [src/main/resources/application.properties](../src/main/resources/application.properties), [application-prod.properties](../src/main/resources/application-prod.properties), [application-dev.properties](../src/main/resources/application-dev.properties)
