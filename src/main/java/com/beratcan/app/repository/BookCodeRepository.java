package com.beratcan.app.repository;

import com.beratcan.app.entity.BookCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookCodeRepository extends JpaRepository<BookCode, Long> {
    Optional<BookCode> findByCodeAndUsedFalse(String code);

    boolean existsByCode(String code);
}
