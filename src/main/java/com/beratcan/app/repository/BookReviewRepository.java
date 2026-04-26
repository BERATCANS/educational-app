package com.beratcan.app.repository;

import com.beratcan.app.entity.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

        List<BookReview> findByBookIdOrderByCreatedAtDesc(Long bookId);

        Optional<BookReview> findByBookIdAndStudentProfileId(Long bookId, Long studentProfileId);

        /**
         * Kullanıcının yorumunu hariç tutarak diğer yorumları pagination ile getirir
         * JOIN FETCH ile StudentProfile → User ve Book ilişkilerini yükler
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.book.id = :bookId " +
                        "AND (:excludeUserId IS NULL OR sp.user.id != :excludeUserId) " +
                        "ORDER BY br.createdAt DESC")
        Page<BookReview> findByBookIdExcludingUserId(
                        @Param("bookId") Long bookId,
                        @Param("excludeUserId") Long excludeUserId,
                        Pageable pageable);

        /**
         * BookReview'ı StudentProfile → User ve Book ile birlikte getirir (JOIN FETCH)
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.id = :reviewId")
        Optional<BookReview> findByIdWithStudentProfileAndBook(@Param("reviewId") Long reviewId);

        /**
         * BookReview'ı StudentProfile → User ve Book ile birlikte getirir
         * BookId ve UserId ile
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.studentProfile sp " +
                        "JOIN FETCH sp.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.book.id = :bookId AND sp.user.id = :userId")
        Optional<BookReview> findByBookIdAndUserIdWithStudentProfileAndBook(
                        @Param("bookId") Long bookId,
                        @Param("userId") Long userId);

        /**
         * Kullanıcının yorumunu hariç tutarak toplam yorum sayısını getirir
         */
        @Query("SELECT COUNT(br) FROM BookReview br " +
                        "JOIN br.studentProfile sp " +
                        "WHERE br.book.id = :bookId " +
                        "AND (:excludeUserId IS NULL OR sp.user.id != :excludeUserId)")
        long countByBookIdExcludingUserId(
                        @Param("bookId") Long bookId,
                        @Param("excludeUserId") Long excludeUserId);

        interface BookRatingAggregate {
                Long getBookId();

                Double getAverageRating();

                Long getReviewCount();
        }
}
