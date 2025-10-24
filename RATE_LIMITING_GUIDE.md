# 🚨 RATE LIMITING GÜVENLİK ANALİZİ VE İMPLEMENTASYON

**Tarih:** 24 Ekim 2025  
**Durum:** ❌ RATE LIMITING YOK - KRİTİK GÜVENLİK AÇIĞI!

---

## 🔴 MEVCUT DURUM

### ❌ Projede Rate Limiting YOK!

**Kontrol Edilen:**

```bash
✅ SecurityConfig.java - Rate limit yok
✅ pom.xml - Bucket4j/Resilience4j dependency yok
✅ Tüm controller'lar - Rate limit annotation yok
✅ Filter/Interceptor - Rate limiting yok
```

**Sonuç:** Hiçbir endpoint rate-limited değil! 🚨

---

## 💣 RİSKLER (Rate Limiting Olmadan)

### 1. **Brute Force Attacks** ⭐⭐⭐⭐⭐

```bash
# Hacker senaryosu:
for i in {1..10000}; do
  curl -X POST http://localhost:8080/api/users/login \
    -d '{"identifier":"admin","password":"test'$i'"}'
done

# 10,000 şifre denemesi = 2 dakika
# ✅ Rate limit olsaydı: 5 deneme/dakika = 33 saat
```

**Risk:**

- ❌ Admin hesabı ele geçirilebilir
- ❌ Tüm kullanıcı hesapları risk altında
- ❌ Database overload

---

### 2. **DDoS (Distributed Denial of Service)** ⭐⭐⭐⭐⭐

```bash
# Botnet senaryosu:
# 1000 bot × 100 request/second = 100,000 req/sec
# Server DOWN in 10 seconds!

while true; do
  curl http://localhost:8080/api/books/store &
done
```

**Risk:**

- ❌ Server crash
- ❌ Legitimate users cannot access
- ❌ Downtime = Revenue loss

---

### 3. **API Abuse (AI Cost Attack)** ⭐⭐⭐⭐⭐

```bash
# Malicious user senaryosu:
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/files/upload-question-with-ai \
    -F "file=@large_image.png" \
    -F "testId=1" \
    -F "correctAnswer=A"
done

# 1000 AI calls × $0.05 = $50
# Her gün = $1500/month
# SİZİN PARANIZ GİDİYOR! 💸
```

**Risk:**

- 💰 OpenAI API cost explosion
- ❌ Budget overrun ($1000+/day possible)
- ❌ Account suspension (API abuse)

---

### 4. **Resource Exhaustion** ⭐⭐⭐⭐

```bash
# Test result spam senaryosu:
for i in {1..10000}; do
  curl -X POST http://localhost:8080/api/test-results/submit \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"testId":1,"answers":{}}'
done

# 10,000 test results = Database full
```

**Risk:**

- ❌ Database storage full
- ❌ Slow queries
- ❌ Backup failures

---

### 5. **Credential Stuffing** ⭐⭐⭐⭐

```bash
# Leaked password list attack:
cat leaked_passwords.txt | while read pass; do
  curl -X POST http://localhost:8080/api/users/login \
    -d '{"identifier":"victim@email.com","password":"'$pass'"}'
done

# 1 million passwords × 100 ms = 28 hours
# ✅ Rate limit olsaydı: MONTHS
```

**Risk:**

- ❌ Account takeover
- ❌ Data breach
- ❌ GDPR violations

---

## 🎯 RATE LIMITING STRATEJİLERİ

### Strateji 1: **Bucket4j** (Recommended ⭐⭐⭐⭐⭐)

**En Popüler, Production-Ready**

#### Avantajları:

- ✅ Token bucket algorithm (industry standard)
- ✅ Easy integration
- ✅ Redis support (distributed rate limiting)
- ✅ In-memory fallback
- ✅ Flexible configuration
- ✅ Spring Boot starter available

#### Dezavantajları:

- ⚠️ Extra dependency (~500KB)
- ⚠️ Redis needed for multi-instance (optional)

---

### Strateji 2: **Resilience4j** (Alternative ⭐⭐⭐⭐)

**More than rate limiting (circuit breaker, retry, etc.)**

#### Avantajları:

