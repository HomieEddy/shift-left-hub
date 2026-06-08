package com.shiftleft.hub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks already-used refresh token IDs to prevent replay attacks.
 */
@Entity
@Table(name = "used_refresh_token")
public class UsedRefreshToken {

    @Id
    private UUID id;

    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsedRefreshToken() {
    }

    /**
     * Create a new used-token record.
     *
     * @param tokenId the JWT token ID
     * @param userId the owning user's UUID
     * @param expiresAt when this record expires
     */
    public UsedRefreshToken(String tokenId, UUID userId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public String getTokenId() {
        return tokenId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
