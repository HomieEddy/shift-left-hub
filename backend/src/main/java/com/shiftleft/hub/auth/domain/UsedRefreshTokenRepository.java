package com.shiftleft.hub.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedRefreshTokenRepository extends JpaRepository<UsedRefreshToken, UUID> {
    Optional<UsedRefreshToken> findByTokenId(String tokenId);
    void deleteByExpiresAtBefore(Instant instant);
}
