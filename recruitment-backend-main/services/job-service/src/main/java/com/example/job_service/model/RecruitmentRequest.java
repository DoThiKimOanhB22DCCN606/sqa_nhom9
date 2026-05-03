package com.example.job_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.job_service.utils.enums.RecruitmentRequestStatus;

@Entity
@Table(name = "recruitment_requests")
@Getter
@Setter
public class RecruitmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int quantity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentRequestStatus status;

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

    // @Column(columnDefinition = "TEXT")
    // private String approvalNotes;
    // private Long approvedId; // CEO Employee Id (lưu employeeId)
    private Long requesterId; // Employee request Id (lưu employeeId)

    /**
     * Người chịu trách nhiệm chính (Submitter/Owner)
     * Thường là giống requesterId, nhưng có thể khác nếu thư ký nhập hộ sếp
     */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /**
     * Ngày gửi đi (khác ngày tạo nháp)
     * NULL nếu chưa submit (status = DRAFT)
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private Long workflowId;

    // private LocalDateTime approvedAt;
    private boolean isActive;

    // @ManyToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "job_category_id", nullable = false)
    // private JobCategory jobCategory;

    private Long departmentId;
}
