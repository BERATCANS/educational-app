package com.beratcan.app.repository;

import com.beratcan.app.entity.BookReview;
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

    @Query("SELECT br.book.id AS bookId, AVG(br.rating) AS averageRating, COUNT(br.id) AS reviewCount " +
            "FROM BookReview br WHERE br.book.id IN :bookIds GROUP BY br.book.id")
    List<BookRatingAggregate> findRatingStatsByBookIds(@Param("bookIds") List<Long> bookIds);

    interface BookRatingAggregate {
        Long getBookId();

        Double getAverageRating();

        Long getReviewCount();
    }
}
