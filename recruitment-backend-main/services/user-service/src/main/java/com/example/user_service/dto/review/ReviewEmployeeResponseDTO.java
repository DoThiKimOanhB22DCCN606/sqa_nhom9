package com.example.user_service.dto.review;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewEmployeeResponseDTO {
    private Long id;
    private Long employeeId;
    private Long reviewerId;
    private String reviewerName;

    // ========== Các trường dùng cho PROBATION ==========
    private Integer onTimeCompletionScore;
    private Integer workEfficiencyScore;
    private Integer professionalSkillScore;
    private Integer selfLearningScore;
    private Integer workAttitudeScore;
    private Integer communicationSkillScore;
    private Integer honestyResponsibilityScore;
    private Integer teamIntegrationScore;

    /**
     * Điểm trung bình (tính từ 8 tiêu chí PROBATION)
     */
    private Double averageScore;

    private String additionalComments; // Nhận xét bổ sung
    /**
     * Kết quả đánh giá: true = Đạt yêu cầu, false = Chưa đạt yêu cầu
     */
    private Boolean probationResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
