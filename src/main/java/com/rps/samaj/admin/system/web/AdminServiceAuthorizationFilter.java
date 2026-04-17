package com.rps.samaj.admin.system.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.admin.system.AdminAuthorizationService;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces per-service access for {@code MODERATOR} (child) admins on {@code /admin/**}.
 */
@Component
public class AdminServiceAuthorizationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ObjectMapper objectMapper;

    public AdminServiceAuthorizationFilter(
            UserRepository userRepository,
            AdminAuthorizationService adminAuthorizationService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UUID userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            writeForbidden(response, "User not found");
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            writeForbidden(response, "Account is not active");
            return;
        }
        String path = request.getServletPath();
        if (!adminAuthorizationService.canAccessAdminPath(user, path)) {
            writeForbidden(response, "Insufficient admin permissions for this area");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("error", message));
    }
}
