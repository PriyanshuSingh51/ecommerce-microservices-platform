package com.ecommerce.notification.service;

import com.ecommerce.notification.model.Notification;
import com.ecommerce.notification.model.NotificationChannel;
import com.ecommerce.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Persists and "sends" a notification. In production this delegates to
     * a real provider (SES/SendGrid for email, Twilio for SMS, FCM/APNs for
     * push) behind a channel-specific adapter; here we log and mark it sent.
     */
    public Notification send(String recipientId, NotificationChannel channel, String subject, String body, String sourceEvent) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setChannel(channel);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setSourceEvent(sourceEvent);
        notification.setSent(true);
        notification.setSentAt(Instant.now());

        Notification saved = notificationRepository.save(notification);
        log.info("Sent {} notification to {} (event: {}): {}", channel, recipientId, sourceEvent, subject);
        return saved;
    }
}
