package com.rps.samaj.emergency.web;

import com.rps.samaj.api.dto.EmergencyDtos;
import com.rps.samaj.emergency.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/emergencies")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    public List<EmergencyDtos.EmergencyItemResponse> list(
            @RequestParam(required = false) UUID creatorUserId
    ) {
        return emergencyService.listForUser(creatorUserId);
    }

    @GetMapping("/me")
    public List<EmergencyDtos.EmergencyItemResponse> listMine(Authentication auth) {
        return emergencyService.listMine(requireUser(auth));
    }

    @GetMapping("/dashboard")
    public EmergencyDtos.DashboardStatsResponse dashboard(Authentication auth) {
        return emergencyService.dashboardStats(requireUser(auth));
    }

    @GetMapping("/helpers/{userId}/stats")
    public EmergencyDtos.HelperStatsResponse helperStats(
            Authentication auth,
            @PathVariable("userId") UUID userId
    ) {
        requireUser(auth);
        return emergencyService.helperStats(userId);
    }

    @GetMapping("/{id}")
    public EmergencyDtos.EmergencyItemResponse getById(@PathVariable long id) {
        return emergencyService.getById(id);
    }

    @GetMapping("/{id}/helpers")
    public List<EmergencyDtos.EmergencyHelpItemResponse> listHelpers(@PathVariable long id) {
        return emergencyService.listHelpers(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyDtos.EmergencyItemResponse create(
            Authentication auth,
            @Valid @RequestBody EmergencyDtos.EmergencyCreateRequest body
    ) {
        requireUser(auth);
        return emergencyService.create(body);
    }

    @PutMapping("/{id}")
    public EmergencyDtos.EmergencyItemResponse update(
            Authentication auth,
            @PathVariable long id,
            @Valid @RequestBody EmergencyDtos.EmergencyUpdateRequest body
    ) {
        return emergencyService.update(id, requireUser(auth), body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable long id) {
        emergencyService.delete(id, requireUser(auth));
    }

    @PatchMapping("/{id}/status")
    public EmergencyDtos.EmergencyItemResponse patchStatus(
            Authentication auth,
            @PathVariable long id,
            @Valid @RequestBody EmergencyDtos.EmergencyStatusPatchRequest body
    ) {
        return emergencyService.patchStatus(id, requireUser(auth), body.status());
    }

    @PostMapping("/{id}/resolve")
    public EmergencyDtos.EmergencyItemResponse resolve(
            Authentication auth,
            @PathVariable long id,
            @Valid @RequestBody EmergencyDtos.EmergencyResolveRequest body
    ) {
        return emergencyService.resolve(id, requireUser(auth), body);
    }

    @PostMapping("/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackView(Authentication auth, @PathVariable long id) {
        requireUser(auth);
        emergencyService.trackView(id);
    }

    @PostMapping("/{id}/contact-click")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackContactClick(Authentication auth, @PathVariable long id) {
        requireUser(auth);
        emergencyService.trackContactClick(id);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
