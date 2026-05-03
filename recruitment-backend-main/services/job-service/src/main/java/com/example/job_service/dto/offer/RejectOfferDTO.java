package com.example.job_service.dto.offer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectOfferDTO {
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
