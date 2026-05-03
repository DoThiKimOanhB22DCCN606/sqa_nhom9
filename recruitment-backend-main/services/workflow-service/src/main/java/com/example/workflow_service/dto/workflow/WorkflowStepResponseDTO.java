package com.example.workflow_service.dto.workflow;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkflowStepResponseDTO {

    private Long id;

    private Integer stepOrder;

    private Long approverPositionId;

    private String approverPositionName;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
