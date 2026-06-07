package com.shiftleft.hub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

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

    protected UsedRefreshToken() {}

    public UsedRefreshToken(String tokenId, UUID userId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public String getTokenId() { return tokenId; }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
