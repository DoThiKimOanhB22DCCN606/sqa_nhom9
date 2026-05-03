package com.example.workflow_service.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateApprovalTrackingDTO {

    @NotNull(message = "Request ID không được để trống")
    private Long requestId;

    @NotNull(message = "Department ID không được để trống")
    private Long departmentId;

    @NotNull(message = "Level ID không được để trống")
    private Long levelId;
}
