package com.beratcan.app.repository;

import com.beratcan.app.entity.Book;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.entity.enums.LessonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

        /**
         * Arama ve filtreleme ile pagination desteği
         * DB seviyesinde pagination yapar, memory'ye yük bindirmez
         */
        @Query("SELECT b FROM Book b WHERE " +
                        "(:query IS NULL OR :query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.publisher.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
                        +
                        "(:examTypes IS NULL OR b.examType IN :examTypes) AND " +
                        "(:gradeLevels IS NULL OR b.gradeLevel IN :gradeLevels) AND " +
                        "(:lessonTypes IS NULL OR b.lessonType IN :lessonTypes) AND " +
                        "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR b.price <= :maxPrice)")
        Page<Book> searchAndFilterBooksWithPagination(
                        @Param("query") String query,
                        @Param("examTypes") List<ExamType> examTypes,
                        @Param("gradeLevels") List<GradeLevel> gradeLevels,
                        @Param("lessonTypes") List<LessonType> lessonTypes,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

        /**
         * Arama ve filtreleme ile pagination ve sıralama desteği
         * Kullanıcının examType'ına göre önce o kitapları, sonra diğerlerini sıralar
         * DB seviyesinde pagination yapar, memory'ye yük bindirmez
         */
        @Query("SELECT b FROM Book b WHERE " +
                        "(:query IS NULL OR :query = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.publisher.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
                        +
                        "(:examTypes IS NULL OR b.examType IN :examTypes) AND " +
                        "(:gradeLevels IS NULL OR b.gradeLevel IN :gradeLevels) AND " +
                        "(:lessonTypes IS NULL OR b.lessonType IN :lessonTypes) AND " +
                        "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR b.price <= :maxPrice) " +
                        "ORDER BY CASE WHEN :userExamType IS NOT NULL AND b.examType = :userExamType THEN 0 ELSE 1 END, b.id")
        Page<Book> searchAndFilterBooksWithPaginationAndSorting(
                        @Param("query") String query,
                        @Param("examTypes") List<ExamType> examTypes,
                        @Param("gradeLevels") List<GradeLevel> gradeLevels,
                        @Param("lessonTypes") List<LessonType> lessonTypes,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("userExamType") ExamType userExamType,
                        Pageable pageable);

        long countByPublisher_Id(Long publisherId);

        boolean existsByPublisher_Id(Long publisherId);
}
