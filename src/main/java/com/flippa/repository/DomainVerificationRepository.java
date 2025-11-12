package com.flippa.repository;

import com.flippa.entity.DomainVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomainVerificationRepository extends JpaRepository<DomainVerification, Long> {
    Optional<DomainVerification> findByListingId(Long listingId);
    Optional<DomainVerification> findByVerificationToken(String token);
}

