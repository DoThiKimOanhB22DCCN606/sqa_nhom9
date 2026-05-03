package com.example.job_service.dto.offer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateOfferDTO {
    private Long candidateId;
    private Long basicSalary;
    private Integer probationSalaryRate;
    private LocalDate onboardingDate;
    private Integer probationPeriod;
    private String notes;
}
