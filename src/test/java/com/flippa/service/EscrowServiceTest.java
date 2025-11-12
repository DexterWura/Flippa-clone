package com.flippa.service;

import com.flippa.entity.Escrow;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.repository.EscrowRepository;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EscrowService escrowService;

    private Listing listing;
    private User buyer;
    private User seller;
    private Escrow escrow;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setEmail("seller@example.com");
        seller.setFirstName("John");
        seller.setLastName("Doe");

        buyer = new User();
        buyer.setId(2L);
        buyer.setEmail("buyer@example.com");
        buyer.setFirstName("Jane");
        buyer.setLastName("Smith");

        listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Test Listing");
        listing.setPrice(new BigDecimal("1000.00"));
        listing.setSeller(seller);
        listing.setStatus(Listing.ListingStatus.ACTIVE);

        escrow = new Escrow();
        escrow.setId(1L);
        escrow.setListing(listing);
        escrow.setBuyer(buyer);
        escrow.setSeller(seller);
        escrow.setAmount(new BigDecimal("1000.00"));
        escrow.setStatus(Escrow.EscrowStatus.PENDING_PAYMENT);
    }

    @Test
    void testCreateEscrow_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);

        // Act
        Escrow result = escrowService.createEscrow(1L, 2L, Escrow.PaymentGateway.PAYPAL, "Buyer notes", request);

        // Assert
        assertNotNull(result);
        assertEquals(Escrow.EscrowStatus.PENDING_PAYMENT, result.getStatus());
        assertEquals(new BigDecimal("1000.00"), result.getAmount());
        verify(escrowRepository, times(1)).save(any(Escrow.class));
        verify(auditLogService, times(1)).logAction(eq(buyer), eq("ESCROW_CREATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCreateEscrow_ListingNotFound() {
        // Arrange
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.createEscrow(999L, 2L, Escrow.PaymentGateway.PAYPAL, "Notes", request);
        });

        assertEquals("Listing not found", exception.getMessage());
        verify(escrowRepository, never()).save(any(Escrow.class));
    }

    @Test
    void testCreateEscrow_ListingNotActive() {
        // Arrange
        listing.setStatus(Listing.ListingStatus.SOLD);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.createEscrow(1L, 2L, Escrow.PaymentGateway.PAYPAL, "Notes", request);
        });

        assertEquals("Listing is not available for purchase", exception.getMessage());
        verify(escrowRepository, never()).save(any(Escrow.class));
    }

    @Test
    void testCreateEscrow_CannotBuyOwnListing() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.createEscrow(1L, 1L, Escrow.PaymentGateway.PAYPAL, "Notes", request);
        });

        assertEquals("Cannot buy your own listing", exception.getMessage());
        verify(escrowRepository, never()).save(any(Escrow.class));
    }

    @Test
    void testFindByBuyerId() {
        // Arrange
        List<Escrow> escrows = Arrays.asList(escrow);
        when(escrowRepository.findByBuyerId(2L)).thenReturn(escrows);

        // Act
        List<Escrow> result = escrowService.findByBuyerId(2L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getBuyer().getId());
    }

    @Test
    void testFindBySellerId() {
        // Arrange
        List<Escrow> escrows = Arrays.asList(escrow);
        when(escrowRepository.findBySellerId(1L)).thenReturn(escrows);

        // Act
        List<Escrow> result = escrowService.findBySellerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSeller().getId());
    }

    @Test
    void testGetDisputes() {
        // Arrange
        escrow.setDisputeRaised(true);
        List<Escrow> disputes = Arrays.asList(escrow);
        when(escrowRepository.findByDisputeRaisedTrue()).thenReturn(disputes);

        // Act
        List<Escrow> result = escrowService.getDisputes();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDisputeRaised());
    }

    @Test
    void testRaiseDispute_ByBuyer() {
        // Arrange
        escrow.setStatus(Escrow.EscrowStatus.PAYMENT_RECEIVED);
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);

        // Act
        escrowService.raiseDispute(1L, "Item not as described", buyer, request);

        // Assert
        assertTrue(escrow.getDisputeRaised());
        assertEquals("Item not as described", escrow.getDisputeReason());
        assertEquals(Escrow.EscrowStatus.DISPUTE_RAISED, escrow.getStatus());
        verify(escrowRepository, times(1)).save(escrow);
        verify(auditLogService, times(1)).logAction(eq(buyer), eq("ESCROW_DISPUTE_RAISED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testRaiseDispute_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(999L);
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.raiseDispute(1L, "Reason", otherUser, request);
        });

        assertEquals("Unauthorized to raise dispute", exception.getMessage());
        verify(escrowRepository, never()).save(any(Escrow.class));
    }

    @Test
    void testResolveDispute_Success() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(3L);
        escrow.setDisputeRaised(true);
        escrow.setStatus(Escrow.EscrowStatus.DISPUTE_RAISED);

        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);

        // Act
        escrowService.resolveDispute(1L, "Refund buyer", "Admin notes", 
            Escrow.EscrowStatus.REFUNDED, adminUser, request);

        // Assert
        assertEquals("Refund buyer", escrow.getDisputeResolution());
        assertEquals("Admin notes", escrow.getAdminResolutionNotes());
        assertEquals(Escrow.EscrowStatus.REFUNDED, escrow.getStatus());
        assertFalse(escrow.getDisputeRaised());
        verify(escrowRepository, times(1)).save(escrow);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("ESCROW_DISPUTE_RESOLVED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCompleteTransfer_BySeller() {
        // Arrange
        escrow.setStatus(Escrow.EscrowStatus.PAYMENT_RECEIVED);
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);

        // Act
        escrowService.completeTransfer(1L, seller, request);

        // Assert
        assertEquals(Escrow.EscrowStatus.TRANSFER_COMPLETED, escrow.getStatus());
        assertEquals(Listing.ListingStatus.SOLD, listing.getStatus());
        verify(listingRepository, times(1)).save(listing);
        verify(escrowRepository, times(1)).save(escrow);
        verify(auditLogService, times(1)).logAction(eq(seller), eq("ESCROW_TRANSFER_COMPLETED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCompleteTransfer_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setRoles(new java.util.HashSet<>());
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.completeTransfer(1L, otherUser, request);
        });

        assertEquals("Unauthorized to complete transfer", exception.getMessage());
        verify(listingRepository, never()).save(any(Listing.class));
        verify(escrowRepository, never()).save(any(Escrow.class));
    }

    @Test
    void testConvertToDTO() {
        // Act
        com.flippa.dto.EscrowDTO result = escrowService.convertToDTO(escrow);

        // Assert
        assertNotNull(result);
        assertEquals(escrow.getId(), result.getId());
        assertEquals(listing.getId(), result.getListingId());
        assertEquals(listing.getTitle(), result.getListingTitle());
        assertEquals(buyer.getId(), result.getBuyerId());
        assertEquals(seller.getId(), result.getSellerId());
        assertEquals(escrow.getAmount(), result.getAmount());
        assertEquals(escrow.getStatus(), result.getStatus());
    }
}

