package com.rps.samaj.user.model;

/**
 * Denormalized KYC state on the user for quick checks; aligned with latest {@link com.rps.samaj.kyc.KycSubmission}.
 */
public enum KycStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}
