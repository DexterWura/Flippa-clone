package com.flippa.repository;

import com.flippa.entity.Escrow;
import com.flippa.entity.Escrow.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, Long> {
    List<Escrow> findByBuyerId(Long buyerId);
    List<Escrow> findBySellerId(Long sellerId);
    List<Escrow> findByStatus(EscrowStatus status);
    Optional<Escrow> findByListingId(Long listingId);
    List<Escrow> findByDisputeRaisedTrue();
}

