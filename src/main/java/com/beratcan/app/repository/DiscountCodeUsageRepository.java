package com.beratcan.app.repository;

import com.beratcan.app.entity.DiscountCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountCodeUsageRepository extends JpaRepository<DiscountCodeUsage, Long> {
    @Query("SELECT COUNT(du) FROM DiscountCodeUsage du WHERE du.discountCode.id = :discountCodeId AND du.user.id = :userId")
    long countByDiscountCodeIdAndUserId(@Param("discountCodeId") Long discountCodeId, @Param("userId") Long userId);
}
