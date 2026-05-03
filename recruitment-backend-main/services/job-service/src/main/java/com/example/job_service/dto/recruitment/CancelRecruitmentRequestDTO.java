package com.example.job_service.dto.recruitment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelRecruitmentRequestDTO {

    @NotBlank
    private String reason;
}
