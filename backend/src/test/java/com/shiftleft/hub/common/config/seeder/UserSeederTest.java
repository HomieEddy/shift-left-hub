package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new UserSeeder(userRepository, passwordEncoder);
    }

    @Test
    void seedUsers_shouldCreateAdminWhenAbsent() {
        when(userRepository.existsByEmail("admin@x.com")).thenReturn(false);
        when(passwordEncoder.encode("admin-pw")).thenReturn("encoded-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User admin = seeder.seedUsers("admin@x.com", "admin-pw");

        assertEquals("admin@x.com", admin.getEmail());
        assertEquals("encoded-pw", admin.getPassword());
        assertEquals(UserRole.ROLE_ADMIN, admin.getRole());
        assertEquals("System Admin", admin.getDisplayName());
        assertTrue(admin.isEnabled());
    }

    @Test
    void seedUsers_shouldReuseExistingAdmin() {
        User existing = User.builder().email("admin@x.com").role(UserRole.ROLE_ADMIN).build();
        when(userRepository.existsByEmail("admin@x.com")).thenReturn(true);
        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.of(existing));

        User admin = seeder.seedUsers("admin@x.com", "admin-pw");

        assertSame(existing, admin);
        verify(userRepository, never()).save(argThat((User u) -> u.getEmail().equals("admin@x.com")));
    }

    @Test
    void seedUsers_shouldSeedAllSixNonAdminUsers() {
        when(userRepository.existsByEmail("admin@x.com")).thenReturn(true);
        when(userRepository.findByEmail("admin@x.com"))
            .thenReturn(Optional.of(User.builder().email("admin@x.com").build()));
        when(userRepository.existsByEmail(eq("hr.user@company.com"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("hr.tech@company.com"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("legal.user@company.com"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("legal.tech@company.com"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("it.user@company.com"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("it.tech@company.com"))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        seeder.seedUsers("admin@x.com", "pw");

        verify(userRepository, times(6)).save(any(User.class));
    }

    @Test
    void seedUsers_shouldSkipNonAdminThatAlreadyExists() {
        when(userRepository.existsByEmail("admin@x.com")).thenReturn(true);
        when(userRepository.findByEmail("admin@x.com"))
            .thenReturn(Optional.of(User.builder().email("admin@x.com").build()));
        when(userRepository.existsByEmail("hr.user@company.com")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        seeder.seedUsers("admin@x.com", "pw");

        verify(userRepository, times(5)).save(any(User.class));
    }

    @Test
    void generateSeedPassword_produces24CharAlphanumericString() throws Exception {
        var method = UserSeeder.class.getDeclaredMethod("generateSeedPassword");
        method.setAccessible(true);

        String password = (String) method.invoke(null);

        assertEquals(24, password.length());
        assertTrue(password.matches("[A-Za-z0-9]+"),
            "Seed password must be alphanumeric, was: " + password);
    }

    @Test
    void generateSeedPassword_producesDifferentValues() throws Exception {
        var method = UserSeeder.class.getDeclaredMethod("generateSeedPassword");
        method.setAccessible(true);

        String a = (String) method.invoke(null);
        String b = (String) method.invoke(null);

        assertNotEquals(a, b, "Two consecutive seeds must not collide");
    }
}