package com.beratcan.app.repository;

import com.beratcan.app.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

    Optional<Note> findByIdAndUserId(String id, Long userId);

    /**
     * Pull için: kullanıcının silinmemiş notları, since'den sonra güncellenmiş
     * (since=0 ise tüm aktif notlar dönülsün diye {@code >=} kullanılmaz —
     * frontend ilk sync'te zaten 0 gönderir, tüm yenileri alır).
     */
    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.isDeleted = false AND n.updatedAt > :since")
    List<Note> findActiveByUserSince(@Param("userId") Long userId, @Param("since") long since);

    /**
     * Pull için: since'den sonra silinmiş not ID'leri (delta sync).
     */
    @Query("SELECT n.id FROM Note n WHERE n.userId = :userId AND n.isDeleted = true AND n.deletedAt > :since")
    List<String> findDeletedIdsByUserSince(@Param("userId") Long userId, @Param("since") long since);
}
