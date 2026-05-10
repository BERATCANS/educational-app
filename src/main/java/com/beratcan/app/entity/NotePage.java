package com.beratcan.app.entity;

import com.beratcan.app.entity.enums.NotePageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Frontend SQLite'taki "pages" tablosunun backend karşılığı.
 *
 * <p>content_json: ink sayfaları için DrawingPath[] JSON, text/note için
 * metin bloku JSON'u. PostgreSQL TOAST otomatik sıkıştırma uygular.
 * Thumbnail tutulmaz — frontend canvas'tan tekrar üretebilir.
 */
@Entity
@Getter
@Setter
@Table(name = "note_pages")
public class NotePage {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "note_id", nullable = false, length = 36)
    private String noteId;

    @Column(nullable = false, length = 255)
    private String title = "Sayfa";

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false, length = 16)
    private NotePageType pageType;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Long deletedAt;
}
