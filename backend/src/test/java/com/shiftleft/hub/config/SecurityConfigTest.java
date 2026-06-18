package com.shiftleft.hub.config;

import com.shiftleft.hub.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsConfigurationSource_shouldOnlyAllowExplicitConfiguredOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
            mock(UserRepository.class),
            mock(JwtService.class),
            new RateLimitingFilter());
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
            new String[] {"http://localhost:4200", "https://trusted.vercel.app"});

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles");
        CorsConfiguration corsConfig = securityConfig.corsConfigurationSource()
            .getCorsConfiguration(request);

        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowCredentials());
        assertEquals("https://trusted.vercel.app",
            corsConfig.checkOrigin("https://trusted.vercel.app"));
        assertNull(corsConfig.checkOrigin("https://attacker.vercel.app"));
        assertTrue(corsConfig.getAllowedOriginPatterns() == null
            || corsConfig.getAllowedOriginPatterns().isEmpty());
    }

    @Test
    void corsConfigurationSource_shouldRejectEmptyOriginsWhenCredentialsAreAllowed() {
        SecurityConfig securityConfig = new SecurityConfig(
            mock(UserRepository.class),
            mock(JwtService.class),
            new RateLimitingFilter());
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", new String[] {"   ", ""});

        assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);
    }

    @Test
    void corsConfigurationSource_shouldRejectWildcardOriginWhenCredentialsAreAllowed() {
        SecurityConfig securityConfig = new SecurityConfig(
            mock(UserRepository.class),
            mock(JwtService.class),
            new RateLimitingFilter());
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", new String[] {"*"});

        assertThrows(IllegalArgumentException.class, securityConfig::corsConfigurationSource);
    }
}
