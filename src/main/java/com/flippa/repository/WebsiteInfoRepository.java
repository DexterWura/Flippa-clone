package com.flippa.repository;

import com.flippa.entity.WebsiteInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebsiteInfoRepository extends JpaRepository<WebsiteInfo, Long> {
    Optional<WebsiteInfo> findByListingId(Long listingId);
}

