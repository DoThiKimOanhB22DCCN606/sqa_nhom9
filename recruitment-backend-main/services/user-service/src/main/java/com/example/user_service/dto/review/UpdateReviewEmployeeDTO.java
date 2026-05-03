package com.example.user_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewEmployeeDTO {
    // ========== Các trường dùng cho PROBATION ==========
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer onTimeCompletionScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workEfficiencyScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer professionalSkillScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer selfLearningScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workAttitudeScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer communicationSkillScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer honestyResponsibilityScore;

    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer teamIntegrationScore;

    private Boolean probationResult;
    private String additionalComments;
}
