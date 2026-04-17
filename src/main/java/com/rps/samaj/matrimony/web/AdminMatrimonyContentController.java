package com.rps.samaj.matrimony.web;

import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.matrimony.MatrimonyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/matrimony/content")
public class AdminMatrimonyContentController {

    private final MatrimonyService matrimonyService;

    public AdminMatrimonyContentController(MatrimonyService matrimonyService) {
        this.matrimonyService = matrimonyService;
    }

    @GetMapping("/photos")
    public MatrimonyDtos.PaginatedAdminMatrimonyPhotos listPhotos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.adminListPhotos(q, flagged, page, size);
    }

    @GetMapping("/bios")
    public MatrimonyDtos.PaginatedAdminMatrimonyBios listBios(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.adminListBios(q, flagged, page, size);
    }
}
