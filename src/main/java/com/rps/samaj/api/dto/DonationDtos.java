package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DonationDtos {

    private DonationDtos() {
    }

    public record DonationPublicConfigResponse(
            boolean enabled,
            long minAmountPaise,
            long maxAmountPaise,
            String keyId
    ) {
    }

    public record DonationAdminConfigResponse(
            boolean enabled,
            long minAmountPaise,
            long maxAmountPaise,
            String keyId,
            boolean configured
    ) {
    }

    public record DonationConfigUpdateRequest(
            String keyId,
            String keySecret,
            Boolean enabled,
            Long minAmountPaise,
            Long maxAmountPaise
    ) {
    }

    public record CreateOrderRequest(
            @NotNull @Positive long amountPaise,
            @Size(max = 500) String notes
    ) {
    }

    public record CreateOrderResponse(
            String orderId,
            long amountPaise,
            String currency,
            String keyId
    ) {
    }

    public record VerifyPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature,
            @Size(max = 500) String notes
    ) {
    }

    public record DonationItem(
            long id,
            String userId,
            String userName,
            long amountPaise,
            String currency,
            String status,
            String razorpayOrderId,
            String razorpayPaymentId,
            String notes,
            String createdAt
    ) {
    }

    public record DonationPageResponse(
            List<DonationItem> content,
            int totalPages,
            long totalElements,
            int size,
            int number,
            boolean first,
            boolean last
    ) {
    }

    public record DonationStatsResponse(
            long totalSuccessAmountPaise,
            long thisMonthSuccessAmountPaise,
            long totalDonors,
            long successCount,
            long failedCount,
            long pendingCount
    ) {
    }
}
