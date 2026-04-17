package com.rps.samaj.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationBatchWriter {

    private final AppNotificationRepository appNotificationRepository;

    public NotificationBatchWriter(AppNotificationRepository appNotificationRepository) {
        this.appNotificationRepository = appNotificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAllInNewTx(List<AppNotification> batch) {
        appNotificationRepository.saveAll(batch);
    }
}
