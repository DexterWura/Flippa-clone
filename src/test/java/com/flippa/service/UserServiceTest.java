package com.flippa.service;

import com.flippa.dto.UserRegistrationDTO;
import com.flippa.entity.Role;
import com.flippa.entity.User;
import com.flippa.repository.RoleRepository;
import com.flippa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDTO registrationDTO;
    private User user;
    private Role roleUser;
    private Role roleBuyer;
    private Role roleSeller;

    @BeforeEach
    void setUp() {
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setEmail("test@example.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setFirstName("John");
        registrationDTO.setLastName("Doe");
        registrationDTO.setPhoneNumber("1234567890");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEnabled(true);
        user.setBanned(false);

        roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setName(Role.RoleType.ROLE_USER);
        roleUser.setEnabled(true);

        roleBuyer = new Role();
        roleBuyer.setId(2L);
        roleBuyer.setName(Role.RoleType.ROLE_BUYER);
        roleBuyer.setEnabled(true);

        roleSeller = new Role();
        roleSeller.setId(3L);
        roleSeller.setName(Role.RoleType.ROLE_SELLER);
        roleSeller.setEnabled(true);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleType.ROLE_USER)).thenReturn(Optional.of(roleUser));
        when(roleRepository.findByName(Role.RoleType.ROLE_BUYER)).thenReturn(Optional.of(roleBuyer));
        when(roleRepository.findByName(Role.RoleType.ROLE_SELLER)).thenReturn(Optional.of(roleSeller));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.registerUser(registrationDTO, request);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogService, times(1)).logAction(any(), eq("USER_REGISTERED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registrationDTO, request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFindByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testFindByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testBanUser_Success() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.banUser(1L, "Violation of terms", adminUser, request);

        // Assert
        assertTrue(user.getBanned());
        assertEquals("Violation of terms", user.getBanReason());
        verify(userRepository, times(1)).save(user);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("USER_BANNED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testBanUser_UserNotFound() {
        // Arrange
        User adminUser = new User();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.banUser(999L, "Reason", adminUser, request);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUnbanUser_Success() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        user.setBanned(true);
        user.setBanReason("Previous violation");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.unbanUser(1L, adminUser, request);

        // Assert
        assertFalse(user.getBanned());
        assertNull(user.getBanReason());
        verify(userRepository, times(1)).save(user);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("USER_UNBANNED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testToggleUserRole_EnableRole() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        user.setRoles(new HashSet<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(Role.RoleType.ROLE_SELLER)).thenReturn(Optional.of(roleSeller));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.toggleUserRole(1L, Role.RoleType.ROLE_SELLER, true, adminUser, request);

        // Assert
        assertTrue(user.getRoles().contains(roleSeller));
        verify(userRepository, times(1)).save(user);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("USER_ROLE_ENABLED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testToggleUserRole_DisableRole() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        Set<Role> roles = new HashSet<>();
        roles.add(roleSeller);
        user.setRoles(roles);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(Role.RoleType.ROLE_SELLER)).thenReturn(Optional.of(roleSeller));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.toggleUserRole(1L, Role.RoleType.ROLE_SELLER, false, adminUser, request);

        // Assert
        assertFalse(user.getRoles().contains(roleSeller));
        verify(userRepository, times(1)).save(user);
        verify(auditLogService, times(1)).logAction(eq(adminUser), eq("USER_ROLE_DISABLED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testToggleUserRole_RoleNotFound() {
        // Arrange
        User adminUser = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(Role.RoleType.ROLE_SELLER)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.toggleUserRole(1L, Role.RoleType.ROLE_SELLER, true, adminUser, request);
        });

        assertEquals("Role not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}

