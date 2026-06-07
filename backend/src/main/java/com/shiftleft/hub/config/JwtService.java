package com.shiftleft.hub.config;

import com.shiftleft.hub.auth.domain.UsedRefreshToken;
import com.shiftleft.hub.auth.domain.UsedRefreshTokenRepository;
import com.shiftleft.hub.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    private final String secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    private final Map<String, String> usedRefreshTokens = new ConcurrentHashMap<>();

    private final UsedRefreshTokenRepository usedRefreshTokenRepository;

    public JwtService(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.access-token-expiration}") long accessExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshExpiration,
            UsedRefreshTokenRepository usedRefreshTokenRepository) {
        this.secretKey = secretKey;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.usedRefreshTokenRepository = usedRefreshTokenRepository;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("tokenId", UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (JwtException e) {
            log.warn("Token validation failed: not a refresh token — {}", e.getMessage());
            return false;
        }
    }

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

    public void invalidateRefreshToken(String tokenId) {
        usedRefreshTokens.remove(tokenId);
        usedRefreshTokenRepository.findByTokenId(tokenId).ifPresent(usedRefreshTokenRepository::delete);
    }

    @Scheduled(fixedRate = 300_000)
    public void evictExpiredRefreshTokens() {
        usedRefreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

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