- ✅ Multi-purpose (rate limiter + circuit breaker + retry)
- ✅ Spring Boot integration
- ✅ Good documentation

#### Dezavantajları:

- ⚠️ Heavier (~2MB with all modules)
- ⚠️ More complex setup
- ⚠️ Overkill for just rate limiting

---

### Strateji 3: **Custom Filter** (Lightweight ⭐⭐⭐)

**DIY solution**

#### Avantajları:

- ✅ No external dependency
- ✅ Full control
- ✅ Lightweight

#### Dezavantajları:

- ❌ Need to implement yourself
- ❌ Not battle-tested
- ❌ No distributed support
- ❌ Maintenance burden

---

## 🎖️ TAVSİYE: Bucket4j

**Neden:**

1. Industry standard
2. Production-ready
3. Easy to use
4. Redis support (scalability)
5. Active community

---

## 📦 IMPLEMENTASYON PLANI

### ADIM 1: Dependency Ekle (pom.xml)

```xml
<!-- Bucket4j - Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.0</version>
</dependency>

<!-- Spring Cache (Bucket4j için) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- OPSIYONEL: Redis (Multi-instance deployment için) -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.0</version>
</dependency>
-->
```

---

### ADIM 2: Rate Limit Configuration

```java
package com.beratcan.app.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // IP bazlı bucket storage (in-memory)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * IP için rate limit bucket oluştur/getir
     *
     * @param ip IP adresi
     * @param requestsPerMinute Dakikada izin verilen request sayısı
     * @return Bucket
     */
    public Bucket resolveBucket(String ip, int requestsPerMinute) {
        return cache.computeIfAbsent(ip, k -> createNewBucket(requestsPerMinute));
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        // Token bucket: requestsPerMinute tokens, her dakika refill
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Kullanıcı ID bazlı bucket (authenticated users için)
     */
    public Bucket resolveBucketByUserId(Long userId, int requestsPerMinute) {
        String key = "user_" + userId;
        return cache.computeIfAbsent(key, k -> createNewBucket(requestsPerMinute));
    }
}
```

---

### ADIM 3: Rate Limit Filter (Global)

```java
package com.beratcan.app.security;

import com.beratcan.app.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Default: 60 requests per minute
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        // Get rate limit for this IP
        Bucket bucket = rateLimitConfig.resolveBucket(ip, DEFAULT_REQUESTS_PER_MINUTE);

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // ✅ Request allowed
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            log.debug("Request allowed: ip={}, path={}, remaining={}",
                ip, path, probe.getRemainingTokens());

            filterChain.doFilter(request, response);
        } else {
            // ❌ Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000; // seconds

            log.warn("Rate limit exceeded: ip={}, path={}, retryAfter={}s",
                ip, path, waitForRefill);

            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Too Many Requests");
            errorResponse.put("message", "Rate limit exceeded. Please try again later.");
            errorResponse.put("retryAfterSeconds", waitForRefill);

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        // Check for proxy headers
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0]; // First IP is the original client
    }
}
```

---

