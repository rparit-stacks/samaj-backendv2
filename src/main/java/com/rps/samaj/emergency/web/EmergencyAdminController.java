package com.rps.samaj.emergency.web;

import com.rps.samaj.api.dto.EmergencyDtos;
import com.rps.samaj.emergency.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/emergencies")
public class EmergencyAdminController {

    private final EmergencyService emergencyService;

    public EmergencyAdminController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    public List<EmergencyDtos.EmergencyItemResponse> listAll() {
        return emergencyService.listForAdmin();
    }

    @PatchMapping("/{id}/status")
    public EmergencyDtos.EmergencyItemResponse patchStatus(
            @PathVariable long id,
            @Valid @RequestBody EmergencyDtos.EmergencyStatusPatchRequest body
    ) {
        return emergencyService.adminPatchStatus(id, body.status());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        emergencyService.adminDelete(id);
    }
}
