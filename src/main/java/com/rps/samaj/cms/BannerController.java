package com.rps.samaj.cms;

import com.rps.samaj.api.dto.AppConfigDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final CmsBannerService bannerService;

    public BannerController(CmsBannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/active")
    public List<AppConfigDtos.CmsMobileBannerResponse> listActiveBanners() {
        return bannerService.listActive();
    }
}
