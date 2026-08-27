package com.vaultx.userservice.repository;

import com.vaultx.userservice.model.KycSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycSubmissionRepository extends JpaRepository<KycSubmission, UUID> {

    Optional<KycSubmission> findByUserId(UUID userId);
}
