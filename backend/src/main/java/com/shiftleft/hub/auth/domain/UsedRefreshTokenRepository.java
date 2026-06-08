package com.shiftleft.hub.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for used refresh tokens.
 */
public interface UsedRefreshTokenRepository extends JpaRepository<UsedRefreshToken, UUID> {

    Optional<UsedRefreshToken> findByTokenId(String tokenId);

    void deleteByExpiresAtBefore(Instant instant);
}
