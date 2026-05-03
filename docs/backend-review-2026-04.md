# Backend Kod İnceleme Raporu — `app/`

> İlk inceleme: 2026-04-26 · Son güncelleme: 2026-05-03 · Kapsam: `app/src/main/java` + Flyway migration + application properties

## Context

`app/` altındaki Spring Boot backend güvenlik, iş mantığı, JPA/migration ve doğrulama açılarından incelendi. İlk raporun **tüm kritik, yüksek, orta ve düşük öncelikli bulguları kapatıldı** (K1-K4, Y1-Y6, O1, O3, O4, O6). Bu sürüm tamamlanmış denetimin özeti.

> **Not — Ödeme gateway:** `processPayment()` stub'u bilinçli iş kararı; gerçek gateway entegrasyonu rapor kapsamı dışında.

---

## Tamamlanan Bulgular (özet)

| ID | Bulgu | Çözüm |
|----|-------|-------|
| K1 | `carts` student_profile_id geçişi | V17 + V20 migration ile şema eşitlendi, entity & repository güncel |
| K2 | `BookCode` race condition | `BookCodeRepository.findByCodeAndUsedFalseForUpdate` + `@Lock(PESSIMISTIC_WRITE)` |
| K3 | Checkout idempotency | `Idempotency-Key` header, `Purchase.idempotencyKey`, V21 unique index, controller & service akışı |
| K4 | Test→Question cascade | `Test.questions` artık `{PERSIST, MERGE}`; soft-delete `@SQLDelete` + `@SQLRestriction` |
| Y1 | LocalStorage path traversal | Filename basename + UUID prefix, MIME whitelist, `resolveSafe()` boundary check |
| Y3 | Discount/payment sıralama | `useDiscountCode` aynı transaction içinde payment sonrası, atomic rollback |
| Y4 | `Book.publisher` EAGER → LAZY | `FetchType.LAZY` + iki paginated query'de `@EntityGraph(attributePaths = "publisher")` |
| Y5 | FK index eksiklikleri (`tests.topic_id`, `questions.test_id`) | V22 migration ile her ikisi için index eklendi |
| Y6 | GlobalExceptionHandler sızıntısı | Prod profilinde generic mesaj, dev'de detaylı |
| O1 | `JWT_SECRET` boş fallback'i | `application.properties`'te `${JWT_SECRET}` (no fallback) + `SecurityValidationConfig` sadeleştirildi |
| O3 | Timezone karışıklığı | Tek `AppTime.now()` (Europe/Istanbul) constants paketinde; tüm service+entity+validator+exception handler buna geçti; `spring.jackson.time-zone=Europe/Istanbul` eklendi |
| Y2 | Cart/checkout fiyat semantiği | `PriceChangedException` → 409 (PRICE_CHANGED + changedItems payload); checkout total artık `priceAtAdd` üzerinden hesaplanıyor; yeni `POST /api/cart/refresh-prices` endpoint'i ile client değişen fiyatları onaylayıp checkout'u tekrarlıyor |
| O4 | Controller-seviye role guard | `CartController` class-level `@PreAuthorize("hasRole('STUDENT')")`; `BookLibraryController` class-level `@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")` + manuel auth-null check'leri kaldırıldı (Spring Security global authenticated() zaten sağlıyor) |
| O6 | Admin DTO sızıntısı | Tüm 4 admin controller (User/Book/DiscountCode/Purchase) yalnızca `*Response` DTO döndürüyor; `User`/`EmailVerificationToken`/`RefreshToken` gibi entity'ler raw response'da yok — denetim sonucu **temiz** |
| Cosmetic | `restore_constraints.sql` taşıma | Repo kökünden `app/db/manual/` altına taşındı |

---

## 🟢 Düşük Öncelik / Notlar (sadece bilgi)

- **BCrypt cost** default `10` — yeterli; `12`'ye çıkarmak isteğe bağlı (login latency ~4x artar, ek saldırı vektörü yok).

---

## İleri Adım Notları

- **Frontend Y2 entegrasyonu:** Checkout 409 (`error: "PRICE_CHANGED"`) alınca `changedItems`'ı modal'da göster → kullanıcı onayı → `POST /api/cart/refresh-prices` çağır → yeni `Idempotency-Key` ile checkout'u tekrarla.
- **O3 ileri adım:** `LocalDateTime` (timestamp without time zone) sütunları korunuyor. İleride coğrafi genişleme olursa `Instant` + `timestamptz` migration'ı düşünülebilir; bu rapor kapsamı dışı.

---

## Kritik Dosyalar

- [src/main/java/com/beratcan/app/entity/Book.java](../src/main/java/com/beratcan/app/entity/Book.java)
- [src/main/java/com/beratcan/app/service/PurchaseService.java](../src/main/java/com/beratcan/app/service/PurchaseService.java)
- [src/main/resources/application.properties](../src/main/resources/application.properties)
- [src/main/resources/db/migration/V1__Initial_schema.sql](../src/main/resources/db/migration/V1__Initial_schema.sql)
