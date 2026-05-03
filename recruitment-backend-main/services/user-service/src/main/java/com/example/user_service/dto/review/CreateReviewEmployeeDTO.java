package com.example.user_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewEmployeeDTO {
    /**
     * Employee ID (bắt buộc cho PROBATION)
     */
    @NotNull(message = "Employee ID là bắt buộc")
    private Long employeeId;

    // ========== Các trường dùng cho PROBATION - Đánh giá năng lực chuyên môn
    // ==========
    /**
     * Điểm hoàn thành công việc đúng tiến độ (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer onTimeCompletionScore;

    /**
     * Điểm hiệu quả trong công việc (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workEfficiencyScore;

    /**
     * Điểm kỹ năng chuyên môn liên quan (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer professionalSkillScore;

    /**
     * Điểm tự học hỏi, phát triển bản thân (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer selfLearningScore;

    // ========== Các trường dùng cho PROBATION - Đánh giá tính cách và tác phong
    // ==========
    /**
     * Điểm thái độ làm việc (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer workAttitudeScore;

    /**
     * Điểm khả năng giao tiếp (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer communicationSkillScore;

    /**
     * Điểm tính trung thực và trách nhiệm (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer honestyResponsibilityScore;

    /**
     * Điểm khả năng hòa nhập với tập thể (1-5) - PROBATION
     */
    @Min(value = 1, message = "Điểm phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm phải từ 1 đến 5")
    private Integer teamIntegrationScore;

    // ========== Kết quả thử việc ==========
    /**
     * Kết quả đánh giá: true = Đạt yêu cầu, false = Chưa đạt yêu cầu
     */
    private Boolean probationResult;

    /**
     * Nhận xét bổ sung - PROBATION
     */
    private String additionalComments;
}
