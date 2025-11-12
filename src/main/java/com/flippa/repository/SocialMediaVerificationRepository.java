package com.flippa.repository;

import com.flippa.entity.SocialMediaVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialMediaVerificationRepository extends JpaRepository<SocialMediaVerification, Long> {
    Optional<SocialMediaVerification> findByListingId(Long listingId);
    Optional<SocialMediaVerification> findByVerificationToken(String token);
}

