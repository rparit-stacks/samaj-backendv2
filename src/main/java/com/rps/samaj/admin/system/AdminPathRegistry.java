package com.rps.samaj.admin.system;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Maps {@code /admin/...} request paths to a service key. Longest registered prefix wins.
 */
public final class AdminPathRegistry {

    private record Prefix(String pathPrefix, AdminServiceKey key) {
    }

    private static final List<Prefix> PREFIXES = List.of(
            new Prefix("/admin/emergencies", AdminServiceKey.EMERGENCY),
            new Prefix("/admin/notifications", AdminServiceKey.NOTIFICATIONS),
            new Prefix("/admin/history", AdminServiceKey.HISTORY),
            new Prefix("/admin/events", AdminServiceKey.EVENTS),
            new Prefix("/admin/news", AdminServiceKey.NEWS),
            new Prefix("/admin/app-config", AdminServiceKey.APP_CONFIG),
            new Prefix("/admin/documents", AdminServiceKey.DOCUMENTS),
            new Prefix("/admin/kyc", AdminServiceKey.KYC),
            new Prefix("/admin/community", AdminServiceKey.COMMUNITY),
            new Prefix("/admin/directory", AdminServiceKey.DIRECTORY),
            new Prefix("/admin/chat", AdminServiceKey.CHAT),
            new Prefix("/admin/exam", AdminServiceKey.EXAM),
            new Prefix("/admin/matrimony", AdminServiceKey.MATRIMONY),
            new Prefix("/admin/gallery", AdminServiceKey.GALLERY),
            new Prefix("/admin/suggestions", AdminServiceKey.SUGGESTION),
            new Prefix("/admin/achievement-templates", AdminServiceKey.ACHIEVER),
            new Prefix("/admin/achievements", AdminServiceKey.ACHIEVER)
    );

    private static final Comparator<Prefix> LONGEST_FIRST =
            Comparator.<Prefix>comparingInt(p -> p.pathPrefix().length()).reversed();

    private AdminPathRegistry() {
    }

    /**
     * @param servletPath normalized path starting with "/", e.g. {@code /admin/news/articles}
     */
    public static Optional<AdminServiceKey> resolveServiceKey(String servletPath) {
        if (servletPath == null || servletPath.isEmpty()) {
            return Optional.empty();
        }
        String p = servletPath.startsWith("/") ? servletPath : "/" + servletPath;
        return PREFIXES.stream()
                .sorted(LONGEST_FIRST)
                .filter(x -> p.equals(x.pathPrefix()) || p.startsWith(x.pathPrefix() + "/"))
                .map(Prefix::key)
                .findFirst();
    }
}
