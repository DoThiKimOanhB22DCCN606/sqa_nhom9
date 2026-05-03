package com.example.job_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.job_service.model.Offer;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.OfferRepository;
import com.example.job_service.repository.RecruitmentRequestRepository;
import com.example.job_service.utils.enums.OfferStatus;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WorkflowCompletionListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCompletionListener.class);

    private final ObjectMapper objectMapper;
    private final RecruitmentRequestRepository recruitmentRequestRepository;
    private final OfferRepository offerRepository;

    public WorkflowCompletionListener(ObjectMapper objectMapper,
            RecruitmentRequestRepository recruitmentRequestRepository,
            OfferRepository offerRepository) {
        this.objectMapper = objectMapper;
        this.recruitmentRequestRepository = recruitmentRequestRepository;
        this.offerRepository = offerRepository;
    }

    @KafkaListener(topics = "${kafka.topic.recruitment-workflow:recruitment-workflow-events}", groupId = "${spring.kafka.consumer.group-id:job-service}")
    @Transactional
    public void handleWorkflowEvent(String message) {
        try {
            RecruitmentWorkflowEvent event = objectMapper.readValue(message, RecruitmentWorkflowEvent.class);

            // Chỉ xử lý event WORKFLOW_COMPLETED
            if ("WORKFLOW_COMPLETED".equals(event.getEventType())) {
                log.info("Received WORKFLOW_COMPLETED event for request {}", event.getRequestId());

                String requestType = event.getRequestType();

                if ("RECRUITMENT_REQUEST".equalsIgnoreCase(requestType)) {
                    // Xử lý RecruitmentRequest
                    RecruitmentRequest request = recruitmentRequestRepository.findById(event.getRequestId())
                            .orElse(null);

                    if (request != null && (request.getStatus() == RecruitmentRequestStatus.SUBMITTED
                            || request.getStatus() == RecruitmentRequestStatus.PENDING)) {
                        request.setStatus(RecruitmentRequestStatus.APPROVED);
                        recruitmentRequestRepository.save(request);
                        log.info("Request {} đã được chuyển sang APPROVED sau khi hoàn thành workflow",
                                event.getRequestId());
                    }
                } else if ("OFFER".equalsIgnoreCase(requestType)) {
                    // Xử lý Offer
                    Offer offer = offerRepository.findById(event.getRequestId()).orElse(null);
                    if (offer != null && offer.getStatus() == OfferStatus.PENDING) {
                        offer.setStatus(OfferStatus.APPROVED);
                        offerRepository.save(offer);
                        log.info("Offer {} đã được chuyển sang APPROVED sau khi hoàn thành workflow",
                                event.getRequestId());
                    }
                } else {
                    // Trường hợp cũ chưa có requestType: fallback logic cũ
                    RecruitmentRequest request = recruitmentRequestRepository.findById(event.getRequestId())
                            .orElse(null);

                    if (request != null && (request.getStatus() == RecruitmentRequestStatus.SUBMITTED
                            || request.getStatus() == RecruitmentRequestStatus.PENDING)) {
                        request.setStatus(RecruitmentRequestStatus.APPROVED);
                        recruitmentRequestRepository.save(request);
                        log.info("Request {} đã được chuyển sang APPROVED sau khi hoàn thành workflow (fallback)",
                                event.getRequestId());
                    } else {
                        Offer offer = offerRepository.findById(event.getRequestId()).orElse(null);
                        if (offer != null && offer.getStatus() == OfferStatus.PENDING) {
                            offer.setStatus(OfferStatus.APPROVED);
                            offerRepository.save(offer);
                            log.info("Offer {} đã được chuyển sang APPROVED sau khi hoàn thành workflow (fallback)",
                                    event.getRequestId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing workflow completion event: {}", message, e);
        }
    }
}
