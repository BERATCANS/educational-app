package com.beratcan.app.repository;

import com.beratcan.app.entity.NotePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotePageRepository extends JpaRepository<NotePage, String> {

    /**
     * Pull için: belirtilen note ID'lerine ait silinmemiş, since'den sonra
     * güncellenmiş sayfalar. noteIds boşsa Spring Data boş liste döner.
     */
    @Query("SELECT p FROM NotePage p WHERE p.noteId IN :noteIds AND p.isDeleted = false AND p.updatedAt > :since")
    List<NotePage> findActiveByNoteIdsSince(@Param("noteIds") List<String> noteIds, @Param("since") long since);

    /**
     * Pull için: silinmiş sayfa ID'leri (delta).
     */
    @Query("SELECT p.id FROM NotePage p WHERE p.noteId IN :noteIds AND p.isDeleted = true AND p.deletedAt > :since")
    List<String> findDeletedIdsByNoteIdsSince(@Param("noteIds") List<String> noteIds, @Param("since") long since);

    /**
     * Push'ta sayfa ownership doğrulamasında kullanılır: kullanıcının note
     * ID'lerini bulmak için ayrıca NoteRepository'den çekilir; bu repository
     * sadece sayfaları yönetir.
     */
}
