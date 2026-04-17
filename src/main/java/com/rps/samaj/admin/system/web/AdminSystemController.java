package com.rps.samaj.admin.system.web;

import com.rps.samaj.admin.system.ChildAdminManagementService;
import com.rps.samaj.api.dto.AdminSystemDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/system")
public class AdminSystemController {

    private final UserRepository userRepository;
    private final ChildAdminManagementService childAdminManagementService;

    public AdminSystemController(UserRepository userRepository, ChildAdminManagementService childAdminManagementService) {
        this.userRepository = userRepository;
        this.childAdminManagementService = childAdminManagementService;
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
}
