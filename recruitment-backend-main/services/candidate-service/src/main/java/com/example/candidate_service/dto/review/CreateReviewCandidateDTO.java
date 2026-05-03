package com.example.candidate_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewCandidateDTO {
    /**
     * Candidate ID (bắt buộc cho INTERVIEW)
     */
    @NotNull(message = "Candidate ID là bắt buộc")
    private Long candidateId;

    // ========== Các trường dùng cho INTERVIEW ==========
    /**
     * Điểm kỹ năng chuyên môn (1-5) - INTERVIEW
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer professionalSkillScore;

    /**
     * Điểm kỹ năng giao tiếp (1-5) - INTERVIEW
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer communicationSkillScore;

    /**
     * Điểm kinh nghiệm làm việc (1-5) - INTERVIEW
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workExperienceScore;

    /**
     * Điểm mạnh của ứng viên
     */
    private String strengths;

    /**
     * Điểm yếu của ứng viên
     */
    private String weaknesses;

    /**
     * Kết luận đánh giá (true/false) - INTERVIEW
     * true = đạt, false = không đạt
     */
    private Boolean conclusion;
}
