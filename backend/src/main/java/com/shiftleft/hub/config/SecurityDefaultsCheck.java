package com.shiftleft.hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup check that fails the application if it boots with a known
 * weak default for any security-sensitive property. Each check is
 * mirrored in a focused unit test.
 *
 * <p>Companion to {@code JwtService.validateSecret} for non-JWT
 * properties. Kept in a single class so the policy is in one place.
 */
@Component
@Profile("!test")
public class SecurityDefaultsCheck {

    private static final List<String> FORBIDDEN_PASSWORD_FRAGMENTS = List.of(
        "change-me", "changeme", "your-password", "yourpassword"
    );

    private static final List<String> FORBIDDEN_PASSWORD_EXACT = List.of(
        "shiftleft", "password", "postgres", "admin", "root", "test"
    );

    private final String dbPassword;
    private final String aiEncryptionSalt;

    public SecurityDefaultsCheck(
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.ai.encryption-salt:}") String aiEncryptionSalt) {
        this.dbPassword = dbPassword == null ? "" : dbPassword;
        this.aiEncryptionSalt = aiEncryptionSalt == null ? "" : aiEncryptionSalt;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        validateDbPassword();
        validateAiSalt();
    }

    void validateDbPassword() {
        if (dbPassword.isEmpty()) {
            return; // some embedded profiles have no password
        }
        String lower = dbPassword.toLowerCase();
        for (String exact : FORBIDDEN_PASSWORD_EXACT) {
            if (lower.equals(exact)) {
                throw new IllegalStateException(
                    "spring.datasource.password is the well-known default '"
                        + exact + "'. Set DB_PASSWORD to a non-default value.");
            }
        }
        for (String fragment : FORBIDDEN_PASSWORD_FRAGMENTS) {
            if (lower.contains(fragment)) {
                throw new IllegalStateException(
                    "spring.datasource.password contains a placeholder fragment '"
                        + fragment + "'. Set DB_PASSWORD to a non-default value.");
            }
        }
    }

    void validateAiSalt() {
        if (aiEncryptionSalt.isEmpty()) {
            return;
        }
        // The default in application.properties is "ShiftLeftKBSalt"
        if (aiEncryptionSalt.equals("ShiftLeftKBSalt")) {
            throw new IllegalStateException(
                "app.ai.encryption-salt is the well-known default 'ShiftLeftKBSalt'."
                    + " Set APP_AI_ENCRYPTION_SALT to a non-default value.");
        }
    }
}
