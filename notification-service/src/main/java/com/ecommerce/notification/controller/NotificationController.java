package com.ecommerce.notification.controller;

import com.ecommerce.notification.model.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Email, SMS and push notification history")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "List notifications, optionally filtered by recipient")
    public ResponseEntity<List<Notification>> list(@RequestParam(required = false) String recipientId) {
        if (recipientId != null) return ResponseEntity.ok(notificationRepository.findByRecipientId(recipientId));
        return ResponseEntity.ok(notificationRepository.findAll());
    }
}
