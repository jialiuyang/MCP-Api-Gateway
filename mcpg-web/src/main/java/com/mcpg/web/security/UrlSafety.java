package com.mcpg.web.security;

import com.mcpg.core.exception.McpgException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Validates outbound HTTP(S) URLs before the gateway fetches or proxies them.
 */
@Component
public class UrlSafety {

    private final SsrfProperties props;

    public UrlSafety(SsrfProperties props) {
        this.props = props;
    }

    public URI validateHttpUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new McpgException("URL must not be blank");
        }
        final URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (Exception e) {
            throw new McpgException("Invalid URL: " + raw);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new McpgException("Only http/https URLs are allowed (got scheme='" + scheme + "')");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new McpgException("URL must include a host: " + raw);
        }
        String hostLower = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(hostLower) || hostLower.endsWith(".localhost")
                || "metadata.google.internal".equals(hostLower)) {
            rejectUnlessPrivateAllowed("Host '" + host + "' is not allowed");
        }
        if (!props.getAllowedHosts().isEmpty() && !hostAllowed(hostLower)) {
            throw new McpgException("Host '" + host + "' is not in mcpg.ssrf.allowed-hosts");
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (isBlockedAddress(addr)) {
                    rejectUnlessPrivateAllowed(
                            "Resolved address " + addr.getHostAddress()
                                    + " for host '" + host + "' is not allowed (SSRF guard)");
                }
            }
        } catch (UnknownHostException e) {
            // Host may be offline or a test fixture. Literal private IPs and
            // known-bad hostnames are already rejected above; unresolved names
            // are left to the HTTP client which will fail closed on connect.
        }
        return uri;
    }

    private boolean hostAllowed(String hostLower) {
        for (String pattern : props.getAllowedHosts()) {
            if (pattern == null || pattern.isBlank()) continue;
            String p = pattern.trim().toLowerCase(Locale.ROOT);
            if (p.startsWith("*.")) {
                String suffix = p.substring(1); // ".example.com"
                if (hostLower.endsWith(suffix) || hostLower.equals(p.substring(2))) return true;
            } else if (hostLower.equals(p)) {
                return true;
            }
        }
        return false;
    }

    private void rejectUnlessPrivateAllowed(String message) {
        if (props.isAllowPrivate()) return;
        throw new McpgException(message + ". Set mcpg.ssrf.allow-private=true only for trusted local demos.");
    }

    private static boolean isBlockedAddress(InetAddress addr) {
        return addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()
                || isCloudMetadata(addr)
                || isCarrierGradeNat(addr);
    }

    private static boolean isCloudMetadata(InetAddress addr) {
        byte[] b = addr.getAddress();
        // 169.254.169.254
        return b.length == 4
                && (b[0] & 0xff) == 169
                && (b[1] & 0xff) == 254
                && (b[2] & 0xff) == 169
                && (b[3] & 0xff) == 254;
    }

    private static boolean isCarrierGradeNat(InetAddress addr) {
        byte[] b = addr.getAddress();
        // 100.64.0.0/10
        return b.length == 4
                && (b[0] & 0xff) == 100
                && ((b[1] & 0xff) >= 64 && (b[1] & 0xff) <= 127);
    }
}
