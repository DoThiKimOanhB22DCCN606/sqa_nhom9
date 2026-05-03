package com.example.workflow_service.dto.workflow;

import com.example.workflow_service.utils.enums.WorkflowType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WorkflowResponseDTO {

    private Long id;

    private String name;

    private String description;

    private WorkflowType type;

    private Long departmentId;

    private Boolean isActive;

    private Long createdBy;

    private Long updatedBy;

    private List<WorkflowStepResponseDTO> steps;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
