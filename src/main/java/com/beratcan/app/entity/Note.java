package com.beratcan.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Frontend SQLite'taki "notebooks" tablosunun backend karşılığı.
 *
 * <p>Soft delete service katmanında elle yapılır (is_deleted + deleted_at
 * birlikte). @SQLDelete/@SQLRestriction kullanılmıyor — sync delta sorguları
 * silinmiş kayıtları da görmek zorunda.
 */
@Entity
@Getter
@Setter
@Table(name = "notes")
public class Note {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title = "";

    @Column(nullable = false, length = 16)
    private String color = "#4F8EF7";

    @Column(nullable = false, length = 64)
    private String icon = "book-outline";

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Long deletedAt;
}
