package com.beratcan.app.repository;

import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
        List<Book> findByPaidFalse(); // Ücretsiz kitaplar

        @Query("SELECT b FROM Book b WHERE " +
                        "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(b.publisher) LIKE LOWER(CONCAT('%', :query, '%'))")
        List<Book> searchByTitleOrPublisher(@Param("query") String query);

        @Query("SELECT b FROM Book b WHERE " +
                        "(:query IS NULL OR :query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
                        +
                        "(:examTypes IS NULL OR b.examType IN :examTypes) AND " +
                        "(:gradeLevels IS NULL OR b.gradeLevel IN :gradeLevels) AND " +
                        "(:lessonTypes IS NULL OR b.lessonType IN :lessonTypes) AND " +
                        "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR b.price <= :maxPrice)")
        List<Book> searchAndFilterBooks(
                        @Param("query") String query,
                        @Param("examTypes") List<ExamType> examTypes,
                        @Param("gradeLevels") List<GradeLevel> gradeLevels,
                        @Param("lessonTypes") List<LessonType> lessonTypes,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice);
}
