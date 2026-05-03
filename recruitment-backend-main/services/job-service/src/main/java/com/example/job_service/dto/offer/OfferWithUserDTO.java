package com.example.job_service.dto.offer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.job_service.model.Offer;
import com.example.job_service.utils.enums.OfferStatus;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class OfferWithUserDTO {
    private Long id;
    private Long candidateId;
    private JsonNode candidate; // Thông tin ứng viên từ candidate-service
    private Long basicSalary;
    private Integer probationSalaryRate;
    private LocalDate onboardingDate;
    private Integer probationPeriod;
    private String notes;
    private OfferStatus status;
    private Long requesterId;
    private JsonNode requester;
    private Long ownerUserId;
    private Long workflowId;
    private JsonNode workflowInfo; // Thông tin workflow và approval tracking
    private LocalDateTime submittedAt;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin bổ sung từ candidate và job position
    private String jobPositionTitle; // Vị trí ứng tuyển (từ JobPosition.title)
    private String departmentName; // Tên phòng ban (từ Department.name)
    private String levelName; // Tên cấp bậc (từ Level.name hoặc Position.level)

    public static OfferWithUserDTO fromEntity(Offer entity) {
        OfferWithUserDTO dto = new OfferWithUserDTO();
        dto.setId(entity.getId());
        dto.setCandidateId(entity.getCandidateId());
        dto.setBasicSalary(entity.getBasicSalary());
        dto.setProbationSalaryRate(entity.getProbationSalaryRate());
        dto.setOnboardingDate(entity.getOnboardingDate());
        dto.setProbationPeriod(entity.getProbationPeriod());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus());
        dto.setRequesterId(entity.getRequesterId());
        dto.setOwnerUserId(entity.getOwnerUserId());
        dto.setWorkflowId(entity.getWorkflowId());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
