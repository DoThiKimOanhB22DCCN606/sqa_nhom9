package com.example.job_service.dto.offer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.job_service.utils.enums.OfferStatus;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class OfferDetailDTO {
    // Thông tin offer cơ bản
    private Long id;
    private Long candidateId;
    private Long basicSalary;
    private Integer probationSalaryRate;
    private LocalDate onboardingDate;
    private Integer probationPeriod;
    private String notes;
    private OfferStatus status;
    private Long requesterId;
    private String requesterName; // Tên người tạo
    private Long ownerUserId;
    private Long workflowId;
    private JsonNode workflowInfo; // Thông tin workflow và approval tracking
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin candidate (chỉ thông tin cần thiết)
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;

    // Thông tin bổ sung
    private String jobPositionTitle; // Vị trí ứng tuyển
    private String departmentName; // Tên phòng ban
    private String levelName; // Tên cấp bậc
}
