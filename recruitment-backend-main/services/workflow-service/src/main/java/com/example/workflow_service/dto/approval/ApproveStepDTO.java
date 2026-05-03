package com.example.workflow_service.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveStepDTO {

    @NotNull(message = "Quyết định phê duyệt không được để trống")
    private Boolean approved; // true = approve, false = reject

    private String approvalNotes;
}
