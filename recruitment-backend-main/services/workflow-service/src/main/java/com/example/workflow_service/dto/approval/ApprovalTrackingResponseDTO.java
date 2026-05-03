package com.example.workflow_service.dto.approval;

import com.example.workflow_service.utils.enums.ApprovalStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApprovalTrackingResponseDTO {

    private Long id;

    private Long requestId;

    private Long stepId;

    private ApprovalStatus status;

    private Long approverPositionId;
    private String approverPositionName;
    private Long actionUserId;

    private String actionUserName;

    private LocalDateTime actionAt;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
