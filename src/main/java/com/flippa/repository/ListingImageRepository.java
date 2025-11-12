package com.flippa.repository;

import com.flippa.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long listingId);
    Optional<ListingImage> findByListingIdAndIsPrimaryTrue(Long listingId);
    void deleteByListingId(Long listingId);
}

