package com.example.workflow_service.dto.approval;

import com.example.workflow_service.dto.workflow.WorkflowResponseDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RequestWorkflowInfoDTO {
    private WorkflowResponseDTO workflow;
    private List<ApprovalTrackingResponseDTO> approvalTrackings;
    private Long currentStepId; // Bước hiện tại đang xử lý
}
