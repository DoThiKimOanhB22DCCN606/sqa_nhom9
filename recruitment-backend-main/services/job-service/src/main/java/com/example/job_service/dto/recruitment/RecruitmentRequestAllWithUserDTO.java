package com.example.job_service.dto.recruitment;

import java.time.LocalDateTime;

import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class RecruitmentRequestAllWithUserDTO {
    private Long id;
    private String title;
    private Integer quantity;
    private Long requesterId;
    private Long departmentId;
    private JsonNode requester;
    private JsonNode department;
    private RecruitmentRequestStatus status;
    private LocalDateTime createdAt;
    // private String jobCategoryName;

    public static RecruitmentRequestAllWithUserDTO fromEntity(RecruitmentRequest entity) {
        RecruitmentRequestAllWithUserDTO dto = new RecruitmentRequestAllWithUserDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setQuantity(entity.getQuantity());
        dto.setRequesterId(entity.getRequesterId());
        // dto.setApprovedId(entity.getApprovedId());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        // dto.setJobCategoryName(entity.getJobCategory().getName());
        return dto;
    }
}