# Backend Denetim Raporu – 15 Şubat 2025

## 1. Özet
- **Kapsam:** Spring Boot backend (`com.beratcan.app`) ve destekleyici bileşenler (rate limit, indirim kodları, profil yönetimi, dosya yükleme, ödeme simülasyonu).
- **Yöntem:** Controller, service, DTO, config ve ortak yardımcı sınıflar incelendi; mevcut TODO listesi (`YAPILMASI_GEREKENLER.md`) ile karşılaştırıldı.
- **Genel durum:** Çekirdek özellikler çalışıyor ancak input doğrulama, güvenlik sertleştirmesi, dayanıklılık ve gözlemlenebilirlikte kritik boşluklar var. Yeni eklenen modüller (indirim kodları, enum tabanlı profil) tamamlanma aşamasında ama tam entegrasyon bekliyor.

## 2. Kritik Bulgular (Öncelik: Hemen)

### 2.1 DTO Validasyonu Eksikliği
- **Nerede:** `UserRequest`, `BookRequest`, `PurchaseRequest`, `DiscountCodeRequest` vb.
- **Sorun:** Alanlarda `@NotBlank`, `@Email`, `@Positive` benzeri constraint’ler yok; controller’larda `@Valid` nadiren kullanılmış. Servislerde manuel validasyon tekrarları var.
- **Çözüm:** DTO alanlarına Bean Validation anotasyonları ekle; controller parametrelerine `@Valid` koy; `MethodArgumentNotValidException` için mevcut global handler’ı kullan.

### 2.2 Dosya Yükleme Güvenliği
- **Nerede:** `FileService.saveFile`, `FileController.uploadFile`
- **Risk:** MIME/uzantı beyaz listesi yok, boyut limiti yok, AV taraması yok; orijinal dosya adları direkt kullanılıyor; yetim dosya temizliği yok.
- **Çözüm:** `FileValidationUtil` ile boyut/uzantı kontrolü; UUID tabanlı dosya adı; gerekiyorsa checksum/AV; dosya ilişkilerini track eden metadata tablosu ve temizlik mekanizması.

### 2.3 Ödeme Simülasyonu Her Zaman Başarılı
- **Nerede:** `PurchaseService.processPayment()`
- **Sorun:** Her zaman `true` döndürüyor; başarısız senaryolar test edilmiyor; gateway soyutlaması yok.
- **Çözüm:** `PaymentStrategy` arayüzü (mock + gerçek); transaction ID bazlı logging; başarısızlıkları kullanıcıya ilet.

### 2.4 PII (Kişisel Veri) Loglama
- **Nerede:** `UserService.registerUser` gibi yerlerde email/telefon düz metin loglanıyor.
- **Risk:** KVKK/GDPR ihlali, log sızıntısı.
- **Çözüm:** `LogSanitizer` utility ile maskelenmiş log (örn. `ab***@mail.com`); structured logging + MDC kullan.

## 3. Yüksek Öncelikli İyileştirmeler (2–4 Hafta)

1. **Rate Limit Stratejisi:** `RateLimitFilter` default (IP başına 60/dk) BookStore endpointlerini engelliyor. `/api/books/*` için özel limit ekle veya `@RateLimit` anotasyonu kullan.
2. **Harici Servis Dayanıklılığı:** `AIQuestionAnalyzer` ve gelecekteki S3/ödeme entegrasyonları için Resilience4j (`@Retry`, `@CircuitBreaker`, latency/cost loglama) ekle.
3. **İndirim Kodu Admin Süreci:** Controller tekrar full CRUD destekliyor; FE entegrasyonu, Swagger dokümantasyonu ve kullanım raporu endpointleri ekle.
4. **Gözlemlenebilirlik:** `TestResultService`, `PurchaseService`, `BookCatalogService` gibi kritik flow’larda log yok. İşlem başlangıç/bitiş ve hata logları ekle; Prometheus/Micrometer ile metrik çıkar.
5. **CORS Sertleştirmesi:** `allowedHeaders("*")` yerine env bazlı whitelist ayarla.
6. **Dosya Yaşam Döngüsü:** Upload klasöründe yetim dosyalar birikiyor. Entity–dosya eşleştirmesi ve zamanlanmış temizlik planla.

## 4. Orta Öncelikli Geliştirmeler

| Alan | Sorun | Öneri |
|------|-------|-------|
| Query Performansı | `CartService.mapToResponse` gibi fonksiyonlarda N+1 sorgu | `@EntityGraph` ve `JOIN FETCH` ile ilişkileri eager biçimde çek |
| Enum Kullanımı | Bazı DTO’lar hâlâ string (örn. `UserRequest`) | `UpdateAcademicInfoRequest` örneğindeki gibi Enum’a çevir ve custom validator kullan |
| Test Açığı | Sadece `AppApplicationTests` var | Service seviyesinde unit testler + controller integration testleri ekle |
| Güvenlik Header’ları | CSP/HSTS yok | `HttpSecurity.headers()` ile best practice ekle |
| Scheduler Gözlemi | `DailyQuestionScheduler` log/metrik üretmiyor | Başlangıç/bitiş logları, hatada alarm ekle |
| Discount Analytics | Aktif/pasif kod görünürlüğü yok | Admin kullanım raporları, kullanıcı başına kalan hak endpointleri ekle |
| Profil Validasyonu | DTO enum’a alındı ama controller’da `@Valid` kaldırıldı | `@Valid` tekrar eklenmeli; validatorlar devreye girmiyor |

## 5. Yeni Özellik Fikirleri
1. **Kullanıcıya açık indirim kodu listesi:** uygunsa aktif kodları listeme servisi.
2. **Referral/Gift kodları:** `DiscountCode` modeline `ownerUserId`, friend referral mantığı.
3. **Audit Log:** Profil güncelleme, indirim kullanımı, dosya yükleme gibi olayları `AuditLog` tablosunda izle.
4. **Search caching & text search:** Kitap aramasında Redis + esnek metin arama (Elasticsearch) entegrasyonu.
5. **Rate limit dashboard:** Admin için token durumlarını gösteren endpoint/panel.
6. **Migration otomasyonu:** Flyway/Liquibase ile index, constraint ve schema değişikliklerini yönet.
7. **Domain event mimarisi:** Test tamamlama, satın alma, indirim kullanımı gibi olayları event bus’a yayınla (notification/analytics için).

## 6. Sonuç
Öncelikli aksiyonlar: DTO validation (özellikle `@Valid` + constraint’ler), güvenli dosya yükleme, ödeme akışının gerçeğe uygun hale getirilmesi ve PII loglamanın sanitize edilmesi. Ardından rate limit, resilience, logging ve CORS gibi iyileştirmeler tamamlanmalı. Orta vadede query optimizasyonu, test kapsamı ve yeni raporlama/analitik özellikleriyle platform sürdürülebilir hale getirilebilir. `BACKEND_AUDIT_2025-02-15.md` İngilizce versiyonuyla senkronize edilerek, alınan aksiyonlar kanban/TODO listesinde takip edilmelidir.

