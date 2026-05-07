package com.beratcan.app.service;

import com.beratcan.app.dto.request.SyncPushRequest;
import com.beratcan.app.dto.response.NoteDto;
import com.beratcan.app.dto.response.NotePageDto;
import com.beratcan.app.dto.response.SyncPullResponse;
import com.beratcan.app.dto.response.SyncPushResponse;
import com.beratcan.app.dto.response.SyncPushResponse.SyncResult;
import com.beratcan.app.entity.Note;
import com.beratcan.app.entity.NotePage;
import com.beratcan.app.entity.enums.NotePageType;
import com.beratcan.app.repository.NotePageRepository;
import com.beratcan.app.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Frontend'in sync_queue'sundan gelen batch operasyonları işler ve
 * delta-pull sağlar. Last-write-wins: gelen updatedAt server'daki kayıttan
 * küçükse "conflict" döner — frontend pull yapıp local'i overwrite eder.
 *
 * <p>Ownership: bir kullanıcı sadece kendi notebook'larını ve onlara ait
 * sayfaları upsert/delete edebilir. İhlal "error" sonucu döner.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSyncService {

    private final NoteRepository noteRepository;
    private final NotePageRepository pageRepository;

    // ── PULL ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SyncPullResponse pull(Long userId, long since) {
        long serverTime = System.currentTimeMillis();

        List<Note> activeNotes = noteRepository.findActiveByUserSince(userId, since);
        List<String> deletedNoteIds = noteRepository.findDeletedIdsByUserSince(userId, since);

        // Sayfa sorguları için kullanıcının TÜM not ID'lerine ihtiyaç var
        // (sadece since'den sonra güncellenen değil — başka not'un sayfası
        // güncellenmişse de gelmesi gerek). En basit: kullanıcının tüm
        // aktif notlarının ID'lerini ayrı sorguyla al.
        List<String> allUserNoteIds = noteRepository.findActiveByUserSince(userId, 0L)
                .stream().map(Note::getId).toList();

        List<NotePage> activePages = allUserNoteIds.isEmpty()
                ? List.of()
                : pageRepository.findActiveByNoteIdsSince(allUserNoteIds, since);

        // Silinmiş sayfalar: aktif notların altındaki silinmiş sayfalar +
        // silinmiş notlara ait sayfalar (bunlar zaten cascade ile düşer ama
        // delta'da bilgi olarak ID'leri verelim ki frontend local'i temizlesin)
        List<String> noteIdsForDeletedPagesQuery = new ArrayList<>(allUserNoteIds);
        noteIdsForDeletedPagesQuery.addAll(deletedNoteIds);
        List<String> deletedPageIds = noteIdsForDeletedPagesQuery.isEmpty()
                ? List.of()
                : pageRepository.findDeletedIdsByNoteIdsSince(noteIdsForDeletedPagesQuery, since);

        return new SyncPullResponse(
                activeNotes.stream().map(NoteDto::from).toList(),
                activePages.stream().map(NotePageDto::from).toList(),
                deletedNoteIds,
                deletedPageIds,
                serverTime);
    }

    // ── PUSH ──────────────────────────────────────────────────────────

    @Transactional
    public SyncPushResponse push(Long userId, SyncPushRequest req) {
        long serverTime = System.currentTimeMillis();
        List<SyncResult> results = new ArrayList<>(req.getOperations().size());

        // Bu batch içinde upsert edilen notebook ID'lerini cache'le —
        // aynı batch'te hem notebook hem onun sayfası gelirse ownership
        // kontrolü için DB'ye tekrar gitmeden bilebilelim.
        Set<String> ownedNoteIdsCache = new HashSet<>();

        for (SyncPushRequest.SyncOperation op : req.getOperations()) {
            try {
                SyncResult r = handleOperation(userId, op, ownedNoteIdsCache);
                results.add(r);
            } catch (Exception e) {
                log.warn("Sync op failed userId={} entity={} id={}: {}",
                        userId, op.getEntityType(), op.getId(), e.getMessage());
                results.add(new SyncResult(op.getId(), "error", null, e.getMessage()));
            }
        }

        return new SyncPushResponse(results, serverTime);
    }

    private SyncResult handleOperation(Long userId, SyncPushRequest.SyncOperation op,
                                       Set<String> ownedNoteIdsCache) {
        boolean isNotebook = "notebook".equals(op.getEntityType());
        boolean isPage = "page".equals(op.getEntityType());
        boolean isUpsert = "upsert".equals(op.getAction());
        boolean isDelete = "delete".equals(op.getAction());

        if (!isNotebook && !isPage) {
            return new SyncResult(op.getId(), "error", null,
                    "Bilinmeyen entityType: " + op.getEntityType());
        }
        if (!isUpsert && !isDelete) {
            return new SyncResult(op.getId(), "error", null,
                    "Bilinmeyen action: " + op.getAction());
        }

        if (isNotebook && isUpsert) {
            return upsertNotebook(userId, op, ownedNoteIdsCache);
        } else if (isNotebook && isDelete) {
            return deleteNotebook(userId, op);
        } else if (isPage && isUpsert) {
            return upsertPage(userId, op, ownedNoteIdsCache);
        } else {
            return deletePage(userId, op);
        }
    }

    // ── Notebook upsert/delete ────────────────────────────────────────

    private SyncResult upsertNotebook(Long userId, SyncPushRequest.SyncOperation op,
                                      Set<String> ownedNoteIdsCache) {
        Map<String, Object> p = op.getPayload();
        if (p == null) {
            return new SyncResult(op.getId(), "error", null, "payload eksik");
        }
        long incomingUpdated = readLong(p, "updatedAt", 0L);

        Optional<Note> existing = noteRepository.findById(op.getId());
        if (existing.isPresent()) {
            Note n = existing.get();
            if (!n.getUserId().equals(userId)) {
                return new SyncResult(op.getId(), "error", null, "Erişim yok");
            }
            if (n.isDeleted()) {
                // Silinmiş kayıt revive edilebilir mi? Şimdilik hayır —
                // silinmişse sync'te yeniden hayata döndürmek beklenmedik
                // davranış. Frontend yeni ID ile yaratmalı.
                return new SyncResult(op.getId(), "error", null, "Bu not silinmiş");
            }
            if (incomingUpdated < n.getUpdatedAt()) {
                return new SyncResult(op.getId(), "conflict", n.getUpdatedAt(),
                        "Server tarafı daha güncel");
            }
            applyNotebookFields(n, p);
            n.setUpdatedAt(incomingUpdated);
            noteRepository.save(n);
            ownedNoteIdsCache.add(n.getId());
            return new SyncResult(op.getId(), "ok", n.getUpdatedAt(), null);
        }

        Note n = new Note();
        n.setId(op.getId());
        n.setUserId(userId);
        applyNotebookFields(n, p);
        n.setCreatedAt(readLong(p, "createdAt", incomingUpdated));
        n.setUpdatedAt(incomingUpdated);
        noteRepository.save(n);
        ownedNoteIdsCache.add(n.getId());
        return new SyncResult(op.getId(), "ok", n.getUpdatedAt(), null);
    }

    private void applyNotebookFields(Note n, Map<String, Object> p) {
        Object title = p.get("title");
        if (title != null) n.setTitle(String.valueOf(title));
        Object color = p.get("color");
        if (color != null) n.setColor(String.valueOf(color));
        Object icon = p.get("icon");
        if (icon != null) n.setIcon(String.valueOf(icon));
    }

    private SyncResult deleteNotebook(Long userId, SyncPushRequest.SyncOperation op) {
        Optional<Note> existing = noteRepository.findById(op.getId());
        if (existing.isEmpty()) {
            // Idempotent: zaten yoksa başarılı say
            return new SyncResult(op.getId(), "ok", null, null);
        }
        Note n = existing.get();
        if (!n.getUserId().equals(userId)) {
            return new SyncResult(op.getId(), "error", null, "Erişim yok");
        }
        long now = System.currentTimeMillis();
        n.setDeleted(true);
        n.setDeletedAt(now);
        n.setUpdatedAt(now);
        noteRepository.save(n);
        return new SyncResult(op.getId(), "ok", n.getUpdatedAt(), null);
    }

    // ── Page upsert/delete ────────────────────────────────────────────

    private SyncResult upsertPage(Long userId, SyncPushRequest.SyncOperation op,
                                  Set<String> ownedNoteIdsCache) {
        Map<String, Object> p = op.getPayload();
        if (p == null) {
            return new SyncResult(op.getId(), "error", null, "payload eksik");
        }
        String noteId = (String) p.get("noteId");
        if (noteId == null) {
            return new SyncResult(op.getId(), "error", null, "noteId eksik");
        }

        if (!isOwnedByUser(noteId, userId, ownedNoteIdsCache)) {
            return new SyncResult(op.getId(), "error", null, "Bu not'a erişim yok");
        }

        long incomingUpdated = readLong(p, "updatedAt", 0L);
        Optional<NotePage> existing = pageRepository.findById(op.getId());

        if (existing.isPresent()) {
            NotePage page = existing.get();
            if (!page.getNoteId().equals(noteId)) {
                return new SyncResult(op.getId(), "error", null, "noteId değiştirilemez");
            }
            if (page.isDeleted()) {
                return new SyncResult(op.getId(), "error", null, "Bu sayfa silinmiş");
            }
            if (incomingUpdated < page.getUpdatedAt()) {
                return new SyncResult(op.getId(), "conflict", page.getUpdatedAt(),
                        "Server tarafı daha güncel");
            }
            applyPageFields(page, p);
            page.setUpdatedAt(incomingUpdated);
            pageRepository.save(page);
            return new SyncResult(op.getId(), "ok", page.getUpdatedAt(), null);
        }

        NotePage page = new NotePage();
        page.setId(op.getId());
        page.setNoteId(noteId);
        applyPageFields(page, p);
        page.setCreatedAt(readLong(p, "createdAt", incomingUpdated));
        page.setUpdatedAt(incomingUpdated);
        pageRepository.save(page);
        return new SyncResult(op.getId(), "ok", page.getUpdatedAt(), null);
    }

    private void applyPageFields(NotePage page, Map<String, Object> p) {
        Object title = p.get("title");
        if (title != null) page.setTitle(String.valueOf(title));

        Object pageType = p.get("pageType");
        if (pageType != null) {
            page.setPageType(NotePageType.valueOf(String.valueOf(pageType).toUpperCase()));
        }

        Object orderIndex = p.get("orderIndex");
        if (orderIndex instanceof Number n) page.setOrderIndex(n.intValue());

        // content_json: payload'da AÇIKÇA varsa overwrite et
        // (null değer açıkça silmek anlamına gelir).
        if (p.containsKey("contentJson")) {
            page.setContentJson(p.get("contentJson") == null ? null : String.valueOf(p.get("contentJson")));
        }
    }

    private SyncResult deletePage(Long userId, SyncPushRequest.SyncOperation op) {
        Optional<NotePage> existing = pageRepository.findById(op.getId());
        if (existing.isEmpty()) {
            return new SyncResult(op.getId(), "ok", null, null);
        }
        NotePage page = existing.get();
        if (!isOwnedByUser(page.getNoteId(), userId, new HashSet<>())) {
            return new SyncResult(op.getId(), "error", null, "Bu sayfaya erişim yok");
        }
        long now = System.currentTimeMillis();
        page.setDeleted(true);
        page.setDeletedAt(now);
        page.setUpdatedAt(now);
        pageRepository.save(page);
        return new SyncResult(op.getId(), "ok", page.getUpdatedAt(), null);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private boolean isOwnedByUser(String noteId, Long userId, Set<String> cache) {
        if (cache.contains(noteId)) return true;
        Optional<Note> n = noteRepository.findByIdAndUserId(noteId, userId);
        if (n.isPresent() && !n.get().isDeleted()) {
            cache.add(noteId);
            return true;
        }
        return false;
    }

    private long readLong(Map<String, Object> p, String key, long fallback) {
        Object v = p.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}
