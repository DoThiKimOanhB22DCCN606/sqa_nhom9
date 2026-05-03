package com.example.job_service.dto.offer;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateOfferDTO {

    @NotNull(message = "ID ứng viên không được để trống")
    private Long candidateId;

    /**
     * Lương cơ bản (VNĐ)
     */
    @NotNull(message = "Lương cơ bản không được để trống")
    private Long basicSalary;

    /**
     * Tỷ lệ lương thử việc (%)
     * Ví dụ: 85 = 85% lương chính thức
     */
    private Integer probationSalaryRate;

    /**
     * Ngày onboarding
     */
    @NotNull(message = "Ngày onboarding không được để trống")
    private LocalDate onboardingDate;

    /**
     * Thời gian thử việc (tháng)
     */
    @NotNull(message = "Thời gian thử việc không được để trống")
    private Integer probationPeriod;

    /**
     * Ghi chú về offer
     */
    private String notes;

    /**
     * Người tạo có thể chọn workflow cụ thể hoặc để hệ thống tự chọn.
     */
    private Long workflowId;
}
