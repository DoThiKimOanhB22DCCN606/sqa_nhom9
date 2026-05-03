package com.example.candidate_service.dto.review;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCandidateResponseDTO {
    private Long id;
    private Long candidateId;
    private Long reviewerId;
    private String reviewerName;

    // ========== Các trường dùng cho INTERVIEW ==========
    private Integer professionalSkillScore;
    private Integer communicationSkillScore;
    private Integer workExperienceScore;

    /**
     * Điểm trung bình (tính từ 3 tiêu chí INTERVIEW)
     */
    private Double averageScore;

    private String strengths;
    private String weaknesses;
    /**
     * Kết luận đánh giá (true/false) - dùng cho INTERVIEW
     * true = đạt, false = không đạt
     */
    private Boolean conclusion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

