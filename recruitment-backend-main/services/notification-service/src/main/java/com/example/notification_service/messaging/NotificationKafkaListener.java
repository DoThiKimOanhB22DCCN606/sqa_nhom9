package com.example.notification_service.messaging;

import com.example.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topic.notifications:notification-events}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consume(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            System.out.println("Received notification event: " + event);
            notificationService.processNotificationEvent(event);
        } catch (Exception ex) {
            log.error("Failed to process notification event: {}", message, ex);
        }
    }
}
