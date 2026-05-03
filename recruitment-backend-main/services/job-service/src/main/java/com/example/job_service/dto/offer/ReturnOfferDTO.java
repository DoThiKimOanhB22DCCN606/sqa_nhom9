package com.example.job_service.dto.offer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnOfferDTO {
    @NotBlank(message = "Lý do trả về không được để trống")
    private String reason;

    /**
     * ID của bước workflow cần trả về (optional)
     * - Nếu null: trả về bước đầu tiên (requester/submitter)
     * - Nếu có giá trị: trả về bước cụ thể đó
     */
    private Long returnedToStepId;
}
