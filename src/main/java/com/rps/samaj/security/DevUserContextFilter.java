package com.rps.samaj.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * When {@code samaj.dev.permissive-auth=true}, sets request attributes from headers for local testing
 * until JWT is wired. Production must keep this {@code false}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DevUserContextFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "samajUserId";
    public static final String ATTR_ADMIN_USER_ID = "samajAdminUserId";

    @Value("${samaj.dev.permissive-auth:false}")
    private boolean permissiveAuth;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (permissiveAuth) {
            String u = request.getHeader("X-User-Id");
            if (u != null && !u.isBlank()) {
                try {
                    request.setAttribute(ATTR_USER_ID, UUID.fromString(u.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            String a = request.getHeader("X-Admin-User-Id");
            if (a != null && !a.isBlank()) {
                try {
                    request.setAttribute(ATTR_ADMIN_USER_ID, UUID.fromString(a.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
