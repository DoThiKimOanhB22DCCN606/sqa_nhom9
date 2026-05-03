package com.example.job_service.dto.jobposition;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobPositionDTO {
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String employmentType;
    private String experienceLevel;
    private String location;
    private Boolean isRemote;
    private Integer quantity;
    private LocalDate deadline;
    private String yearsOfExperience;
}