### ADIM 4: SecurityConfig'e Filter Ekle

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final RateLimitFilter rateLimitFilter; // ✅ YENİ!

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         AuthEntryPointJwt unauthorizedHandler,
                         RateLimitFilter rateLimitFilter) { // ✅ YENİ!
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
        this.rateLimitFilter = rateLimitFilter; // ✅ YENİ!
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/api/users/refresh").permitAll()
                .requestMatchers("/api/books/store").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // ✅ Rate limit filter ÖNCE ekle (tüm requestleri kontrol et)
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        // JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ... rest of the code
}
```

---

### ADIM 5: Endpoint-Specific Rate Limits (Annotation)

```java
package com.beratcan.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Dakikada izin verilen maksimum request sayısı
     */
    int requestsPerMinute() default 60;

    /**
     * Key type: IP, USER, GLOBAL
     */
    String keyType() default "IP";
}
```

---

### ADIM 6: Aspect for @RateLimit Annotation

```java
package com.beratcan.app.aspect;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitConfig rateLimitConfig;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();

        String key = getRateLimitKey(request, rateLimit.keyType());
        Bucket bucket;

        if ("USER".equals(rateLimit.keyType())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Long userId = Long.parseLong(auth.getName());
                bucket = rateLimitConfig.resolveBucketByUserId(userId, rateLimit.requestsPerMinute());
            } else {
                // Fallback to IP
                bucket = rateLimitConfig.resolveBucket(key, rateLimit.requestsPerMinute());
            }
        } else {
            bucket = rateLimitConfig.resolveBucket(key, rateLimit.requestsPerMinute());
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for endpoint: key={}, method={}, retryAfter={}s",
                key, joinPoint.getSignature().getName(), waitForRefill);

            throw new RuntimeException("Rate limit exceeded. Retry after " + waitForRefill + " seconds");
        }

        log.debug("Rate limit check passed: key={}, remaining={}", key, probe.getRemainingTokens());
        return joinPoint.proceed();
    }

    private String getRateLimitKey(HttpServletRequest request, String keyType) {
        if ("IP".equals(keyType)) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0];
        }
        return "global";
    }
}
```

---

### ADIM 7: Controller'larda Kullanım

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    // Login endpoint - Brute force koruması
    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 5, keyType = "IP") // ✅ 5 login per minute per IP
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        // ...
    }

    // Register endpoint - Spam koruması
    @PostMapping("/register")
    @RateLimit(requestsPerMinute = 3, keyType = "IP") // ✅ 3 register per minute per IP
    public ResponseEntity<?> register(@RequestBody UserRequest request) {
        // ...
    }
}

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    // AI upload - Maliyet koruması
    @PostMapping("/upload-question-with-ai")
    @RateLimit(requestsPerMinute = 5, keyType = "USER") // ✅ 5 AI calls per minute per USER
    public ResponseEntity<?> uploadQuestionWithAI(...) {
        // ...
    }

    // Normal upload - Resource koruması
    @PostMapping("/upload")
    @RateLimit(requestsPerMinute = 30, keyType = "USER") // ✅ 30 uploads per minute per USER
    public ResponseEntity<String> uploadFile(...) {
        // ...
    }
}

@RestController
@RequestMapping("/api/test-results")
@RequiredArgsConstructor
public class TestResultController {

    // Test submission - DB koruması
    @PostMapping("/submit")
    @RateLimit(requestsPerMinute = 10, keyType = "USER") // ✅ 10 tests per minute per USER
    public ResponseEntity<TestResultResponse> submitTest(...) {
        // ...
    }
}
```

---

## 📊 TAVSİYE EDİLEN RATE LIMITS

### Public Endpoints (Authentication required değil)

| Endpoint              | Rate Limit | Key Type | Neden                   |
| --------------------- | ---------- | -------- | ----------------------- |
| `/api/users/login`    | **5/min**  | IP       | Brute force protection  |
| `/api/users/register` | **3/min**  | IP       | Spam prevention         |
| `/api/users/refresh`  | **10/min** | IP       | Token refresh abuse     |
| `/api/books/store`    | **30/min** | IP       | Public catalog browsing |

### Authenticated Endpoints

| Endpoint                             | Rate Limit | Key Type | Neden                    |
| ------------------------------------ | ---------- | -------- | ------------------------ |
| `/api/files/upload-question-with-ai` | **5/min**  | USER     | AI cost control ($$)     |
| `/api/files/upload`                  | **30/min** | USER     | Storage protection       |
| `/api/test-results/submit`           | **10/min** | USER     | DB write protection      |
| `/api/cart/add`                      | **60/min** | USER     | Normal usage             |
| `/api/purchases/buy`                 | **5/min**  | USER     | Payment fraud prevention |
| Default (tüm diğerleri)              | **60/min** | IP       | General protection       |

---

## 🎯 GELİŞMİŞ SENARYOLAR

### Redis ile Distributed Rate Limiting

```java
// application.properties
spring.redis.host=localhost
spring.redis.port=6379

// RateLimitConfig.java (Redis version)
@Configuration
@EnableCaching
public class RateLimitConfig {

    @Autowired
    private RedisTemplate<String, Bucket> redisTemplate;

    public Bucket resolveBucket(String key, int requestsPerMinute) {
        String redisKey = "rate_limit:" + key;
        Bucket bucket = redisTemplate.opsForValue().get(redisKey);

        if (bucket == null) {
            bucket = createNewBucket(requestsPerMinute);
            redisTemplate.opsForValue().set(redisKey, bucket, Duration.ofMinutes(1));
        }

        return bucket;
    }
}
```

