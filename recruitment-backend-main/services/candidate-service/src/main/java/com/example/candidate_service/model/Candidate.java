package com.example.candidate_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.candidate_service.utils.SecurityUtil;
import com.example.candidate_service.utils.enums.CandidateStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

@Entity
@Table(name = "candidates")
@Getter
@Setter
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;
    private String nationality;

    @Column(unique = true)
    private String idNumber;

    private String address;
    private String avatarUrl;

    private String highestEducation;
    private String university;
    private String graduationYear;
    private BigDecimal gpa;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate appliedDate;
    @Enumerated(EnumType.STRING)
    private CandidateStatus status;
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    private String resumeUrl;
    @Column(columnDefinition = "TEXT")

    private Long jobPositionId;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL)
    private Set<Comment> comments;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL)
    private Set<ReviewCandidate> reviews;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.createdBy = SecurityUtil.extractEmployeeId();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = SecurityUtil.extractEmployeeId();
    }

}