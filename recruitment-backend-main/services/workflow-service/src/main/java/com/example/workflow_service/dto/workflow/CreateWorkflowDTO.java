package com.example.workflow_service.dto.workflow;

import com.example.workflow_service.utils.enums.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateWorkflowDTO {

    @NotBlank(message = "Tên workflow không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại workflow không được để trống")
    private WorkflowType type;

    // Department ID để áp dụng workflow
    private Long departmentId;

    private Long createdBy;

    private List<CreateStepDTO> steps;
}
