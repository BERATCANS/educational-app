# Backend Audit Report – 15 Feb 2025

## 1. Executive Summary

- **Scope:** Spring Boot backend (`com.beratcan.app`) plus supporting services (rate limiting, discount codes, profile management, file uploads, payment simulation).
- **Method:** Static review of controllers, services, DTOs, configuration, and shared utilities; cross-referenced with current TODO inventory (`YAPILMASI_GEREKENLER.md`).
- **Overall posture:** Functional core features exist, but critical gaps remain in input validation, security hardening, resilience and observability. Several recently added modules (discount codes, profile enums) still need glue work (validation activation, admin listing).

## 2. Critical Findings (fix ASAP)

### 2.1 DTO Validation Coverage (High)

- **Where:** Most request DTOs (e.g. `UserRequest`, `BookRequest`, `PurchaseRequest`, `DiscountCodeRequest`) lack `@NotBlank`, `@Email`, `@Positive` etc. Controllers rarely use `@Valid` (e.g. `UserController.register`, `BookStoreController`, `PurchaseController`).
- **Risk:** Malformed payloads reach services, leading to silent truncation, constraint violations, or security bypass. Manual validation is duplicated in services (e.g. `UserService.registerUser`), increasing bug surface.
- **Remediation:**
  1. Annotate DTO fields with Bean Validation constraints.
  2. Add `@Valid` to controller method parameters (regressed in `ProfileController.updateAcademicInfo` and `updateProfileDetails`).
  3. Rely on `MethodArgumentNotValidException` for response consistency (already handled in `GlobalExceptionHandler`).

### 2.2 File Upload Attack Surface (High)

- **Where:** `FileService.saveFile`, `FileController.uploadFile`.
- **Issues:** No MIME/type whitelist, size limit, or antivirus scan; filenames use unescaped `originalFilename`; duplicate uploads lack dedupe; uploads folder accessible without clean-up.
- **Impact:** RCE / storage abuse risk.
- **Fix:** Introduce `FileValidationUtil` with size cap, allowed extensions, optional checksum scanning; sanitize filename via UUID; store metadata for later purge; schedule clean-up for orphan files.

### 2.3 Payment Simulation Always Succeeds (High)

- **Where:** `PurchaseService.processPayment` (still TODO).
- **Problem:** Returns `true` unconditionally; failure paths never tested; no payment strategy/abstraction.
- **Impact:** Any payment attempt is marked as paid even if upstream gateway is down.
- **Fix:** Implement `PaymentStrategy` interface (mock + production), log transaction IDs, and propagate failures so checkout can abort with clear messaging.

### 2.4 PII Logging (High)

- **Where:** `UserService.registerUser` logs raw email/phone on duplicates; `AuthService` logs usernames and error messages verbatim.
- **Risk:** GDPR/privacy violations, log tampering.
- **Fix:** Introduce `LogSanitizer` utility to mask sensitive strings (e.g. `ab***@domain.com`, `***1234`). Use structured logging with MDC for correlation instead of plaintext PII.

## 3. High Priority Improvements (2–4 weeks)

### 3.1 Rate Limiting Strategy

- **Issue:** `RateLimitFilter` default (60 req/min per IP) throttles `/api/books/store` and `/api/books/filters`, causing UX issues; BookStore endpoints lack bespoke limits and per-user buckets.
- **Action:** Add explicit entries in `getRateLimitForPath` for store endpoints (higher limit and USER-based), or annotate with `@RateLimit` to bypass filter and use tuned values. Consider burst tokens for read-heavy endpoints.

### 3.2 AI & External Resilience

- **Where:** `AIQuestionAnalyzer`, `FileService` network calls (future S3), payment stub.
- **Gap:** No retry/backoff/circuit breaker as flagged in TODO; errors bubble directly to users.
- **Fix:** Add Resilience4j (`@Retry`, `@CircuitBreaker`), log latency/cost metrics, provide fallback responses.

### 3.3 Discount Code Admin UX & Validation

