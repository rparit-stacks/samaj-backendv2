package com.rps.samaj.admin.system.web;

import com.rps.samaj.admin.system.AdminInvitationService;
import com.rps.samaj.admin.system.ChildAdminManagementService;
import com.rps.samaj.api.dto.AdminSystemDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/system")
public class AdminSystemController {

    private final UserRepository userRepository;
    private final ChildAdminManagementService childAdminManagementService;
    private final AdminInvitationService adminInvitationService;

    public AdminSystemController(
            UserRepository userRepository,
            ChildAdminManagementService childAdminManagementService,
            AdminInvitationService adminInvitationService
    ) {
        this.userRepository = userRepository;
        this.childAdminManagementService = childAdminManagementService;
        this.adminInvitationService = adminInvitationService;
    }

    @GetMapping("/me")
    public AdminSystemDtos.AdminMeResponse me(Authentication auth) {
        User user = loadUser(requireUserId(auth));
        return childAdminManagementService.me(user);
    }

    @GetMapping("/catalog")
    public List<AdminSystemDtos.ServiceCatalogEntry> catalog(Authentication auth) {
        requireUserId(auth);
        return childAdminManagementService.catalog();
    }

    // ── Child admin CRUD (direct creation with password) ──────────────────────

    @GetMapping("/child-admins")
    public AdminSystemDtos.ChildAdminPageResponse listChildAdmins(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireUserId(auth);
        return childAdminManagementService.listChildAdmins(page, size);
    }

    @GetMapping("/child-admins/{id}")
    public AdminSystemDtos.ChildAdminSummaryResponse getChildAdmin(Authentication auth, @PathVariable UUID id) {
        requireUserId(auth);
        return childAdminManagementService.getChildAdmin(id);
    }

    @PostMapping("/child-admins")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSystemDtos.ChildAdminSummaryResponse createChildAdmin(
            Authentication auth,
            @Valid @RequestBody AdminSystemDtos.ChildAdminCreateRequest body
    ) {
        requireUserId(auth);
        return childAdminManagementService.createChildAdmin(body);
    }

    @PutMapping("/child-admins/{id}")
    public AdminSystemDtos.ChildAdminSummaryResponse updateChildAdmin(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody AdminSystemDtos.ChildAdminUpdateRequest body
    ) {
        requireUserId(auth);
        return childAdminManagementService.updateChildAdmin(id, body);
    }

    // ── Invitation flow (email-based, OTP-verified) ───────────────────────────

    @PostMapping("/child-admins/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSystemDtos.InvitationResponse inviteChildAdmin(
            Authentication auth,
            @Valid @RequestBody AdminSystemDtos.ChildAdminInviteRequest body,
            HttpServletRequest request
    ) {
        UUID userId = requireUserId(auth);
        User user = loadUser(userId);
        if (!user.isParentAdmin() && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the parent admin can invite sub-admins");
        }
        String origin = resolveOrigin(request);
        return adminInvitationService.createInvitation(body, userId, origin);
    }

    @GetMapping("/invitations")
    public List<AdminSystemDtos.InvitationResponse> listPendingInvitations(Authentication auth) {
        requireUserId(auth);
        return adminInvitationService.listPending();
    }

    @DeleteMapping("/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(Authentication auth, @PathVariable UUID id) {
        requireUserId(auth);
        adminInvitationService.cancelInvitation(id);
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private User loadUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static UUID requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }

    private static String resolveOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) return origin.trim();
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                java.net.URL url = new java.net.URL(referer);
                int port = url.getPort();
                return url.getProtocol() + "://" + url.getHost() + (port > 0 ? ":" + port : "");
            } catch (Exception ignored) {
            }
        }
        int port = request.getServerPort();
        boolean defaultPort = (port == 80 && "http".equals(request.getScheme()))
                || (port == 443 && "https".equals(request.getScheme()));
        return request.getScheme() + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }
}
