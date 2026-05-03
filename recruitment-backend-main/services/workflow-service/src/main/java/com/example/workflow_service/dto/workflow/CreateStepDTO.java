package com.example.workflow_service.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStepDTO {

    @NotNull(message = "Thứ tự bước không được để trống")
    private Integer stepOrder;

    @NotNull(message = "Vị trí phê duyệt không được để trống")
    private Long approverPositionId;
}
