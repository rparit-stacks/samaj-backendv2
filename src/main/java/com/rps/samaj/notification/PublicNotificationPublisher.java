package com.rps.samaj.notification;

import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fires app-wide notifications for major user-visible actions.
 * Each method:
 *   1. Saves an in-app notification for every active member (async fan-out via DB).
 *   2. Sends an FCM push to the matching topic so Android devices are notified instantly.
 */
@Component
public class PublicNotificationPublisher {

    private final NotificationFanoutAsync fanoutAsync;
    private final UserProfileRepository userProfileRepository;
    private final FcmService fcmService;

    public PublicNotificationPublisher(
            NotificationFanoutAsync fanoutAsync,
            UserProfileRepository userProfileRepository,
            FcmService fcmService
    ) {
        this.fanoutAsync = fanoutAsync;
        this.userProfileRepository = userProfileRepository;
        this.fcmService = fcmService;
    }

    public void onCommunityPostCreated(long postId, UUID authorId, String contentPreview) {
        String name = displayName(authorId);
        String body = (name.isBlank() ? "Someone" : name) + " shared a new post.";
        if (contentPreview != null && !contentPreview.isBlank()) {
            body = body + " " + truncate(contentPreview, 140);
        }
        fanoutAsync.fanOut("New community post", body, "COMMUNITY", "/feeds", authorId);
        fcmService.sendToTopic("general", "New community post", body, "/feeds", "COMMUNITY");
    }

    public void onEventCreated(long eventId, UUID organizerId, String title) {
        String org = displayName(organizerId);
        String body = (org.isBlank() ? "A member" : org) + " posted an event: " + truncate(title, 120);
        fanoutAsync.fanOut("New event", body, "EVENT", "/events/" + eventId, organizerId);
        fcmService.sendToTopic("general", "New event", body, "/events/" + eventId, "EVENT");
    }

    public void onEmergencyCreated(long emergencyId, UUID creatorId, String title) {
        String who = displayName(creatorId);
        String body = "Emergency reported" + (title != null && !title.isBlank() ? ": " + truncate(title, 120) : "");
        body = (who.isBlank() ? "" : who + " — ") + body;
        fanoutAsync.fanOut("Emergency alert", body, "ALERT", "/emergency", creatorId);
        // Emergency goes to its own high-priority topic
        fcmService.sendToTopic("emergency", "🚨 Emergency alert", body, "/emergency", "ALERT");
    }

    public void onNewsArticlePublished(long articleId, String title) {
        String t = title == null ? "News update" : truncate(title, 160);
        fanoutAsync.fanOut("New article", t, "NEWS", "/news/" + articleId, null);
        fcmService.sendToTopic("general", "New article", t, "/news/" + articleId, "NEWS");
    }

    public void onGalleryAlbumCreated(UUID albumId, UUID creatorId, String albumName) {
        String who = displayName(creatorId);
        String body = (who.isBlank() ? "Someone" : who) + " added a gallery album"
                + (albumName != null && !albumName.isBlank() ? ": " + truncate(albumName, 100) : "");
        fanoutAsync.fanOut("New gallery album", body, "INFO", "/gallery", creatorId);
        fcmService.sendToTopic("general", "New gallery album", body, "/gallery", "INFO");
    }

    public void onDocumentApprovedPublic(UUID documentId, String title) {
        String t = title == null ? "A new document" : truncate(title, 160);
        fanoutAsync.fanOut("New document", t + " is now available in the library.", "INFO", "/documents", null);
        fcmService.sendToTopic("general", "New document", t + " is now available.", "/documents", "INFO");
    }

    public void adminBroadcast(String title, String body, String type, String link) {
        fanoutAsync.fanOut(title, body, type == null ? "SYSTEM" : type, link, null);
        fcmService.sendToTopic("general", title, body, link, type == null ? "SYSTEM" : type);
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
