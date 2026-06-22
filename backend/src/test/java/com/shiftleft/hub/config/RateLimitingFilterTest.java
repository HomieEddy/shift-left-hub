package com.shiftleft.hub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitingFilterTest {

    private FilterChain chain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        chain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    private MockHttpServletRequest authRequest(String remoteAddr, String xff) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(remoteAddr);
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        return req;
    }

    @Test
    void usesRemoteAddrWhenTrustForwardedForDisabled() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, false);
        HttpServletRequest req = authRequest("10.0.0.1", "203.0.113.50");

        filter.doFilter(req, response, chain);

        verify(chain, times(1)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void usesFirstXffEntryWhenTrustForwardedForEnabled() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, true);
        HttpServletRequest req = authRequest("10.0.0.1", "203.0.113.50, 10.0.0.2");

        filter.doFilter(req, response, chain);

        verify(chain, times(1)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void throttlesAfterTenRequestsFromSameKey() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, true);
        for (int i = 0; i < 10; i++) {
            filter.doFilter(authRequest("10.0.0.1", "203.0.113.50"),
                new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse eleventh = new MockHttpServletResponse();
        filter.doFilter(authRequest("10.0.0.1", "203.0.113.50"),
            eleventh, chain);

        assertEquals(429, eleventh.getStatus());
    }

    @Test
    void differentXffEntriesAreSeparateBuckets() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, true);
        for (int i = 0; i < 10; i++) {
            filter.doFilter(authRequest("10.0.0.1", "203.0.113.50"),
                new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse different = new MockHttpServletResponse();
        filter.doFilter(authRequest("10.0.0.1", "203.0.113.99"),
            different, chain);

        assertEquals(200, different.getStatus());
    }

    @Test
    void xffIgnoredWhenTrustForwardedForDisabled() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, false);
        for (int i = 0; i < 10; i++) {
            filter.doFilter(authRequest("10.0.0.1", "203.0.113.50"),
                new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse rotated = new MockHttpServletResponse();
        filter.doFilter(authRequest("10.0.0.1", "203.0.113.99"),
            rotated, chain);

        // Without XFF trust, the bucket key is the remoteAddr (the proxy),
        // so all 11 requests share the same bucket and the 11th is throttled.
        assertEquals(429, rotated.getStatus());
    }
}
