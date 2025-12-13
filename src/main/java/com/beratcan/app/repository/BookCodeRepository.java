package com.beratcan.app.repository;

import com.beratcan.app.entity.BookCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookCodeRepository extends JpaRepository<BookCode, Long> {
    Optional<BookCode> findByCodeAndUsedFalse(String code);

    boolean existsByCode(String code);

    List<BookCode> findByBookId(Long bookId);

    /**
     * Tüm BookCode'ları book ve usedBy ilişkileri ile birlikte getirir (JOIN FETCH)
     * N+1 query problemini önlemek için
     */
    @Query("SELECT DISTINCT bc FROM BookCode bc " +
            "LEFT JOIN FETCH bc.book b " +
            "LEFT JOIN FETCH bc.usedBy u")
    List<BookCode> findAllWithBookAndUsedBy();

    /**
     * Belirli bir kitaba ait BookCode'ları book ve usedBy ilişkileri ile birlikte
     * getirir (JOIN FETCH)
     * N+1 query problemini önlemek için
     */
    @Query("SELECT DISTINCT bc FROM BookCode bc " +
            "LEFT JOIN FETCH bc.book b " +
            "LEFT JOIN FETCH bc.usedBy u " +
            "WHERE bc.book.id = :bookId")
    List<BookCode> findByBookIdWithBookAndUsedBy(@Param("bookId") Long bookId);

    /**
     * BookCode'u book ve usedBy ilişkileri ile birlikte getirir (JOIN FETCH)
     * N+1 query problemini önlemek için
     */
    @Query("SELECT DISTINCT bc FROM BookCode bc " +
            "LEFT JOIN FETCH bc.book b " +
            "LEFT JOIN FETCH bc.usedBy u " +
            "WHERE bc.id = :codeId")
    Optional<BookCode> findByIdWithBookAndUsedBy(@Param("codeId") Long codeId);
}
