package com.rps.samaj.notification;

import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fires app-wide notifications for major user-visible actions (async fan-out to all members).
 */
@Component
public class PublicNotificationPublisher {

    private final NotificationFanoutAsync fanoutAsync;
    private final UserProfileRepository userProfileRepository;

    public PublicNotificationPublisher(NotificationFanoutAsync fanoutAsync, UserProfileRepository userProfileRepository) {
        this.fanoutAsync = fanoutAsync;
        this.userProfileRepository = userProfileRepository;
    }

    public void onCommunityPostCreated(long postId, UUID authorId, String contentPreview) {
        String name = displayName(authorId);
        String body = (name.isBlank() ? "Someone" : name) + " shared a new post.";
        if (contentPreview != null && !contentPreview.isBlank()) {
            body = body + " " + truncate(contentPreview, 140);
        }
        fanoutAsync.fanOut("New community post", body, "COMMUNITY", "/feeds", authorId);
    }

    public void onEventCreated(long eventId, UUID organizerId, String title) {
        String org = displayName(organizerId);
        String body = (org.isBlank() ? "A member" : org) + " posted an event: " + truncate(title, 120);
        fanoutAsync.fanOut("New event", body, "EVENT", "/events/" + eventId, organizerId);
    }

    public void onEmergencyCreated(long emergencyId, UUID creatorId, String title) {
        String who = displayName(creatorId);
        String body = "Emergency reported" + (title != null && !title.isBlank() ? ": " + truncate(title, 120) : "");
        body = (who.isBlank() ? "" : who + " — ") + body;
        fanoutAsync.fanOut("Emergency alert", body, "ALERT", "/emergency", creatorId);
    }

    public void onNewsArticlePublished(long articleId, String title) {
        String t = title == null ? "News update" : truncate(title, 160);
        fanoutAsync.fanOut("New article", t, "NEWS", "/news/" + articleId, null);
    }

    public void onGalleryAlbumCreated(UUID albumId, UUID creatorId, String albumName) {
        String who = displayName(creatorId);
        String body = (who.isBlank() ? "Someone" : who) + " added a gallery album"
                + (albumName != null && !albumName.isBlank() ? ": " + truncate(albumName, 100) : "");
        fanoutAsync.fanOut("New gallery album", body, "INFO", "/gallery", creatorId);
    }

    public void onDocumentApprovedPublic(UUID documentId, String title) {
        String t = title == null ? "A new document" : truncate(title, 160);
        fanoutAsync.fanOut("New document", t + " is now available in the library.", "INFO", "/documents", null);
    }

    public void adminBroadcast(String title, String body, String type, String link) {
        fanoutAsync.fanOut(title, body, type == null ? "SYSTEM" : type, link, null);
    }

    private String displayName(UUID userId) {
        return userProfileRepository.findByUser_Id(userId)
                .map(UserProfile::getFullName)
                .filter(s -> s != null && !s.isBlank())
                .orElse("");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
