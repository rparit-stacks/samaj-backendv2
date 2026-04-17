package com.rps.samaj.config.app;

import com.rps.samaj.api.dto.AppConfigDtos;
import com.rps.samaj.security.DevUserContextFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/admin/app-config")
public class AdminAppConfigController {

    private final RuntimeConfigService runtimeConfig;
    private final AppConfigEntryRepository repo;

    public AdminAppConfigController(RuntimeConfigService runtimeConfig, AppConfigEntryRepository repo) {
        this.runtimeConfig = runtimeConfig;
        this.repo = repo;
    }

    @GetMapping
    public AppConfigDtos.AppConfigMapResponse getAll(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        return new AppConfigDtos.AppConfigMapResponse(runtimeConfig.listAll());
    }

    @PutMapping
    public AppConfigDtos.AppConfigMapResponse patch(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody AppConfigDtos.AppConfigPatchRequest body
    ) {
        requireAdmin(adminId);
        if (body.entries() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entries required");
        }
        return new AppConfigDtos.AppConfigMapResponse(runtimeConfig.replaceAll(body.entries(), adminId));
    }

    @GetMapping("/storage/effective")
    public AppConfigDtos.EffectiveStorageResponse effectiveStorage(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        var s3 = runtimeConfig.resolvedS3();
        return new AppConfigDtos.EffectiveStorageResponse(
                runtimeConfig.effectiveStorageProvider(),
                s3.bucket(),
                s3.region(),
                s3.publicBaseUrl(),
                s3.usable()
        );
    }

    private static void requireAdmin(UUID adminId) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required");
        }
    }
}
