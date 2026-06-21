package com.shiftleft.hub.ai.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

/**
 * Validates an LLM endpoint URL before the app uses it for outbound
 * HTTP. Rejects:
 *
 * <ul>
 *   <li>non-http(s) schemes (file://, jar:, gopher:, etc.)</li>
 *   <li>loopback (127.0.0.0/8, ::1)</li>
 *   <li>link-local (169.254.0.0/16, fe80::/10) — AWS / GCP metadata</li>
 *   <li>private RFC1918 ranges (10/8, 172.16/12, 192.168/16, fc00::/7)</li>
 *   <li>any host that resolves to one of the above</li>
 * </ul>
 *
 * <p>Operators that need to point the LLM at an internal endpoint
 * can set {@code app.ai.endpoint.allow-private-hosts=true} to
 * opt-in. This is a deliberate fail-secure default: a misconfigured
 * or malicious admin input cannot make the backend fetch from
 * internal services.
 */
@Component
public class EndpointUrlValidator {

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
        "localhost",
        "metadata.google.internal",
        "metadata"
    );

    private final boolean allowPrivateHosts;

    /**
     * Creates a validator.
     *
     * @param allowPrivateHosts whether to allow endpoints on loopback, link-local,
     *                          or RFC1918 private networks. Default false.
     */
    public EndpointUrlValidator(
            @org.springframework.beans.factory.annotation.Value(
                "${app.ai.endpoint.allow-private-hosts:false}") boolean allowPrivateHosts) {
        this.allowPrivateHosts = allowPrivateHosts;
    }

    /**
     * Validates the given URL for safe outbound use. Throws
     * {@link IllegalArgumentException} on non-http(s) schemes,
     * blocked hostnames, or hostnames resolving to loopback,
     * link-local, or RFC1918 private addresses.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL is unsafe
     */
    public void requireSafe(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL must not be blank");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Endpoint URL is not a valid URI: " + url, e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                "Endpoint URL must use http or https scheme, got: " + scheme);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL must include a host");
        }
        if (!allowPrivateHosts) {
            if (BLOCKED_HOSTNAMES.contains(host.toLowerCase())) {
                throw new IllegalArgumentException(
                    "Endpoint host is blocked: " + host
                        + " (set app.ai.endpoint.allow-private-hosts=true to override)");
            }
            try {
                InetAddress addr = InetAddress.getByName(host);
                if (addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress()
                    || addr.isSiteLocalAddress()) {
                    throw new IllegalArgumentException(
                        "Endpoint host resolves to a loopback/link-local/private address: "
                            + host + " -> " + addr.getHostAddress()
                            + " (set app.ai.endpoint.allow-private-hosts=true to override)");
                }
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(
                    "Endpoint host could not be resolved: " + host, e);
            }
        }
    }
}
