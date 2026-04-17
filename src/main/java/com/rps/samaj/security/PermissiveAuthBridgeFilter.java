package com.rps.samaj.security;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.model.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * When {@code samaj.dev.permissive-auth=true}, authenticates from {@code X-Admin-User-Id} or {@code X-User-Id}
 * if no JWT was applied. Production must keep this {@code false}.
 */
@Component
public class PermissiveAuthBridgeFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Value("${samaj.dev.permissive-auth:false}")
    private boolean permissiveAuth;

    public PermissiveAuthBridgeFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (permissiveAuth && needsBridge()) {
            String adminHeader = request.getHeader("X-Admin-User-Id");
            if (adminHeader != null && !adminHeader.isBlank()) {
                try {
                    UUID id = UUID.fromString(adminHeader.trim());
                    if (tryAuthenticate(request, id, true)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            String userHeader = request.getHeader("X-User-Id");
            if (userHeader != null && !userHeader.isBlank()) {
                try {
                    UUID id = UUID.fromString(userHeader.trim());
                    tryAuthenticate(request, id, false);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean needsBridge() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken;
    }

    /**
     * @param requireAdminCapable if true (X-Admin-User-Id path), only admin/moderator/parentAdmin may authenticate
     * @return true if authentication was set
     */
    private boolean tryAuthenticate(HttpServletRequest request, UUID userId, boolean requireAdminCapable) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        if (requireAdminCapable && !JwtService.adminCapable(user)) {
            return false;
        }
        var auth = new UsernamePasswordAuthenticationToken(
                user.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.setAttribute(DevUserContextFilter.ATTR_USER_ID, user.getId());
        if (JwtService.adminCapable(user)) {
            request.setAttribute(DevUserContextFilter.ATTR_ADMIN_USER_ID, user.getId());
        }
        return true;
    }
}
