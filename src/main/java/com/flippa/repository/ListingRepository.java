package com.flippa.repository;

import com.flippa.entity.Listing;
import com.flippa.entity.Listing.ListingStatus;
import com.flippa.entity.Listing.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByStatus(ListingStatus status);
    List<Listing> findByTypeAndStatus(ListingType type, ListingStatus status);
    List<Listing> findBySellerId(Long sellerId);
    List<Listing> findByFeaturedTrueAndStatus(ListingStatus status);
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Listing> searchListings(@Param("query") String query, @Param("status") ListingStatus status);
}

