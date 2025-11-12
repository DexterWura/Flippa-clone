package com.flippa.service;

import com.flippa.dto.ListingDTO;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.WebsiteInfoRepository;
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
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private WebsiteInfoRepository websiteInfoRepository;

    @Mock
    private WebsiteInfoFetchService websiteInfoFetchService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AdminService adminService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ListingService listingService;

    private ListingDTO listingDTO;
    private Listing listing;
    private User seller;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setEmail("seller@example.com");
        seller.setFirstName("John");
        seller.setLastName("Doe");

        listingDTO = new ListingDTO();
        listingDTO.setTitle("Test Listing");
        listingDTO.setDescription("Test Description");
        listingDTO.setType(Listing.ListingType.WEBSITE);
        listingDTO.setPrice(new BigDecimal("1000.00"));
        listingDTO.setWebsiteUrl("https://example.com");

        listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setType(Listing.ListingType.WEBSITE);
        listing.setPrice(new BigDecimal("1000.00"));
        listing.setSeller(seller);
        listing.setStatus(Listing.ListingStatus.ACTIVE);
    }

    @Test
    void testCreateListing_WithAutoApproveEnabled() {
        // Arrange
        when(adminService.isAutoApproveEnabled()).thenReturn(true);
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);
        doNothing().when(websiteInfoFetchService).fetchAndSaveWebsiteInfo(anyLong(), anyString());

        // Act
        Listing result = listingService.createListing(listingDTO, seller, request);

        // Assert
        assertNotNull(result);
        assertEquals(Listing.ListingStatus.ACTIVE, result.getStatus());
        verify(listingRepository, times(1)).save(any(Listing.class));
        verify(websiteInfoFetchService, times(1)).fetchAndSaveWebsiteInfo(anyLong(), eq("https://example.com"));
        verify(auditLogService, times(1)).logAction(any(), eq("LISTING_CREATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCreateListing_WithAutoApproveDisabled() {
        // Arrange
        when(adminService.isAutoApproveEnabled()).thenReturn(false);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing l = invocation.getArgument(0);
            l.setId(1L);
            l.setStatus(Listing.ListingStatus.PENDING_REVIEW);
            return l;
        });

        // Act
        Listing result = listingService.createListing(listingDTO, seller, request);

        // Assert
        assertNotNull(result);
        assertEquals(Listing.ListingStatus.PENDING_REVIEW, result.getStatus());
        verify(listingRepository, times(1)).save(any(Listing.class));
    }

    @Test
    void testCreateListing_WithoutWebsiteUrl() {
        // Arrange
        listingDTO.setWebsiteUrl(null);
        when(adminService.isAutoApproveEnabled()).thenReturn(true);
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act
        Listing result = listingService.createListing(listingDTO, seller, request);

        // Assert
        assertNotNull(result);
        verify(websiteInfoFetchService, never()).fetchAndSaveWebsiteInfo(anyLong(), anyString());
    }

    @Test
    void testGetAllActiveListings() {
        // Arrange
        List<Listing> activeListings = Arrays.asList(listing);
        when(listingRepository.findByStatus(Listing.ListingStatus.ACTIVE)).thenReturn(activeListings);

        // Act
        List<Listing> result = listingService.getAllActiveListings();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Listing", result.get(0).getTitle());
    }

    @Test
    void testGetFeaturedListings() {
        // Arrange
        listing.setFeatured(true);
        List<Listing> featuredListings = Arrays.asList(listing);
        when(listingRepository.findByFeaturedTrueAndStatus(Listing.ListingStatus.ACTIVE)).thenReturn(featuredListings);

        // Act
        List<Listing> result = listingService.getFeaturedListings();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getFeatured());
    }

    @Test
    void testSearchListings() {
        // Arrange
        List<Listing> searchResults = Arrays.asList(listing);
        when(listingRepository.searchListings("test", Listing.ListingStatus.ACTIVE)).thenReturn(searchResults);

        // Act
        List<Listing> result = listingService.searchListings("test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindById_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        // Act
        Optional<Listing> result = listingService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Listing", result.get().getTitle());
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Listing> result = listingService.findById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateListing_ByOwner() {
        // Arrange
        ListingDTO updateDTO = new ListingDTO();
        updateDTO.setTitle("Updated Title");
        updateDTO.setDescription("Updated Description");

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act
        Listing result = listingService.updateListing(1L, updateDTO, seller, request);

        // Assert
        assertNotNull(result);
        verify(listingRepository, times(1)).save(any(Listing.class));
        verify(auditLogService, times(1)).logAction(any(), eq("LISTING_UPDATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testUpdateListing_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setRoles(new java.util.HashSet<>());

        ListingDTO updateDTO = new ListingDTO();
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.updateListing(1L, updateDTO, otherUser, request);
        });

        assertEquals("Unauthorized to update this listing", exception.getMessage());
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    void testActivateListing_Success() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        listing.setStatus(Listing.ListingStatus.PENDING_REVIEW);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // Act
        listingService.activateListing(1L, adminUser, request);

        // Assert
        assertEquals(Listing.ListingStatus.ACTIVE, listing.getStatus());
        verify(listingRepository, times(1)).save(listing);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("LISTING_ACTIVATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testActivateListing_NotFound() {
        // Arrange
        User adminUser = new User();
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            listingService.activateListing(999L, adminUser, request);
        });

        assertEquals("Listing not found", exception.getMessage());
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    void testConvertToDTO() {
        // Act
        ListingDTO result = listingService.convertToDTO(listing);

        // Assert
        assertNotNull(result);
        assertEquals(listing.getId(), result.getId());
        assertEquals(listing.getTitle(), result.getTitle());
        assertEquals(listing.getDescription(), result.getDescription());
        assertEquals(listing.getPrice(), result.getPrice());
        assertEquals(seller.getId(), result.getSellerId());
    }
}

