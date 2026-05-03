package com.example.job_service.messaging;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RecruitmentWorkflowEvent {

    private String eventType; // REQUEST_SUBMITTED, REQUEST_APPROVED, REQUEST_REJECTED, REQUEST_RETURNED,
                              // REQUEST_CANCELLED, STEP_APPROVED, STEP_REJECTED
    // Loại yêu cầu: RECRUITMENT_REQUEST hoặc OFFER (giúp phân biệt ở consumer)
    private String requestType;
    private Long requestId;
    private Long workflowId;
    private Long currentStepId; // Bước workflow hiện tại đang xử lý
    private Long actorUserId;
    private String notes;
    private String reason;
    private String requestStatus;
    private Long ownerUserId;
    private Long requesterId;
    // Đối với offer, departmentId có thể không có nên cần candidateId để suy ra
    private Long candidateId;
    private Long departmentId;
    private LocalDateTime occurredAt;
    private String authToken;

    // Cho trường hợp RETURN
    private Long returnedToStepId; // Bước cần trả về (null = trả về bước đầu tiên)

    // Cho trường hợp APPROVE/REJECT ở bước cụ thể
    private Long stepId; // ID của bước đang approve/reject
    private Boolean approved; // true = approve, false = reject
}
