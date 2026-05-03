package com.example.job_service.dto.recruitment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.utils.TextTruncateUtil;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class RecruitmentRequestWithUserDTO {
    private Long id;
    private String title;
    private Integer quantity;
    private String reason;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private RecruitmentRequestStatus status;
    private Long requesterId;
    private JsonNode requester;
    // private Long approvedId;
    // private JsonNode approver;
    // private String approvalNotes;
    // private LocalDateTime approvedAt;
    private Long departmentId;
    private JsonNode department;
    // private String jobCategoryName;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long ownerUserId;
    private LocalDateTime submittedAt;
    // Thông tin workflow
    private Long workflowId;
    private JsonNode workflowInfo; // Thông tin workflow và approval tracking

    public static RecruitmentRequestWithUserDTO fromEntity(RecruitmentRequest entity) {
        return fromEntity(entity, false);
    }

    public static RecruitmentRequestWithUserDTO fromEntity(RecruitmentRequest entity, boolean truncateText) {
        RecruitmentRequestWithUserDTO dto = new RecruitmentRequestWithUserDTO();
        dto.setId(entity.getId());
        dto.setTitle(truncateText ? TextTruncateUtil.truncateTitle(entity.getTitle()) : entity.getTitle());
        dto.setQuantity(entity.getQuantity());
        dto.setReason(truncateText ? TextTruncateUtil.truncateReason(entity.getReason()) : entity.getReason());
        // Chỉ set salary khi vượt quỹ
        if (entity.getSalaryMin() != null && entity.getSalaryMax() != null) {
            dto.setSalaryMin(entity.getSalaryMin());
            dto.setSalaryMax(entity.getSalaryMax());
        }

        dto.setStatus(entity.getStatus());
        dto.setRequesterId(entity.getRequesterId());
        // dto.setApprovedId(entity.getApprovedId());
        // dto.setApprovalNotes(entity.getApprovalNotes());
        // dto.setApprovedAt(entity.getApprovedAt());
        dto.setDepartmentId(entity.getDepartmentId());
        // dto.setJobCategoryName(entity.getJobCategory() != null ?
        // entity.getJobCategory().getName() : null);
        dto.setActive(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
