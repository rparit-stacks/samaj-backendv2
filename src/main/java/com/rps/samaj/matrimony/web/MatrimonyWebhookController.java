package com.rps.samaj.matrimony.web;

import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.config.SamajProperties;
import com.rps.samaj.matrimony.MatrimonyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * External providers can POST inbound chat events here.
 * Set {@code samaj.matrimony.webhook-secret} and send the same value in {@code X-Matrimony-Webhook-Secret}.
 */
@RestController
@RequestMapping("/api/v1/matrimony/webhooks")
public class MatrimonyWebhookController {

    public static final String HEADER_SECRET = "X-Matrimony-Webhook-Secret";

    private final MatrimonyService matrimonyService;
    private final SamajProperties properties;

    public MatrimonyWebhookController(MatrimonyService matrimonyService, SamajProperties properties) {
        this.matrimonyService = matrimonyService;
        this.properties = properties;
    }

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrimonyDtos.MatrimonyChatMessageResponse chatInbound(
            @RequestHeader(value = HEADER_SECRET, required = false) String secret,
            @Valid @RequestBody MatrimonyDtos.MatrimonyChatWebhookRequest body
    ) {
        String configured = properties.getMatrimony().getWebhookSecret();
        return matrimonyService.ingestWebhookMessage(configured, secret, body);
    }
}
