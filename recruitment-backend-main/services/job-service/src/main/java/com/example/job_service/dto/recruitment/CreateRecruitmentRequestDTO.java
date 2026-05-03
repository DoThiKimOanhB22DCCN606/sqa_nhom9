package com.example.job_service.dto.recruitment;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecruitmentRequestDTO {
    @NotNull
    private String title;
    @NotNull
    private Integer quantity;
    private String reason;
    private BigDecimal salaryMin; // Chỉ có giá trị khi exceedBudget = true
    private BigDecimal salaryMax; // Chỉ có giá trị khi exceedBudget = true
    private boolean exceedBudget;

    // @NotNull
    // private Long jobCategoryId;
    private Long requesterId; // Employee Id
    @NotNull
    private Long departmentId;

    /**
     * Người tạo có thể chọn workflow cụ thể hoặc để hệ thống tự chọn.
     */
    private Long workflowId;

    /**
     * Validation: Nếu vượt quỹ thì phải có salary
     */
    @AssertTrue(message = "Khi vượt quỹ, phải cung cấp thông tin lương")
    public boolean isValidSalaryWhenExceedBudget() {
        if (exceedBudget) {
            return salaryMin != null && salaryMax != null;
        }
        return true;
    }
}
