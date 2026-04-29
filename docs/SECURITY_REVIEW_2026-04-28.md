# Backend Güvenlik Denetim Raporu

**Tarih:** 2026-04-28
**Kapsam:** `app/src/main/java/com/beratcan/app/` ve `app/src/main/resources/`
**Yöntem:** Statik kod analizi (manuel + ajan destekli) — runtime testi yapılmadı.

> Not: `/security-review` skili git üzerine kurulu, bu dizin git repo değil. Bu yüzden manuel tarama yapıldı.

---

## Özet Tablo

| # | Bulgu | Önem | Konum |
|---|---|---|---|
| 1 | Path Traversal — `getFile()` | **Kritik** | [LocalStorageService.java:50-64](../src/main/java/com/beratcan/app/service/LocalStorageService.java#L50-L64) |
| 2 | OTP üretiminde `java.util.Random` (zayıf RNG) | **Yüksek** | [PasswordResetService.java:19,49](../src/main/java/com/beratcan/app/service/PasswordResetService.java#L49) |
| 3 | `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL` — thread-pool sızıntısı | **Yüksek** | [SecurityConfig.java:55-58](../src/main/java/com/beratcan/app/security/SecurityConfig.java#L55-L58) |
| 4 | `saveFile()` — orijinal dosya adında path traversal + MIME doğrulama eksik | **Yüksek** | [LocalStorageService.java:28-47](../src/main/java/com/beratcan/app/service/LocalStorageService.java#L28-L47) |
| 5 | `RateLimitConfig` cache unbounded — DoS / OOM | **Orta** | [RateLimitConfig.java](../src/main/java/com/beratcan/app/config/RateLimitConfig.java) |
| 6 | Generic exception handler `ex.getMessage()`'ı prod'da da döndürüyor | **Orta** | [GlobalExceptionHandler.java:201-216](../src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java#L201-L216) |
| 7 | Swagger UI dev'de `permitAll()` — env yanlış set olursa prod'a sızar | **Orta** | [SecurityConfig.java:69-75](../src/main/java/com/beratcan/app/security/SecurityConfig.java#L69-L75) |
| 8 | `application.properties` boş `JWT_SECRET` default'una izin veriyor | **Orta** | [application.properties:33](../src/main/resources/application.properties#L33) |
| 9 | Audio yüklemede 10MB limit + bandwidth DoS yüzeyi | **Düşük** | [application.properties:56-57](../src/main/resources/application.properties#L56-L57) |

---

## 1. Path Traversal — `getFile()`  ⛔ Kritik

**Konum:** [LocalStorageService.java:50-64](../src/main/java/com/beratcan/app/service/LocalStorageService.java#L50-L64)

```java
public Resource getFile(String filename) {
    Path filePath = Paths.get(uploadDir + filename);   // ← normalize()/startsWith() YOK
    Resource resource = new UrlResource(filePath.toUri());
    ...
}
```

**Sorun:** `deleteFile()` ve `deleteOldFiles()` `normalize() + startsWith(uploadDir)` kontrolü yapıyor; **`getFile()` yapmıyor**. `filename` parametresi `../../../etc/passwd` veya `../application-prod.properties` olarak gönderilebilir.

**Etki:** Sunucudaki herhangi bir dosyaya okuma erişimi (config, secret, kaynak kod, sistem dosyaları). Anonim ya da düşük yetkili kullanıcı, Files endpoint'i üzerinden config'i çekip JWT secret/DB credential'ı alabilir.

**Sömürü örneği:**
```
GET /api/files/..%2F..%2F..%2Fetc%2Fpasswd
GET /api/files/..%2Fapplication-prod.properties
```

**Düzeltme:**
```java
public Resource getFile(String filename) {
    if (filename == null || filename.isBlank()) {
        throw new FileStorageException("Dosya adı boş olamaz");
    }
    Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    Path filePath = uploadRoot.resolve(filename).normalize();
    if (!filePath.startsWith(uploadRoot)) {
        log.warn("Path traversal attempt: {}", filename);
        throw new FileStorageException("Geçersiz dosya yolu");
    }
    ...
}
```

Aynı düzeltme `saveFile(String path, byte[], String)` (satır 100-116) için de gerekli — orada `normalize()` var ama `startsWith()` kontrolü yok.

---

## 2. OTP üretiminde `java.util.Random`  ⚠️ Yüksek

**Konum:** [PasswordResetService.java:49](../src/main/java/com/beratcan/app/service/PasswordResetService.java#L49)

```java
import java.util.Random;
...
String code = String.format("%06d", new Random().nextInt(1_000_000));
```

**Sorun:** `java.util.Random` kriptografik açıdan güvensiz; seed (System.nanoTime) gözlemlenirse sonraki OTP'ler tahmin edilebilir. Her çağrıda **yeni** Random nesnesi yaratılması seed'i `System.nanoTime` zaman damgasına bağladığı için aynı saniyede çağrılan iki istekte tahmin edilebilirliği daha da artırıyor.

**Etki:** Şifre sıfırlama OTP'lerinin (5 dk geçerli) tahmin edilmesi → hesap ele geçirme. Rate limit (10/dk) brute force'u tek başına engellemeye yetmez (1M aralık, 5 dk pencere içinde paralel atak).

**Düzeltme:**
```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();
...
String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
```

**Ek öneri:** Yanlış OTP girişlerinde **sayaç** tut, 5 yanlış denemeden sonra OTP'yi geçersiz kıl ve kullanıcıya yeni kod isteme zorunluluğu getir. `verifyResetCode` şu an saymıyor.

---

## 3. `MODE_INHERITABLETHREADLOCAL` — SecurityContext thread-pool sızıntısı  ⚠️ Yüksek

**Konum:** [SecurityConfig.java:55-58](../src/main/java/com/beratcan/app/security/SecurityConfig.java#L55-L58)

```java
@PostConstruct
public void enableSecurityContextInheritance() {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
}
```

**Sorun:** `INHERITABLETHREADLOCAL`, child thread'in oluşturulduğu **anda parent thread'in SecurityContext'ini kopyalar**. Spring'in async executor'ları (TaskExecutor, @Async, ScheduledExecutor) thread'leri **yeniden kullanır** — yani:

1. Kullanıcı A'nın isteği işlenirken bir thread havuzunda yeni thread doğar; Authentication=A kopyalanır.
2. Kullanıcı A'nın isteği biter, thread havuza geri döner ama SecurityContext **temizlenmez**.
3. Sonraki istekte thread Kullanıcı B için kullanılır, ama SecurityContext hâlâ A.

**Etki:** Privilege escalation, başka kullanıcı kimliği altında işlem yürütme, audit log'ların yanlış kullanıcıya yazılması. SSE/Async kontrolcüsü olan her yerde aktif risk.

**Düzeltme önerisi:** `MODE_INHERITABLETHREADLOCAL`'ı kaldır. SSE için Spring'in **`DelegatingSecurityContextRunnable`** / **`DelegatingSecurityContextExecutor`** sarmalayıcısını kullan veya çağrı bitiminde explicit `SecurityContextHolder.clearContext()` çağır. Async desteği için `@EnableAsync` + `DelegatingSecurityContextAsyncTaskExecutor`.

```java
@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
    delegate.initialize();
    return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
}
```

---

## 4. `saveFile()` — orijinal dosya adı + MIME doğrulama eksik  ⚠️ Yüksek

**Konum:** [LocalStorageService.java:28-47](../src/main/java/com/beratcan/app/service/LocalStorageService.java#L28-L47)

```java
String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
Path filePath = Paths.get(uploadDir + filename);   // normalize/startsWith YOK
Files.createDirectories(filePath.getParent());
Files.write(filePath, file.getBytes());
```

**Sorunlar (üç ayrı):**

1. **Path traversal:** `getOriginalFilename()` → client kontrolünde. `../../webshell.jsp` gibi bir isim `uploadDir` dışına yazma yapabilir.
2. **MIME / extension whitelist yok:** `.jsp`, `.php`, `.sh`, `.html` (XSS için) yüklenebilir. Hangi dizine yazıldığına ve servlet container'ın yorumlamasına bağlı olarak RCE.
3. **`getBytes()` tüm dosyayı belleğe alıyor:** Büyük dosyalarda OOM. `InputStream` + `Files.copy()` daha güvenli.

**Düzeltme:**
```java
public String saveFile(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new FileStorageException("Dosya boş olamaz");

    String contentType = file.getContentType();
    if (!ALLOWED_MIME.contains(contentType)) {
        throw new FileStorageException("Desteklenmeyen dosya türü");
    }
    String ext = getSafeExtension(file.getOriginalFilename()); // whitelist kontrolü
    String safeName = System.currentTimeMillis() + "_" + UUID.randomUUID() + "." + ext;

    Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    Path filePath = uploadRoot.resolve(safeName).normalize();
    if (!filePath.startsWith(uploadRoot)) {
        throw new FileStorageException("Geçersiz dosya yolu");
    }
    Files.createDirectories(uploadRoot);
    try (InputStream in = file.getInputStream()) {
        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
    }
    return safeName;
}
```

`getOriginalFilename()` **asla** doğrudan disk'e yazılmamalı — sadece kayıt amacıyla DB'de saklanabilir.

---

## 5. RateLimitConfig — cache unbounded  ⚠️ Orta

**Konum:** [RateLimitConfig.java](../src/main/java/com/beratcan/app/config/RateLimitConfig.java)

`ConcurrentHashMap<String, Bucket> cache` IP başına entry üretiyor; eviction yok.

**Etki:** Saldırgan random IP header'larla (X-Forwarded-For spoof eğer trust ediliyorsa) veya IPv6 çeşitlenmesiyle haritayı şişirip OOM'a sürükleyebilir.

**Düzeltme:** Caffeine cache (zaten dependency'de büyük ihtimal var; yoksa eklenebilir):

```java
private final Cache<String, Bucket> cache = Caffeine.newBuilder()
    .maximumSize(50_000)
    .expireAfterAccess(Duration.ofHours(1))
    .build();
```

---

## 6. Generic Exception Handler — internal mesajları prod'a sızdırıyor  ⚠️ Orta

**Konum:** [GlobalExceptionHandler.java:201-216](../src/main/java/com/beratcan/app/exception/GlobalExceptionHandler.java#L201-L216)

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorDetails> handleGenericException(...) {
    ErrorDetails errorDetails = buildErrorDetails(
            "An unexpected error occurred",
            ex.getMessage(),                   // ← prod'da da raw mesaj
            HttpStatus.INTERNAL_SERVER_ERROR);
    ...
}
```

**Etki:** SQL hata mesajları (sütun/tablo adları), file system hataları (yollar), Hibernate stack izleri istemciye sızar; saldırganın iç şemayı haritalandırmasını kolaylaştırır.

**Düzeltme:** Prod profile'da `details` alanını generic bir mesaj olarak döndür, log'a `ex.getMessage()` yazmaya devam et:

```java
@Value("${spring.profiles.active:prod}")
private String activeProfile;

String details = "prod".equals(activeProfile) ? "Internal server error" : ex.getMessage();
```

`AuthEntryPointJwt` zaten profile-aware davranıyor — aynı pattern uygulanmalı.

---

## 7. Swagger / OpenAPI — dev'de `permitAll()`  ⚠️ Orta

**Konum:** [SecurityConfig.java:69-75](../src/main/java/com/beratcan/app/security/SecurityConfig.java#L69-L75)

`activeProfile` `prod` değilse Swagger anonim açık. `application.properties` defaultu `dev`. `SPRING_PROFILES_ACTIVE` env yanlış set edilirse (veya unset kalırsa) prod imajda da açılır → tüm endpoint listesi, parametre adları sızar.

**Düzeltme:** Prod için ek defansif önlem — Swagger'ı tamamen kapat:

```properties
# application-prod.properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

ve startup'ta `prod` profile zorunluluğu (Dockerfile/docker-compose'da `SPRING_PROFILES_ACTIVE=prod` set'i doğrula).

---

## 8. JWT secret default boş  ⚠️ Orta

**Konum:** [application.properties:33](../src/main/resources/application.properties#L33)

```properties
jwt.secret=${JWT_SECRET:}
```

`JWT_SECRET` env yoksa boş string ile başlar. JWT kütüphanesi bunu **reddetmeyebilir** (HMAC için zayıf anahtar olarak kullanır) → token forgery.

**Düzeltme:** `@PostConstruct` startup-time validation:

```java
@PostConstruct
public void validate() {
    if (jwtSecret == null || jwtSecret.length() < 32) {
        throw new IllegalStateException("JWT_SECRET must be at least 32 chars");
    }
}
```

---

## 9. Upload boyut limitleri  ℹ️ Düşük

`audio.max.size.mb=10` + 10 req/dk rate limit. 100MB/dk × kullanıcı sayısı → bandwidth/disk DoS yüzeyi var ama mevcut savunmalar makul. Audio için daha sıkı per-IP rate limit + disk kotası önerilir.

---

## Pozitif Bulgular (Güvenli Tasarım)

- ✅ JWT validation katmanlı (session ID, blacklist, expiry kontrolü)
- ✅ `@PreAuthorize` ile method-level security tutarlı
- ✅ Role hierarchy (`SUPER_ADMIN > ADMIN`) tanımlı
- ✅ Password hashing: BCrypt
- ✅ Forgot-password akışında **user enumeration koruması** var ([PasswordResetService.java:42-45](../src/main/java/com/beratcan/app/service/PasswordResetService.java#L42-L45))
- ✅ Reset token → UUID (kriptografik olarak güvenli; `Math.random` değil)
- ✅ Şifre sıfırlamada `setActiveSessionId(null)` ile mevcut oturumlar geçersiz kılınıyor
- ✅ Session creation policy STATELESS, CSRF disable bilinçli (REST + JWT)
- ✅ JPA query'lerde native string concat yok (SQL injection direnci)
- ✅ `deleteFile()` ve `deleteOldFiles()` path traversal'a karşı korumalı (sadece `getFile()` ve `saveFile()` eksik)
- ✅ CORS prod profile'ında origin whitelist'e geçiyor; dev'de `allowCredentials(false)` ile wildcard güvenli kullanım

---

## Önerilen Yama Sırası

1. **Hemen:** #1 (path traversal getFile), #4 (saveFile path + MIME), #2 (SecureRandom OTP) → public attack surface
2. **Bu sprint:** #3 (MODE_INHERITABLETHREADLOCAL), #6 (exception detail leak), #8 (JWT secret validation)
3. **Yakın zamanda:** #5 (cache eviction), #7 (Swagger prod-off)
4. **Backlog:** Security headers (HSTS, X-Frame-Options, CSP), 2FA admin için, audit log immutability

---

## Yapılan Doğrulamalar

Aşağıdaki dosyalar manuel olarak okunup bulgular **kod düzeyinde** doğrulandı:

- ✅ [LocalStorageService.java](../src/main/java/com/beratcan/app/service/LocalStorageService.java) — getFile vs deleteFile karşılaştırması yapıldı
- ✅ [PasswordResetService.java](../src/main/java/com/beratcan/app/service/PasswordResetService.java) — `import java.util.Random` ve `new Random()` çağrısı doğrulandı
- ✅ [SecurityConfig.java](../src/main/java/com/beratcan/app/security/SecurityConfig.java) — `MODE_INHERITABLETHREADLOCAL` ve CORS profile mantığı doğrulandı

Diğer bulgular (RateLimitConfig, GlobalExceptionHandler, application.properties) ajan taramasından geldi; uygulamadan önce ilgili dosyalar bir kez daha okunup teyit edilmeli.
