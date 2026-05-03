package com.example.job_service.dto.recruitment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectRecruitmentRequestDTO {

    @NotBlank
    private String reason; // Lý do từ chối
}