- **Status:** New `DiscountCodeAdminService` handles CRUD with robust validation, but `DiscountCodeAdminController` previously regressed to create-only; now restored but needs FE integration and swagger docs.
- **Follow-ups:** Ensure `DiscountCodeUsage` has unique constraint `(discount_code_id, user_id, transaction_id)` to prevent double counting; add pagination docs; consider filtering by status/expiry.

### 3.4 Observability & Monitoring

- **Issue:** Minimal logging in core flows (`TestResultService`, `PurchaseService`, `BookCatalogService`); no metrics/tracing.
- **Plan:** Add structured logs for business events (test submission, purchase success/failure, rate-limit hits). Integrate Micrometer/Prometheus for request & bucket metrics.

### 3.5 CORS Hardening

- **Current:** `SecurityConfig` uses `allowedHeaders("*")` & `allowedMethods("*")`.
- **Risk:** Wildcards acceptable in dev but not prod.
- **Fix:** Limit allowed headers/methods/origins per env; externalize via config.

### 3.6 Upload Lifecycle Management

- **Observation:** `uploads/` keeps stale files indefinitely; no association table.
- **Action:** Track file references (entity → filename), add scheduled job to purge orphan files older than N days, optionally move to object storage with signed URLs.

## 4. Medium Priority Opportunities

| Area                   | Observation                                                                               | Recommendation                                                                                                         |
| ---------------------- | ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Data Layer             | `CartService.mapToResponse` lazily fetches `item.getBook()` per item causing N+1 queries. | Use `@EntityGraph(attributePaths = {"items", "items.book", "items.book.publisher"})` on `CartRepository#findByUserId`. |
| DTO Consistency        | Many DTOs still use `String` for enums (`UserRequest`, `BookRequest`).                    | Gradually convert to strongly-typed enums + custom validators similar to `UpdateAcademicInfoRequest`.                  |
| Tests                  | Only `AppApplicationTests` exists.                                                        | Add unit tests for services (DiscountCode logic, RateLimitFilter) and integration tests for controller happy paths.    |
| Security Headers       | No Content-Security-Policy / rate limit headers beyond custom ones.                       | Configure `HttpSecurity.headers()` for CSP, HSTS, and X-Frame-Options.                                                 |
| Scheduler              | `DailyQuestionScheduler` exists but lacks monitoring/alerting.                            | Add logging and metrics for job success/failure, plus Slack/webhook alerts.                                            |
| DiscountCode Analytics | No endpoints to see usage stats.                                                          | Add `/usage` endpoint returning redemption counts, remaining quota, per-user usage.                                    |
| Profile Feature        | `UpdateProfileRequest` now enum-based but controller lost `@Valid`.                       | Re-add `@Valid` to ensure Enum binding errors surface nicely.                                                          |

## 5. Suggested New Features & Enhancements

1. **Customer-facing discount insights:** expose non-admin endpoint to list currently active codes for eligible users (respect scopes).
2. **Gift codes / referral program:** extend `DiscountCode` with `owner_user_id`, usage tracking, referral bonuses.
3. **Audit trail:** create `AuditLog` entity (who changed profile, redeemed discount, uploaded file) to support compliance.
4. **Search caching:** add Redis/L2 cache for heavy catalog queries; combine with elastic-style text search for `BookCatalogService`.
5. **Rate limit dashboard:** expose admin API showing remaining tokens per key to debug throttling complaints.
6. **Infrastructure automation:** add Flyway/Liquibase for schema migrations (currently manual via repositories) to manage index/constraint rollout described above.
7. **Domain events:** publish events (test submitted, purchase completed, discount redeemed) via Spring events or message broker for analytics and notifications.

## 6. Conclusion

Addressing the critical items (validation, file safety, payment accuracy, PII logging) should be top priority before shipping further features. High-priority optimizations (rate limits, resilience, observability) will stabilize production. Medium-term opportunities (query optimization, testing, analytics) will keep the platform maintainable and extensible. Refer back to `YAPILMASI_GEREKENLER.md` for previously tracked tasks and sync with this updated audit to avoid duplication.
