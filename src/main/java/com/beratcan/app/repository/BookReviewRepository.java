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

        Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

        /**
         * Kullanıcının yorumunu hariç tutarak diğer yorumları pagination ile getirir
         * Eğer excludeUserId null ise, tüm yorumları getirir
         * JOIN FETCH ile User ve Book ilişkilerini yükler (N+1 query önleme)
         * 
         * Not: DISTINCT kullanılmadı çünkü BookReview -> User -> Book ilişkisi 1:1,
         * duplicate sonuç olmayacak
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.book.id = :bookId " +
                        "AND (:excludeUserId IS NULL OR br.user.id != :excludeUserId) " +
                        "ORDER BY br.createdAt DESC")
        Page<BookReview> findByBookIdExcludingUserId(
                        @Param("bookId") Long bookId,
                        @Param("excludeUserId") Long excludeUserId,
                        Pageable pageable);

        /**
         * BookReview'ı User ve Book ile birlikte getirir (JOIN FETCH)
         * LazyInitializationException önlemek için
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.id = :reviewId")
        Optional<BookReview> findByIdWithUserAndBook(@Param("reviewId") Long reviewId);

        /**
         * BookReview'ı User ve Book ile birlikte getirir (JOIN FETCH)
         * BookId ve UserId ile
         */
        @Query("SELECT br FROM BookReview br " +
                        "JOIN FETCH br.user u " +
                        "JOIN FETCH br.book b " +
                        "WHERE br.book.id = :bookId AND br.user.id = :userId")
        Optional<BookReview> findByBookIdAndUserIdWithUserAndBook(
                        @Param("bookId") Long bookId,
                        @Param("userId") Long userId);

        /**
         * Kullanıcının yorumunu hariç tutarak toplam yorum sayısını getirir
         */
        @Query("SELECT COUNT(br) FROM BookReview br WHERE br.book.id = :bookId " +
                        "AND (:excludeUserId IS NULL OR br.user.id != :excludeUserId)")
        long countByBookIdExcludingUserId(
                        @Param("bookId") Long bookId,
                        @Param("excludeUserId") Long excludeUserId);

        interface BookRatingAggregate {
                Long getBookId();

                Double getAverageRating();

                Long getReviewCount();
        }
}
