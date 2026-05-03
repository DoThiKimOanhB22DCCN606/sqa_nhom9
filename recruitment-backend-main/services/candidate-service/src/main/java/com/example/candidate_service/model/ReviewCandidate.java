package com.example.candidate_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "review_candidates")
@Getter
@Setter
public class ReviewCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @JsonIgnore
    private Candidate candidate;

    /**
     * EmployeeId người đánh giá
     */
    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    // ========== Các trường dùng cho INTERVIEW ==========
    /**
     * Điểm kỹ năng chuyên môn (1-5) - INTERVIEW
     */
    @Column(name = "professional_skill_score")
    private Integer professionalSkillScore;

    /**
     * Điểm kỹ năng giao tiếp (1-5) - INTERVIEW
     */
    @Column(name = "communication_skill_score")
    private Integer communicationSkillScore;

    /**
     * Điểm kinh nghiệm làm việc (1-5) - INTERVIEW
     */
    @Column(name = "work_experience_score")
    private Integer workExperienceScore;

    /**
     * Điểm mạnh của ứng viên
     */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /**
     * Điểm yếu của ứng viên
     */
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    /**
     * Kết luận đánh giá (true/false) - INTERVIEW
     * true = đạt, false = không đạt
     */
    @Column(name = "conclusion")
    private Boolean conclusion;

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
