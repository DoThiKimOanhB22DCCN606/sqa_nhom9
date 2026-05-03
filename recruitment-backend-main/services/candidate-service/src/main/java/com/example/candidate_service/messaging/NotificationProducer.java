package com.example.candidate_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.notifications:notification-events}")
    private String topicName;

    public void sendNotification(Long recipientId, String title, String message, String authToken) {
        if (recipientId == null) {
            return;
        }
        NotificationEvent event = new NotificationEvent();
        event.setRecipientId(recipientId);
        event.setTitle(title);
        event.setMessage(message);
        event.setAuthToken(authToken);
        publish(event);
    }

    public void sendNotificationToDepartment(Long departmentId, Long positionId, String title, String message,
            String authToken) {
        NotificationEvent event = new NotificationEvent();
        event.setDepartmentId(departmentId);
        event.setPositionId(positionId);
        event.setTitle(title);
        event.setMessage(message);
        event.setAuthToken(authToken);
        publish(event);
    }

    public void sendNotificationToAll(String title, String message, String authToken) {
        NotificationEvent event = new NotificationEvent();
        event.setIncludeAllEmployees(true);
        event.setTitle(title);
        event.setMessage(message);
        event.setAuthToken(authToken);
        publish(event);
    }

    public void sendNotificationToMultiple(List<Long> recipientIds, String title, String message, String authToken) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        NotificationEvent event = new NotificationEvent();
        event.setRecipientIds(recipientIds);
        event.setTitle(title);
        event.setMessage(message);
        event.setAuthToken(authToken);
        publish(event);
    }

    private void publish(NotificationEvent event) {
        try {
            String key = event.getRecipientId() != null ? event.getRecipientId().toString() : "broadcast";
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topicName, key, payload);
            log.debug("Published notification event: {}", event.getTitle());
        } catch (Exception ex) {
            log.warn("Failed to publish notification event: {}", ex.getMessage());
        }
    }
}
