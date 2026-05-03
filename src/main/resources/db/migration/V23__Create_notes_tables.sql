-- ============================================================
-- V23: Not defteri (notes + note_pages) tabloları
-- ============================================================
-- Frontend (educational-app-frontend) tarafında SQLite'ta tutulan
-- notebook/page yapısının backend karşılığı. Sync queue + batch
-- push protokolü ile senkronize olur.
--
-- Tasarım kararları:
--  - PK: VARCHAR(36) UUID. Frontend client-side üretiyor; offline
--    yaratılan kayıt aynı ID ile sync edilir, remap gerekmez.
--  - Soft delete: is_deleted BOOLEAN + deleted_at BIGINT (epoch ms).
--    @SQLDelete/@SQLRestriction kullanılmıyor — sync sorguları
--    silinmiş kayıtları da görmek zorunda (delta için deletedIds).
--  - content_json ve thumbnail_b64 TEXT: PostgreSQL TOAST otomatik
--    sıkıştırır. ~1MB'a kadar performans sorunu yok.
--  - updated_at BIGINT (ms epoch): frontend Date.now() ile birebir
--    uyumlu, delta sync için index'li.
-- ============================================================

CREATE TABLE notes (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL DEFAULT '',
    color       VARCHAR(16)  NOT NULL DEFAULT '#4F8EF7',
    icon        VARCHAR(64)  NOT NULL DEFAULT 'book-outline',
    created_at  BIGINT NOT NULL,
    updated_at  BIGINT NOT NULL,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  BIGINT
);

CREATE INDEX idx_notes_user_updated ON notes(user_id, updated_at);
CREATE INDEX idx_notes_user_deleted ON notes(user_id, is_deleted);

CREATE TABLE note_pages (
    id            VARCHAR(36) PRIMARY KEY,
    note_id       VARCHAR(36) NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL DEFAULT 'Sayfa',
    page_type     VARCHAR(16) NOT NULL,
    order_index   INTEGER NOT NULL DEFAULT 0,
    content_json  TEXT,
    thumbnail_b64 TEXT,
    created_at    BIGINT NOT NULL,
    updated_at    BIGINT NOT NULL,
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    BIGINT
);

CREATE INDEX idx_note_pages_note ON note_pages(note_id, is_deleted);
CREATE INDEX idx_note_pages_updated ON note_pages(updated_at);
