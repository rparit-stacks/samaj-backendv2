package com.rps.samaj.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.rps.samaj.config.SamajProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends Firebase Cloud Messaging push notifications.
 *
 * Disabled by default. Enable with:
 *   samaj.firebase.enabled=true
 *   samaj.firebase.service-account-path=classpath:firebase-service-account.json
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private static final int FCM_BATCH_LIMIT = 500;

    private final SamajProperties props;
    private final ResourceLoader resourceLoader;
    private boolean ready = false;

    public FcmService(SamajProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void init() {
        if (!props.getFirebase().isEnabled()) {
            log.info("FCM disabled (samaj.firebase.enabled=false)");
            return;
        }
        String path = props.getFirebase().getServiceAccountPath();
        if (path == null || path.isBlank()) {
            log.warn("FCM: samaj.firebase.service-account-path is not set");
            return;
        }
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                ready = true;
                return;
            }
            var resource = resourceLoader.getResource(path);
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .build();
            FirebaseApp.initializeApp(options);
            ready = true;
            log.info("Firebase initialized from {}", path);
        } catch (IOException e) {
            log.error("Firebase init failed — push notifications disabled: {}", e.getMessage());
        }
    }

    /**
     * Sends a push notification to a Firebase topic (e.g. "general", "emergency").
     * All Android devices that subscribed to this topic receive it.
     * Runs asynchronously so it never blocks the calling thread.
     */
    @Async("notificationExecutor")
    public void sendToTopic(String topic, String title, String body, String link, String type) {
        if (!ready) return;
        try {
            Message msg = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", type != null ? type : "")
                    .putData("url", link != null ? link : "")
                    .build();
            String msgId = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM topic/{} delivered: {}", topic, msgId);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM topic/{} failed [{}]: {}", topic, e.getMessagingErrorCode(), e.getMessage());
        }
    }

    /**
     * Sends a push notification directly to a list of device tokens.
     * Used for individual/targeted notifications (e.g. matrimony interest received).
     * Automatically batches at 500 tokens per call (FCM limit).
     * Runs asynchronously.
     */
    @Async("notificationExecutor")
    public void sendToTokens(List<String> tokens, String title, String body, String link, String type) {
        if (!ready || tokens == null || tokens.isEmpty()) return;
        for (List<String> batch : partition(tokens, FCM_BATCH_LIMIT)) {
            try {
                MulticastMessage msg = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type != null ? type : "")
                        .putData("url", link != null ? link : "")
                        .build();
                BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(msg);
                log.info("FCM multicast: {}/{} delivered", resp.getSuccessCount(), batch.size());
            } catch (FirebaseMessagingException e) {
                log.warn("FCM multicast failed [{}]: {}", e.getMessagingErrorCode(), e.getMessage());
            }
        }
    }

    public boolean isReady() {
        return ready;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
