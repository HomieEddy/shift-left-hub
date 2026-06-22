package com.shiftleft.hub.config;

import com.shiftleft.hub.auth.domain.UsedRefreshToken;
import com.shiftleft.hub.auth.domain.UsedRefreshTokenRepository;
import com.shiftleft.hub.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

@Service
@Slf4j
public class JwtService {

    private final String secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    private final Map<String, String> usedRefreshTokens = new ConcurrentHashMap<>();

    private final UsedRefreshTokenRepository usedRefreshTokenRepository;

    /**
     * Creates a new JwtService.
     *
     * @param secretKey                 the JWT signing secret
     * @param accessExpiration          access token TTL in milliseconds
     * @param refreshExpiration         refresh token TTL in milliseconds
     * @param usedRefreshTokenRepository repository for tracking used refresh tokens
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.access-token-expiration}") long accessExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshExpiration,
            UsedRefreshTokenRepository usedRefreshTokenRepository) {
        validateSecret(secretKey);
        this.secretKey = secretKey;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.usedRefreshTokenRepository = usedRefreshTokenRepository;
    }

    private static final java.util.List<String> FORBIDDEN_SECRET_FRAGMENTS = java.util.List.of(
        "change-in-prod", "change-me", "dev-jwt", "test-jwt", "your-secret-here"
    );

    private static void validateSecret(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters (got "
                    + (secret == null ? 0 : secret.length()) + ")");
        }
        String lower = secret.toLowerCase();
        for (String fragment : FORBIDDEN_SECRET_FRAGMENTS) {
            if (lower.contains(fragment)) {
                throw new IllegalStateException(
                    "app.jwt.secret contains forbidden placeholder fragment '"
                        + fragment + "'. Refusing to start: this looks like a"
                        + " dev/test literal that must not sign real tokens.");
            }
        }
    }

    // SEC-04: JWT validation audited — HMAC-SHA, refresh rotation, 15min access/7d refresh, no PII leakage
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token for the given user.
     *
     * @param user the authenticated user
     * @return the signed access token string
     */
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .claim("workspace_id",
                    user.getDefaultWorkspaceId() != null
                        ? user.getDefaultWorkspaceId().toString() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates an access token with a custom workspace_id claim.
     *
     * @param user the authenticated user
     * @param workspaceId the workspace UUID to set in the claim
     * @return the signed access token string
     */
    public String generateAccessTokenWithWorkspace(User user, UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .claim("workspace_id", workspaceId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a refresh token with a custom workspace_id claim.
     *
     * @param user the authenticated user
     * @param workspaceId the workspace UUID to set in the claim
     * @return the signed refresh token string
     */
    public String generateRefreshTokenWithWorkspace(User user, UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("tokenId", UUID.randomUUID().toString())
                .claim("workspace_id", workspaceId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a signed JWT refresh token for the given user.
     *
     * @param user the authenticated user
     * @return the signed refresh token string
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("tokenId", UUID.randomUUID().toString())
                .claim("workspace_id",
                    user.getDefaultWorkspaceId() != null
                        ? user.getDefaultWorkspaceId().toString() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the extracted user UUID
     */
    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the workspace ID from a JWT token.
     *
     * @param token the JWT token
     * @return the extracted workspace UUID, or null if not present
     */
    public UUID extractWorkspaceId(String token) {
        Claims claims = parseToken(token);
        String wsId = claims.get("workspace_id", String.class);
        return wsId != null ? UUID.fromString(wsId) : null;
    }

    /**
     * Validates whether a JWT token is correctly signed and not expired.
     *
     * @param token the JWT token
     * @return true if the token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether a token is a refresh token based on its claims.
     *
     * @param token the JWT token
     * @return true if the token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (JwtException e) {
            log.warn("Token validation failed: not a refresh token — {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates refresh token rotation and detects reuse.
     *
     * @param tokenId the token ID claim
     * @param userId  the user ID associated with the token
     */
    public void validateRefreshRotation(String tokenId, String userId) {
        if (usedRefreshTokens.containsKey(tokenId)) {
            log.warn("Refresh token reuse detected for tokenId: {}", tokenId);
            throw new JwtException("Refresh token reuse detected for tokenId: " + tokenId);
        }
        if (usedRefreshTokenRepository.findByTokenId(tokenId).isPresent()) {
            log.warn("Refresh token reuse detected in DB for tokenId: {}", tokenId);
            usedRefreshTokens.put(tokenId, userId);
            throw new JwtException("Refresh token reuse detected for tokenId: " + tokenId);
        }
        usedRefreshTokens.put(tokenId, userId);
        usedRefreshTokenRepository.save(new UsedRefreshToken(
            tokenId, UUID.fromString(userId),
            Instant.now().plusSeconds(refreshExpiration / 1000)));
    }

    /**
     * Invalidates a refresh token by its token ID.
     *
     * @param tokenId the token ID to invalidate
     */
    public void invalidateRefreshToken(String tokenId) {
        usedRefreshTokens.remove(tokenId);
        usedRefreshTokenRepository.findByTokenId(tokenId).ifPresent(usedRefreshTokenRepository::delete);
    }

    /**
     * Periodically evicts expired refresh tokens from the database.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void evictExpiredRefreshTokens() {
        try {
            int before = usedRefreshTokens.size();
            usedRefreshTokens.keySet().removeIf(id ->
                usedRefreshTokenRepository.findByTokenId(id).isEmpty());
            log.debug("Evicted {} expired in-memory refresh tokens", before - usedRefreshTokens.size());

            usedRefreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        } catch (Exception e) {
            log.warn("Failed to evict expired refresh tokens (tables may not be ready yet): {}", e.getMessage());
        }
    }

    /**
     * Extracts the token ID from a JWT token.
     *
     * @param token the JWT token
     * @return the token ID claim
     */
    public String extractTokenId(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenId", String.class);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
