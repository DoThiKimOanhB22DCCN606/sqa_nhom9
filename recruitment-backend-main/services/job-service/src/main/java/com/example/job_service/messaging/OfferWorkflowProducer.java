package com.example.job_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OfferWorkflowProducer {

    private static final Logger log = LoggerFactory.getLogger(OfferWorkflowProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public OfferWorkflowProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${kafka.topic.recruitment-workflow:recruitment-workflow-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public void publishEvent(RecruitmentWorkflowEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topicName, event.getRequestId() != null ? event.getRequestId().toString() : null,
                    payload);
            log.debug("Published offer workflow event: {} for offer {}", event.getEventType(), event.getRequestId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize offer workflow event for offer {}", event.getRequestId(), e);
            throw new RuntimeException("Cannot serialize offer workflow event", e);
        }
    }
}
