package com.example.workflow_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.workflow_service.service.ApprovalTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RecruitmentWorkflowEventListener {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentWorkflowEventListener.class);

    private final ObjectMapper objectMapper;
    private final ApprovalTrackingService approvalTrackingService;

    public RecruitmentWorkflowEventListener(ObjectMapper objectMapper,
            ApprovalTrackingService approvalTrackingService) {
        this.objectMapper = objectMapper;
        this.approvalTrackingService = approvalTrackingService;
    }

    @KafkaListener(topics = "${kafka.topic.recruitment-workflow:recruitment-workflow-events}", groupId = "${spring.kafka.consumer.group-id:workflow-service}")
    public void consume(String message) {
        try {
            RecruitmentWorkflowEvent event = objectMapper.readValue(message, RecruitmentWorkflowEvent.class);
            // Xử lý cả recruitment request và offer (cùng event structure)
            approvalTrackingService.handleWorkflowEvent(event);
        } catch (Exception ex) {
            log.error("Failed to process workflow event (request/offer): {}", message, ex);
        }
    }
}
