package com.shiftleft.hub.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for authentication cookies.
 * Binds the {@code app.auth.cookie.secure} and {@code app.auth.cookie.same-site}
 * properties and validates them at startup.
 */
@ConfigurationProperties(prefix = "app.auth.cookie")
public class AuthCookieProperties {

    private boolean secure = false;

    private String sameSite = "Lax";

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    @PostConstruct
    void validate() {
        if (!"Strict".equalsIgnoreCase(sameSite)
                && !"Lax".equalsIgnoreCase(sameSite)
                && !"None".equalsIgnoreCase(sameSite)) {
            throw new IllegalStateException(
                "app.auth.cookie.same-site must be Strict, Lax, or None. Got: " + sameSite);
        }
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                "app.auth.cookie.same-site=None requires app.auth.cookie.secure=true. "
                + "Browsers reject SameSite=None cookies without the Secure flag.");
        }
    }
}
