package com.example.candidate_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewCandidateDTO {
    // ========== Các trường dùng cho INTERVIEW ==========
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer professionalSkillScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer communicationSkillScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workExperienceScore;

    private String strengths;
    private String weaknesses;
    /**
     * Kết luận đánh giá (true/false) - dùng cho INTERVIEW
     * true = đạt, false = không đạt
     */
    private Boolean conclusion;
}
