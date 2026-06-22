package com.shiftleft.hub.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityDefaultsCheckTest {

    private SecurityDefaultsCheck newCheck(String dbPassword, String aiSalt) {
        return new SecurityDefaultsCheck(dbPassword, aiSalt);
    }

    // ── DB password ───────────────────────────────────────────

    @Test
    void dbPassword_acceptsStrongPassword() {
        assertDoesNotThrow(() -> newCheck("a-strong-random-production-password", "x").validateDbPassword());
    }

    @Test
    void dbPassword_acceptsEmpty() {
        assertDoesNotThrow(() -> newCheck("", "x").validateDbPassword());
    }

    @Test
    void dbPassword_rejectsShiftleft() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("shiftleft", "x").validateDbPassword());
    }

    @Test
    void dbPassword_rejectsPassword() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("password", "x").validateDbPassword());
    }

    @Test
    void dbPassword_rejectsPostgres() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("postgres", "x").validateDbPassword());
    }

    @Test
    void dbPassword_rejectsChangeMe() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("change-me-to-something-strong", "x").validateDbPassword());
    }

    @Test
    void dbPassword_isCaseInsensitiveOnDefault() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("ShiftLeft", "x").validateDbPassword());
    }

    // ── AI salt ───────────────────────────────────────────────

    @Test
    void aiSalt_acceptsNonDefault() {
        assertDoesNotThrow(() -> newCheck("x", "a-random-salt-not-the-default").validateAiSalt());
    }

    @Test
    void aiSalt_rejectsDefaultLiteral() {
        assertThrows(IllegalStateException.class,
            () -> newCheck("x", "ShiftLeftKBSalt").validateAiSalt());
    }
}
