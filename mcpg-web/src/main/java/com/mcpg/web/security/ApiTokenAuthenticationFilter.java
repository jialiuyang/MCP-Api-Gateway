package com.mcpg.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Accepts {@code Authorization: Bearer &lt;token&gt;} or {@code X-MCPG-Token}.
 */
@Component
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_TOKEN = "X-MCPG-Token";

    private final SecurityProperties properties;

    public ApiTokenAuthenticationFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (properties.isEnabled()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String presented = extractToken(request);
            String expected = properties.getApiToken();
            if (presented != null && expected != null && !expected.isBlank()
                    && constantTimeEquals(presented, expected)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "mcpg-operator",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_TOKEN);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank()) {
            return null;
        }
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }
}
