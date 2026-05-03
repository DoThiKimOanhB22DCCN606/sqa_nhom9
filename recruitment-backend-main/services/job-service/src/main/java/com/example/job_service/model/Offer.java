package com.example.job_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.job_service.utils.enums.OfferStatus;

@Entity
@Table(name = "offers")
@Getter
@Setter
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID của ứng viên
     */
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    /**
     * Lương cơ bản (VNĐ)
     */
    @Column(name = "basic_salary")
    private Long basicSalary;

    /**
     * Tỷ lệ lương thử việc (%)
     * Ví dụ: 85 = 85% lương chính thức
     */
    @Column(name = "probation_salary_rate")
    private Integer probationSalaryRate;

    /**
     * Ngày onboarding
     */
    @Column(name = "onboarding_date")
    private LocalDate onboardingDate;

    /**
     * Thời gian thử việc (tháng)
     */
    @Column(name = "probation_period")
    private Integer probationPeriod;

    /**
     * Ghi chú về offer
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status;

    /**
     * Người tạo yêu cầu
     */
    @Column(name = "requester_id")
    private Long requesterId;

    /**
     * Người chịu trách nhiệm chính (Submitter/Owner)
     * Thường là giống requesterId, nhưng có thể khác nếu thư ký nhập hộ sếp
     */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /**
     * ID của workflow
     */
    @Column(name = "workflow_id")
    private Long workflowId;

    /**
     * Ngày gửi đi (khác ngày tạo nháp)
     * NULL nếu chưa submit (status = DRAFT)
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * Trạng thái active
     */
    private Boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
