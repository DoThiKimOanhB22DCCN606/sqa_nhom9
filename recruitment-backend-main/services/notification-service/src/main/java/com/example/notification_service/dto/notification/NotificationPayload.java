package com.example.notification_service.dto.notification;

import java.time.format.DateTimeFormatter;

import com.example.notification_service.model.Notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPayload {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Long id;
    private final Long recipientId;
    private final String title;
    private final String message;
    private final boolean read;
    private final boolean delivered;
    private final String deliveryStatus;
    private final String sentAt; // Changed to String for Socket.IO compatibility
    private final String readAt; // Changed to String for Socket.IO compatibility
    private final String eventType;

    public static NotificationPayload from(Notification notification, String eventType) {
        return NotificationPayload.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .delivered(notification.isDelivered())
                .deliveryStatus(notification.getDeliveryStatus())
                .sentAt(notification.getSentAt() != null ? notification.getSentAt().format(FORMATTER) : null)
                .readAt(notification.getReadAt() != null ? notification.getReadAt().format(FORMATTER) : null)
                .eventType(eventType)
                .build();
    }
}
