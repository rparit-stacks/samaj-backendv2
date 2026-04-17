package com.rps.samaj.admin.system;

import com.rps.samaj.security.JwtService;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminAuthorizationService {

    private final AdminServiceGrantRepository grantRepository;

    public AdminAuthorizationService(AdminServiceGrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    /**
     * Main admins (not scoped moderators) may access all existing and future service admin APIs.
     */
    public boolean hasFullAdminServiceAccess(User user) {
        return user.getRole() == UserRole.ADMIN || user.isParentAdmin();
    }

    /**
     * Create/list/update child admins and the service catalog.
     */
    public boolean canManageChildAdmins(User user) {
        return user.getRole() == UserRole.ADMIN || user.isParentAdmin();
    }

    public boolean isModerator(User user) {
        return user.getRole() == UserRole.MODERATOR;
    }

    public boolean canAccessAdminPath(User user, String servletPath) {
        if (servletPath == null) {
            return false;
        }
        String path = servletPath.startsWith("/") ? servletPath : "/" + servletPath;
        if (!path.startsWith("/admin")) {
            return true;
        }
        if (!JwtService.adminCapable(user)) {
            return false;
        }
        if (path.startsWith("/admin/system/me")) {
            return true;
        }
        if (path.startsWith("/admin/system/")) {
            return canManageChildAdmins(user);
        }
        if (path.startsWith("/admin/users")) {
            return hasFullAdminServiceAccess(user);
        }
        if (hasFullAdminServiceAccess(user)) {
            return true;
        }
        if (!isModerator(user)) {
            return false;
        }
        Optional<AdminServiceKey> key = AdminPathRegistry.resolveServiceKey(path);
        return key.filter(k -> grantRepository.existsByUser_IdAndServiceKey(user.getId(), k)).isPresent();
    }
}
