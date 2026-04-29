package com.beratcan.app.repository;

import com.beratcan.app.entity.SuperAdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminProfileRepository extends JpaRepository<SuperAdminProfile, Long> {

    Optional<SuperAdminProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
