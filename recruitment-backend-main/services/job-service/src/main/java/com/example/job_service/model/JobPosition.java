package com.example.job_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

import com.example.job_service.utils.SecurityUtil;
import com.example.job_service.utils.enums.JobPositionStatus;

@Entity
@Table(name = "job_positions")
@Getter
@Setter
public class JobPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;
    @Column(columnDefinition = "TEXT")
    private String benefits;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String employmentType;
    private String experienceLevel;
    private String location;
    private boolean isRemote;
    private int quantity;
    private LocalDate deadline;
    private String yearsOfExperience;
    public LocalDateTime publishedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPositionStatus status;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recruitment_request_id")
    private RecruitmentRequest recruitmentRequest;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    private String createBy;
    private String updateBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createBy = !SecurityUtil.extractUserEmail().isEmpty() == true
                ? SecurityUtil.extractUserEmail()
                : "";
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updateBy = !SecurityUtil.extractUserEmail().isEmpty() == true
                ? SecurityUtil.extractUserEmail()
                : "";
        this.updatedAt = LocalDateTime.now();
    }
}