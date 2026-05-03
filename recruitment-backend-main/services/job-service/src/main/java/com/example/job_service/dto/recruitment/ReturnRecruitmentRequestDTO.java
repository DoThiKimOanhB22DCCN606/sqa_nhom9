package com.example.job_service.dto.recruitment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRecruitmentRequestDTO {

    @NotBlank
    private String reason;

    /**
     * ID của bước workflow cần trả về (optional)
     * - Nếu null: trả về bước đầu tiên (requester/submitter)
     * - Nếu có giá trị: trả về bước cụ thể đó
     * Ví dụ: Bước 2 muốn trả về bước 1 → set returnedToStepId = stepId của bước 1
     */
    private Long returnedToStepId;
}
