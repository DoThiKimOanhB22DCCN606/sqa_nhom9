package com.example.job_service.dto.jobposition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.job_service.model.JobPosition;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.utils.TextTruncateUtil;
import com.example.job_service.utils.enums.JobPositionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPositionResponseDTO {
    private Long id;
    private String title;
    private String description;
    // private String responsibilities;
    private String requirements;
    // private String qualifications;
    private String benefits;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String employmentType;
    private String experienceLevel;
    private String location;
    private boolean isRemote;
    private int quantity;
    private LocalDate deadline;
    private Long departmentId;
    private Integer applicationCount;
    private JobPositionStatus status;
    private RecruitmentRequest recruitmentRequest;
    private String departmentName; // Tên phòng ban
    private String yearsOfExperience;
    private LocalDateTime publishedAt;

    public static JobPositionResponseDTO fromEntity(JobPosition position) {
        return fromEntity(position, false);
    }

    public static JobPositionResponseDTO fromEntity(JobPosition position, boolean truncateText) {
        JobPositionResponseDTO dto = new JobPositionResponseDTO();
        dto.setId(position.getId());
        dto.setTitle(truncateText ? TextTruncateUtil.truncateTitle(position.getTitle()) : position.getTitle());
        dto.setDescription(truncateText ? TextTruncateUtil.truncateDescription(position.getDescription())
                : position.getDescription());
        // dto.setResponsibilities(position.getResponsibilities());
        dto.setRequirements(truncateText ? TextTruncateUtil.truncateRequirements(position.getRequirements())
                : position.getRequirements());
        // dto.setQualifications(position.getQualifications());
        dto.setBenefits(
                truncateText ? TextTruncateUtil.truncateBenefits(position.getBenefits()) : position.getBenefits());
        dto.setSalaryMin(position.getSalaryMin());
        dto.setSalaryMax(position.getSalaryMax());
        dto.setEmploymentType(position.getEmploymentType());
        dto.setExperienceLevel(position.getExperienceLevel());
        dto.setLocation(position.getLocation());
        dto.setRemote(position.isRemote());
        dto.setDeadline(position.getDeadline());
        dto.setQuantity(position.getQuantity());
        // applicationCount sẽ được set từ service (gọi candidate-service)
        dto.setApplicationCount(0);
        dto.setStatus(position.getStatus());

        dto.setRecruitmentRequest(position.getRecruitmentRequest());
        dto.setDepartmentId(position.getRecruitmentRequest().getDepartmentId());
        // Set additional fields for UI display
        dto.setYearsOfExperience(position.getYearsOfExperience());
        dto.setPublishedAt(position.getPublishedAt());
        return dto;
    }

}
