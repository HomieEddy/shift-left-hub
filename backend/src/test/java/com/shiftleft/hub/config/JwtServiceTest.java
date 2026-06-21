package com.shiftleft.hub.config;

import com.shiftleft.hub.auth.domain.UsedRefreshTokenRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JwtServiceTest {

    private static final String GOOD_SECRET =
        "a-strong-production-secret-with-32-plus-chars-aabbccdd";

    private static JwtService newService(String secret) {
        return new JwtService(secret, 3600000L, 604800000L,
            mock(UsedRefreshTokenRepository.class));
    }

    @Test
    void constructor_acceptsStrongSecret() {
        assertDoesNotThrow(() -> newService(GOOD_SECRET));
    }

    @Test
    void constructor_rejectsNullSecret() {
        assertThrows(IllegalStateException.class, () -> newService(null));
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> newService("too-short"));
    }

    @Test
    void constructor_rejectsDevLiteral() {
        assertThrows(IllegalStateException.class, () -> newService(
            "shiftleft-dev-jwt-secret-change-in-prod-256-bits-long"));
    }

    @Test
    void constructor_rejectsChangeInProdFragment() {
        assertThrows(IllegalStateException.class, () -> newService(
            "production-secret-please-change-in-prod-before-deploy"));
    }

    @Test
    void constructor_rejectsChangeMeFragment() {
        assertThrows(IllegalStateException.class, () -> newService(
            "change-me-to-something-strong-aaaaaaaaaaaaaaaa"));
    }

    @Test
    void constructor_rejectsTestJwtFragment() {
        assertThrows(IllegalStateException.class, () -> newService(
            "some-test-jwt-secret-only-for-local-dev-aaaa"));
    }

    @Test
    void constructor_isCaseInsensitiveOnForbiddenFragments() {
        assertThrows(IllegalStateException.class, () -> newService(
            "SHIFTLEFT-DEV-JWT-SECRET-CHANGE-IN-PROD-256-BITS-LONG"));
    }
}
