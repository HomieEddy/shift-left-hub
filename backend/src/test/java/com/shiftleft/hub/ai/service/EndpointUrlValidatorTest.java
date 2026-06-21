package com.shiftleft.hub.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndpointUrlValidatorTest {

    private EndpointUrlValidator strict() {
        return new EndpointUrlValidator(false);
    }

    private EndpointUrlValidator permissive() {
        return new EndpointUrlValidator(true);
    }

    // ── Scheme ────────────────────────────────────────────────

    @Test
    void acceptsHttps() {
        assertDoesNotThrow(() -> strict().requireSafe("https://api.openai.com/v1"));
    }

    @Test
    void acceptsHttp() {
        assertDoesNotThrow(() -> strict().requireSafe("http://example.com"));
    }

    @Test
    void rejectsFileScheme() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("file:///etc/passwd"));
    }

    @Test
    void rejectsJarScheme() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("jar:http://evil.com/x.jar!/"));
    }

    @Test
    void rejectsMissingScheme() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("api.openai.com/v1"));
    }

    // ── Hostname ──────────────────────────────────────────────

    @Test
    void rejectsLocalhost() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://localhost:11434"));
    }

    @Test
    void rejectsLoopbackIp() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://127.0.0.1:11434"));
    }

    @Test
    void rejectsCloudMetadataHost() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://metadata.google.internal/computeMetadata/v1/"));
    }

    @Test
    void rejectsLinkLocalIp() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void rejectsRfc1918PrivateIp() {
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://10.0.0.5:11434"));
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://192.168.1.1"));
        assertThrows(IllegalArgumentException.class,
            () -> strict().requireSafe("http://172.16.0.1"));
    }

    // ── Blank / null / parse errors ───────────────────────────

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> strict().requireSafe(""));
        assertThrows(IllegalArgumentException.class, () -> strict().requireSafe(null));
        assertThrows(IllegalArgumentException.class, () -> strict().requireSafe("   "));
    }

    @Test
    void rejectsMalformedUri() {
        assertThrows(IllegalArgumentException.class, () -> strict().requireSafe("http://["));
    }

    // ── Opt-in override ───────────────────────────────────────

    @Test
    void permissiveAllowsLoopbackWhenEnabled() {
        assertDoesNotThrow(() -> permissive().requireSafe("http://localhost:11434"));
        assertDoesNotThrow(() -> permissive().requireSafe("http://10.0.0.5:11434"));
    }
}
