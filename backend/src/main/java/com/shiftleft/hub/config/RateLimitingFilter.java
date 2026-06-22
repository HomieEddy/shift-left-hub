package com.shiftleft.hub.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Cache<String, Integer> requestCounts;
    private final boolean enabled;
    private final boolean trustForwardedFor;

    /**
     * Creates a rate limiting filter that limits auth requests to 10 per minute per IP.
     *
     * @param enabled           whether rate limiting is active
     * @param trustForwardedFor whether to honor the X-Forwarded-For header when
     *                          present. Enable ONLY when the app is behind a
     *                          trusted reverse proxy that overwrites the header
     *                          on incoming requests; otherwise an attacker can
     *                          set the header to a random value and bypass the
     *                          per-IP throttle.
     */
    public RateLimitingFilter(
            @Value("${app.rate-limiting.enabled:true}") boolean enabled,
            @Value("${app.rate-limiting.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            String clientIp = resolveClientIp(request);
            Integer count = requestCounts.get(clientIp, k -> 0);
            if (count >= 10) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"error\":\"Too many requests — please try again in 60 seconds\"}");
                return;
            }
            requestCounts.put(clientIp, count + 1);
        }
        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                String first = (comma > 0 ? xff.substring(0, comma) : xff).trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
