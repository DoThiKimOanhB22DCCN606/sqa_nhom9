package com.example.job_service.dto.jobposition;

import java.math.BigDecimal;

import com.example.job_service.model.JobPosition;
import com.example.job_service.utils.enums.JobPositionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAllJobPositionDTO {
    private Long id;
    private String title;
    private String experienceLevel;
    private String yearsOfExperience;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Long recruitmentRequestId;
    private String departmentName;
    private Integer applicationCount;
    private JobPositionStatus status;

    public static GetAllJobPositionDTO fromEntity(JobPosition position) {
        GetAllJobPositionDTO dto = new GetAllJobPositionDTO();
        dto.setId(position.getId());
        dto.setTitle(position.getTitle());
        dto.setSalaryMin(position.getSalaryMin());
        dto.setSalaryMax(position.getSalaryMax());
        dto.setExperienceLevel(position.getExperienceLevel());
        dto.setYearsOfExperience(position.getYearsOfExperience());
        dto.setStatus(position.getStatus());
        // applicationCount sẽ được set từ service (gọi candidate-service)
        dto.setApplicationCount(0);
        if (position.getRecruitmentRequest() != null) {
            dto.setRecruitmentRequestId(position.getRecruitmentRequest().getId());
        }
        return dto;
    }
}
