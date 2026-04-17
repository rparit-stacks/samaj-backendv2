package com.rps.samaj.security;

import io.jsonwebtoken.JwtException;
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
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveBearer(request);
        if (token != null && !token.isBlank()) {
            try {
                JwtService.ParsedJwt parsed = jwtService.parse(token);
                if (!JwtService.TYP_ACCESS.equals(parsed.typ())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String authority = "ROLE_" + parsed.role().name();
                var auth = new UsernamePasswordAuthenticationToken(
                        parsed.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(DevUserContextFilter.ATTR_USER_ID, parsed.userId());
                if (parsed.adminCapable()) {
                    request.setAttribute(DevUserContextFilter.ATTR_ADMIN_USER_ID, parsed.userId());
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String resolveBearer(HttpServletRequest request) {
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return h.substring(7).trim();
        }
        String x = request.getHeader("X-Auth-Token");
        if (x != null && x.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return x.substring(7).trim();
        }
        if (x != null && !x.isBlank()) {
            return x.trim();
        }
        return null;
    }

    public static UUID currentUserIdOrNull() {
        var ctx = SecurityContextHolder.getContext().getAuthentication();
        if (ctx == null || !ctx.isAuthenticated()) {
            return null;
        }
        Object p = ctx.getPrincipal();
        if (p instanceof UUID u) {
            return u;
        }
        return null;
    }
}
