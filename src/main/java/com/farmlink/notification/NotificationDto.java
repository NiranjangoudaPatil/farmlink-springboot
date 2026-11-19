package com.farmlink.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(UUID id, String title, String message, String type, UUID referenceId,
                              boolean read, Instant createdAt) {

    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.getReferenceId(),
                n.isRead(), n.getCreatedAt());
    }
}