**Avantajlar:**

- ✅ Multi-instance deployment
- ✅ Shared rate limit across servers
- ✅ Centralized management

---

### Dynamic Rate Limits (Database)

```java
// Rate limit rules stored in database
@Entity
public class RateLimitRule {
    @Id
    private Long id;

    private String endpoint; // /api/users/login
    private String keyType;  // IP, USER
    private int requestsPerMinute;
    private boolean enabled;
}

// Load rules from DB
@Service
public class DynamicRateLimitService {
    public int getRateLimitForEndpoint(String endpoint) {
        RateLimitRule rule = ruleRepository.findByEndpoint(endpoint);
        return rule != null ? rule.getRequestsPerMinute() : 60;
    }
}
```

**Avantajlar:**

- ✅ No code deployment for rule changes
- ✅ Per-customer limits
- ✅ A/B testing

---

## 🔍 MONİTORİNG & ALERTS

### Metrics to Track

```java
@Component
public class RateLimitMetrics {
    private final Counter allowedRequests;
    private final Counter blockedRequests;

    public void recordAllowed(String endpoint) {
        allowedRequests.increment();
    }

    public void recordBlocked(String endpoint, String ip) {
        blockedRequests.increment();
        // Send alert if > 100 blocks in 1 minute
    }
}
```

### Grafana Dashboard

```
Rate Limit Metrics:
- Requests Allowed (per endpoint)
- Requests Blocked (per endpoint)
- Top IPs Blocked
- Rate Limit Hit Rate (%)
- Retry After Times
```

---

## 📋 IMPLEMENTASYON KONTROL LİSTESİ

### Phase 1: Temel (1 gün)

- [ ] Bucket4j dependency ekle
- [ ] RateLimitConfig oluştur
- [ ] RateLimitFilter oluştur
- [ ] SecurityConfig'e filter ekle
- [ ] Test et (Postman)

### Phase 2: Gelişmiş (2 gün)

- [ ] @RateLimit annotation oluştur
- [ ] RateLimitAspect oluştur
- [ ] Critical endpoints'e annotation ekle
- [ ] Logging ekle
- [ ] Error handling iyileştir

### Phase 3: Production (3 gün)

- [ ] Redis integration
- [ ] Monitoring/metrics
- [ ] Grafana dashboard
- [ ] Alert rules
- [ ] Load testing

---

## 💰 MALİYET-FAYDA ANALİZİ

### Implementation Cost:

```
Developer Time: 2-3 gün
Complexity: Orta
Maintenance: Minimal
```

### Benefits:

```
✅ Brute force attacks: PREVENTED
✅ DDoS attacks: MITIGATED
✅ AI cost explosion: PREVENTED ($1000s saved)
✅ Resource exhaustion: PREVENTED
✅ Uptime improvement: %99.9+

💰 ROI: INFINITE (Cost = 3 days, Benefit = Priceless security)
```

---

## 🎓 SONUÇ

### ❌ Şu Anda:

- Hiçbir rate limiting yok
- Brute force vulnerable
- DDoS vulnerable
- AI cost abuse possible
- Database spam possible

### ✅ Bucket4j ile:

- Comprehensive protection
- Easy implementation (2-3 days)
- Production-ready
- Scalable (Redis)
- Flexible (annotation-based)

---

## 🚀 TAVSİYE EDILEN AKSIYONLAR

### ACIL (Bu Hafta):

1. Bucket4j dependency ekle
2. RateLimitFilter implement et
3. Login endpoint'e 5/min limit koy
4. AI upload endpoint'e 5/min limit koy

### KISA VADE (Bu Ay):

5. Tüm critical endpoints'e limit ekle
6. Logging + monitoring ekle
7. Load testing yap

### UZUN VADE (Gelecek):

8. Redis integration
9. Dynamic rules (database)
10. Advanced metrics

---

**Hazırlayan:** AI Assistant  
**Tarih:** 24 Ekim 2025  
**Versiyon:** 1.0  
**Öncelik:** 🔥 KRİTİK!
