package com.rps.samaj.admin.system;

/**
 * Granular admin areas. Assign to {@link com.rps.samaj.user.model.UserRole#MODERATOR} child admins only;
 * {@link com.rps.samaj.user.model.UserRole#ADMIN} and {@link com.rps.samaj.user.model.User#isParentAdmin()}
 * users bypass these checks for service routes.
 */
public enum AdminServiceKey {
    COMMUNITY,
    DIRECTORY,
    EMERGENCY,
    DOCUMENTS,
    CHAT,
    NEWS,
    EVENTS,
    KYC,
    NOTIFICATIONS,
    HISTORY,
    APP_CONFIG,
    EXAM,
    MATRIMONY,
    GALLERY,
    SUGGESTION,
    ACHIEVER
}
