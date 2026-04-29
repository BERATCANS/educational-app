package com.beratcan.app.repository;

import com.beratcan.app.entity.ParentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, Long> {

    Optional<ParentProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
