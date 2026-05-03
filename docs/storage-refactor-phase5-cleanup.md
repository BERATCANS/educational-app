# Storage Refactor — Phase 5 Cleanup Checklist

> **Status.** Phases 1–4 shipped (foundation + book covers + question images +
> daily question images). This document collects everything the cleanup PR
> must do, plus the soak-period evidence to gather before merging it.
>
> **Do NOT run this PR until** logs prove the legacy surface is no longer
> used by production traffic. See [Pre-flight checks](#pre-flight-checks).

## Goal

Tear out the legacy file-handling surface that phases 2–4 replaced:
- The public, path-traversal-prone generic file endpoints
  (`GET /api/files/{filename}`, `POST /api/files/upload`).
- The single-arg `StorageService.saveFile(MultipartFile)` (now
  `@Deprecated`); leaves only the path-keyed 2-arg variant and the
  byte-array `(path, bytes, contentType)` variant for TTS audio.
- Any DTO field whose only purpose was to round-trip a flat-namespace
  filename through the old upload flow.

After Phase 5, all uploads are resource-specific endpoints with
ID-based hierarchical paths, all downloads go through entity-ID URLs,
and `FileController` is gone or quarantined.

---

## Pre-flight checks (before opening the PR)

These guard against deleting an endpoint someone is still using.

1. **Production access logs — last 7 days.** Confirm zero traffic to:
   - `GET /api/files/*`
   - `POST /api/files/upload`
   - `DELETE /api/files/*`
   If any matched, identify the caller and migrate it first.

2. **Frontend audit.** `grep -r "/api/files" frontend/src` (or whatever the
   FE repo is). The only acceptable matches today are:
   - test fixtures
   - dead code being deleted in this same PR
   No live render path may still construct `/api/files/{filename}` URLs.

3. **Mobile clients.** If any iOS/Android clients pin to `/api/files/...`,
   coordinate with their release cadence — the PR cannot ship until the
   oldest still-supported app version no longer uses those URLs.

4. **External integrations / webhooks.** Search any internal docs or
   integration partners for `/api/files`. Likely none, but cheap to
   verify.

5. **Search for `.coverImageUrl`-as-string in callers.** If frontend or
   admin tooling still posts `BookRequest.coverImageUrl = "<filename>"`,
   they must switch to the dedicated `POST /api/books/{bookId}/cover`
   endpoint first.

Document the evidence (log queries, grep output) in the PR body.

---

## Deletions

### 1. `FileController` — delete the entire class

[app/src/main/java/com/beratcan/app/controller/FileController.java](app/src/main/java/com/beratcan/app/controller/FileController.java)

Drop all three endpoints:
- `POST /api/files/upload`
- `GET /api/files/{filename}`
- `DELETE /api/files/{filename}`

Resource-specific endpoints (added in phases 2/3/4) replace them.

### 2. `FileService` — delete the wrapper

[app/src/main/java/com/beratcan/app/service/FileService.java](app/src/main/java/com/beratcan/app/service/FileService.java)

Currently a passthrough around `StorageService`. After migrations:
- `QuestionService`, `DailyQuestionService` already inject `FileService`
  but only call methods that exist on `StorageService`. Switch them to
  inject `StorageService` directly.
- `BookAdminService` already injects `StorageService` directly (Phase 2).

Once no caller remains, delete the class. Spring will warn about an unused
bean during the migration; acceptable transient state inside the same PR.

### 3. `StorageService.saveFile(MultipartFile)` — delete the single-arg variant

[app/src/main/java/com/beratcan/app/service/StorageService.java](app/src/main/java/com/beratcan/app/service/StorageService.java)
([line 18](app/src/main/java/com/beratcan/app/service/StorageService.java#L18))

After migrations there should be no caller. Verify with:
```
grep -rn "saveFile([^,]*MultipartFile[^,)]*)" app/src/main/java
```
The only legitimate matches must be `saveFile(MultipartFile, String)`
(the 2-arg variant we keep). If the regex still finds 1-arg uses,
migrate them before deleting the interface method.

Then drop:
- The `@Deprecated` 1-arg method from the interface
- Its implementations in `LocalStorageService` and `CloudStorageService`
- The `writeMultipart` helper's pre-validation duplication is no longer
  needed (the 2-arg variant has the same checks)

### 4. `BookRequest.coverImageUrl` — drop the field

[app/src/main/java/com/beratcan/app/dto/request/BookRequest.java:32](app/src/main/java/com/beratcan/app/dto/request/BookRequest.java#L32)

Remove the field. Then in `BookAdminService.addBook` /
`updateBook`, drop the line that writes `request.getCoverImageUrl()`
into the entity. Books are created without a cover; the cover comes via
`POST /api/books/{bookId}/cover`.

If any seed/test fixture sets it, migrate.

### 5. `VoiceAIChatController` manual `..` check — optional removal

[app/src/main/java/com/beratcan/app/controller/VoiceAIChatController.java:69-72](app/src/main/java/com/beratcan/app/controller/VoiceAIChatController.java#L69-L72)

The check became redundant when Y1 added `resolveSafe` inside
`LocalStorageService.getFile`. Two equally valid moves:
- **Keep** as defence in depth (one-liner, harmless).
- **Delete** for consistency, since storage layer is the boundary.

Decision: **keep**. It's three lines, makes the controller self-evident,
and costs nothing.

---

## Verification (cleanup PR)

1. **Static**
   - `./mvnw clean compile` — no warnings about unused imports / dead
     code from the deletions.
   - `./mvnw test` — all green.
   - `grep -rn "FileController\|FileService\|fileService" app/src/main/java`
     should return nothing (or only references inside files that are
     themselves being deleted in this PR).

2. **Boot**
   - App boots under `dev` profile.
   - App boots under `prod` profile (`ddl-auto=validate`) — schema
     validation must pass.

3. **Endpoint inventory**
   - `curl -i http://localhost:8080/api/files/upload` → **404**
   - `curl -i http://localhost:8080/api/files/foo.png` → **404**
   - `curl -i http://localhost:8080/api/books/1/cover` → 200 if cover exists, 404 otherwise (works)
   - `curl -i http://localhost:8080/api/daily-questions/1/image` → same
   - `curl -i http://localhost:8080/api/questions/1/image` → 401 anonymous (still gated)

4. **Smoke test the happy path**
   - Admin uploads a book cover via `POST /api/books/{id}/cover` →
     `Book.coverImageUrl` written as `books/{id}/cover-XXXXXXXX.jpg`
   - `GET /api/books/{id}/cover` returns it
   - Owner fetches `GET /api/questions/{id}/image` → 200
   - Non-owner fetches same → 403

5. **Audio TTS** (the only remaining 3-arg `saveFile` caller)
   - `SpeechProcessingService.saveTemporaryAudio` continues to write
     under `audio/temp/{uuid}.mp3`. No change expected; verify the
     voice-AI flow round-trips one audio file.

---

## Risk register

| Risk | Mitigation |
|---|---|
| Some forgotten caller still uses `/api/files/...` | Pre-flight log audit + grep |
| Frontend hasn't migrated | Coordinate cutover; cleanup PR holds until FE merges |
| Mobile app pins to old URL | Hold cleanup until oldest supported version is gone |
| Test or seed fixture uses old DTO field | grep before deletion; migrate test data |
| Removing `FileService` breaks an Async/scheduled job | grep `@Async` and `@Scheduled` callers — there are none today, recheck before merge |
| Breaks build because deprecation warning was load-bearing | Compile early; the `@Deprecated` warning was diagnostic only |

---

## Deferred — explicitly NOT in Phase 5

These are tempting but out of scope:

- **Tightening V4's `ON DELETE CASCADE`** on `questions.test_id` to
  `RESTRICT`. Belongs to a separate refactor about the soft-delete
  cascade story (Topic, Book counterparts).
- **TTL/sweeper for old idempotency keys** on `purchases`. Storage isn't
  pressure on us yet; revisit when row count justifies it.
- **Antivirus scanning** on uploads. Separate security initiative.
- **Signed URLs / time-limited tokens** for question-image downloads.
  Current ownership check is enough; signed URLs would only matter if we
  wanted CDN edge caching, which we do not for paid content.
- **Migrating `UserLibraryService` and `PurchaseService` inline ownership
  checks** to `userRepository.existsByIdAndOwnedBooks_Id`. Cosmetic +
  perf cleanup; separate PR after Phase 5 lands.

---

## Sequencing

1. Wait one sprint (≈2 weeks) after Phase 4 ships to gather soak evidence.
2. Run the [Pre-flight checks](#pre-flight-checks).
3. Open the cleanup PR; the diff should be mostly **deletions**.
4. Confirm CI green, then ship behind a feature flag *if* there's any
   uncertainty about external clients — otherwise direct merge.
5. Watch error rates for 24h post-deploy; rollback plan = revert.

---

## Files touched by Phase 5 (predicted)

Deletions:
- [app/src/main/java/com/beratcan/app/controller/FileController.java](app/src/main/java/com/beratcan/app/controller/FileController.java) (whole file)
- [app/src/main/java/com/beratcan/app/service/FileService.java](app/src/main/java/com/beratcan/app/service/FileService.java) (whole file)

Edits:
- [app/src/main/java/com/beratcan/app/service/StorageService.java](app/src/main/java/com/beratcan/app/service/StorageService.java) — drop 1-arg `saveFile`
- [app/src/main/java/com/beratcan/app/service/LocalStorageService.java](app/src/main/java/com/beratcan/app/service/LocalStorageService.java) — drop 1-arg implementation, drop `writeMultipart` if it became unused
- [app/src/main/java/com/beratcan/app/service/CloudStorageService.java](app/src/main/java/com/beratcan/app/service/CloudStorageService.java) — drop 1-arg implementation
- [app/src/main/java/com/beratcan/app/dto/request/BookRequest.java](app/src/main/java/com/beratcan/app/dto/request/BookRequest.java) — drop `coverImageUrl`
- [app/src/main/java/com/beratcan/app/service/BookAdminService.java](app/src/main/java/com/beratcan/app/service/BookAdminService.java) — drop the `setCoverImageUrl(request.getCoverImageUrl())` lines from `addBook` / `updateBook`
- [app/src/main/java/com/beratcan/app/service/QuestionService.java](app/src/main/java/com/beratcan/app/service/QuestionService.java) — replace `FileService` injection with `StorageService`
- [app/src/main/java/com/beratcan/app/service/DailyQuestionService.java](app/src/main/java/com/beratcan/app/service/DailyQuestionService.java) — same

Tests already in repo (`StoragePathsTest`, `LocalStorageServiceTest`,
`UserOwnershipQueryTest`, `CheckoutIdempotencyTest`,
`TestQuestionSoftDeleteCascadeTest`) should continue to pass without
modification.
