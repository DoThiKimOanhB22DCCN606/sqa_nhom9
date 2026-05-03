package com.example.user_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "review_employees")
@Getter
@Setter
public class ReviewEmployee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * EmployeeId người được đánh giá (PROBATION)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private Employee employee;

    /**
     * EmployeeId người đánh giá
     */
    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    // ========== Các trường dùng cho PROBATION - Đánh giá năng lực chuyên môn
    // ==========
    /**
     * Điểm hoàn thành công việc đúng tiến độ (1-5) - PROBATION
     */
    @Column(name = "on_time_completion_score")
    private Integer onTimeCompletionScore;

    /**
     * Điểm hiệu quả trong công việc (1-5) - PROBATION
     */
    @Column(name = "work_efficiency_score")
    private Integer workEfficiencyScore;

    /**
     * Điểm kỹ năng chuyên môn (1-5) - PROBATION
     */
    @Column(name = "professional_skill_score")
    private Integer professionalSkillScore;

    /**
     * Điểm tự học hỏi, phát triển bản thân (1-5) - PROBATION
     */
    @Column(name = "self_learning_score")
    private Integer selfLearningScore;

    // ========== Các trường dùng cho PROBATION - Đánh giá tính cách và tác phong
    // ==========
    /**
     * Điểm thái độ làm việc (1-5) - PROBATION
     */
    @Column(name = "work_attitude_score")
    private Integer workAttitudeScore;

    /**
     * Điểm kỹ năng giao tiếp (1-5) - PROBATION
     */
    @Column(name = "communication_skill_score")
    private Integer communicationSkillScore;

    /**
     * Điểm tính trung thực và trách nhiệm (1-5) - PROBATION
     */
    @Column(name = "honesty_responsibility_score")
    private Integer honestyResponsibilityScore;

    /**
     * Điểm khả năng hòa nhập với tập thể (1-5) - PROBATION
     */
    @Column(name = "team_integration_score")
    private Integer teamIntegrationScore;

    // ========== Kết quả thử việc ==========
    /**
     * Kết quả đánh giá: true = Đạt yêu cầu, false = Chưa đạt yêu cầu
     */
    @Column(name = "probation_result")
    private Boolean probationResult;

    /**
     * Nhận xét bổ sung (dùng cho PROBATION)
     */
    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
