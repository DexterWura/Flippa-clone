package com.flippa.service;

import com.flippa.entity.SystemConfig;
import com.flippa.entity.User;
import com.flippa.repository.SystemConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminService adminService;

    private SystemConfig systemConfig;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@example.com");

        systemConfig = new SystemConfig();
        systemConfig.setId(1L);
        systemConfig.setConfigKey("system.name");
        systemConfig.setConfigValue("Flippa Clone");
        systemConfig.setDescription("System name");
        systemConfig.setEnabled(true);
        systemConfig.setConfigType("STRING");
    }

    @Test
    void testGetAllConfigs() {
        // Arrange
        List<SystemConfig> configs = Arrays.asList(systemConfig);
        when(systemConfigRepository.findAll()).thenReturn(configs);

        // Act
        List<SystemConfig> result = adminService.getAllConfigs();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("system.name", result.get(0).getConfigKey());
    }

    @Test
    void testGetConfigByKey_Success() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(systemConfig));

        // Act
        Optional<SystemConfig> result = adminService.getConfigByKey("system.name");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Flippa Clone", result.get().getConfigValue());
    }

    @Test
    void testGetConfigByKey_NotFound() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // Act
        Optional<SystemConfig> result = adminService.getConfigByKey("nonexistent.key");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateConfig_ExistingConfig() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(systemConfig));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(systemConfig);

        // Act
        SystemConfig result = adminService.updateConfig("system.name", "New Name", "Updated description", adminUser, request);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", systemConfig.getConfigValue());
        assertEquals("Updated description", systemConfig.getDescription());
        verify(systemConfigRepository, times(1)).save(systemConfig);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("CONFIG_UPDATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testUpdateConfig_NewConfig() {
        // Arrange
        SystemConfig newConfig = new SystemConfig();
        newConfig.setConfigKey("new.key");
        newConfig.setConfigValue("new value");

        when(systemConfigRepository.findByConfigKey("new.key")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(newConfig);

        // Act
        SystemConfig result = adminService.updateConfig("new.key", "new value", "Description", adminUser, request);

        // Assert
        assertNotNull(result);
        verify(systemConfigRepository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    void testToggleConfig_Enable() {
        // Arrange
        systemConfig.setEnabled(false);
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(systemConfig));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(systemConfig);

        // Act
        adminService.toggleConfig("system.name", true, adminUser, request);

        // Assert
        assertTrue(systemConfig.getEnabled());
        verify(systemConfigRepository, times(1)).save(systemConfig);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("CONFIG_TOGGLED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testToggleConfig_Disable() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(systemConfig));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(systemConfig);

        // Act
        adminService.toggleConfig("system.name", false, adminUser, request);

        // Assert
        assertFalse(systemConfig.getEnabled());
        verify(systemConfigRepository, times(1)).save(systemConfig);
    }

    @Test
    void testToggleConfig_NotFound() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminService.toggleConfig("nonexistent.key", true, adminUser, request);
        });

        assertEquals("Config not found: nonexistent.key", exception.getMessage());
        verify(systemConfigRepository, never()).save(any(SystemConfig.class));
    }

    @Test
    void testIsPaymentGatewayEnabled_Enabled() {
        // Arrange
        SystemConfig paypalConfig = new SystemConfig();
        paypalConfig.setEnabled(true);
        when(systemConfigRepository.findByConfigKey("payment.gateway.paypal.enabled"))
            .thenReturn(Optional.of(paypalConfig));

        // Act
        boolean result = adminService.isPaymentGatewayEnabled("paypal");

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsPaymentGatewayEnabled_Disabled() {
        // Arrange
        SystemConfig paypalConfig = new SystemConfig();
        paypalConfig.setEnabled(false);
        when(systemConfigRepository.findByConfigKey("payment.gateway.paypal.enabled"))
            .thenReturn(Optional.of(paypalConfig));

        // Act
        boolean result = adminService.isPaymentGatewayEnabled("paypal");

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsPaymentGatewayEnabled_DefaultTrue() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("payment.gateway.paypal.enabled"))
            .thenReturn(Optional.empty());

        // Act
        boolean result = adminService.isPaymentGatewayEnabled("paypal");

        // Assert
        assertTrue(result); // Defaults to true when not configured
    }

    @Test
    void testGetPaymentConfigValue() {
        // Arrange
        SystemConfig config = new SystemConfig();
        config.setConfigValue("client-id-123");
        when(systemConfigRepository.findByConfigKey("payment.gateway.paypal.client-id"))
            .thenReturn(Optional.of(config));

        // Act
        String result = adminService.getPaymentConfigValue("paypal", "client-id");

        // Assert
        assertEquals("client-id-123", result);
    }

    @Test
    void testGetPaymentConfigValue_WithDefault() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("payment.gateway.paypal.client-id"))
            .thenReturn(Optional.empty());

        // Act
        String result = adminService.getPaymentConfigValue("paypal", "client-id", "default-value");

        // Assert
        assertEquals("default-value", result);
    }

    @Test
    void testGetSystemName() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.of(systemConfig));

        // Act
        String result = adminService.getSystemName();

        // Assert
        assertEquals("Flippa Clone", result);
    }

    @Test
    void testGetSystemName_Default() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("system.name")).thenReturn(Optional.empty());

        // Act
        String result = adminService.getSystemName();

        // Assert
        assertEquals("Flippa Clone", result); // Default fallback
    }

    @Test
    void testIsAutoApproveEnabled_True() {
        // Arrange
        SystemConfig config = new SystemConfig();
        config.setEnabled(true);
        config.setConfigValue("true");
        when(systemConfigRepository.findByConfigKey("listing.auto-approve.enabled"))
            .thenReturn(Optional.of(config));

        // Act
        boolean result = adminService.isAutoApproveEnabled();

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsAutoApproveEnabled_False() {
        // Arrange
        SystemConfig config = new SystemConfig();
        config.setEnabled(false);
        config.setConfigValue("false");
        when(systemConfigRepository.findByConfigKey("listing.auto-approve.enabled"))
            .thenReturn(Optional.of(config));

        // Act
        boolean result = adminService.isAutoApproveEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsAutoApproveEnabled_DefaultFalse() {
        // Arrange
        when(systemConfigRepository.findByConfigKey("listing.auto-approve.enabled"))
            .thenReturn(Optional.empty());

        // Act
        boolean result = adminService.isAutoApproveEnabled();

        // Assert
        assertFalse(result); // Defaults to false
    }
}

