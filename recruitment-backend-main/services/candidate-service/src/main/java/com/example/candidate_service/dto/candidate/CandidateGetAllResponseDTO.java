package com.example.candidate_service.dto.candidate;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.utils.enums.CandidateStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateGetAllResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String idNumber;
    private String address;
    private String avatarUrl;
    private String highestEducation;
    private String university;
    private String graduationYear;
    private BigDecimal gpa;
    private String notes;

    private LocalDate appliedDate;
    private CandidateStatus status;
    private String resumeUrl;
    private Long jobPositionId;
    private String jobPositionTitle;
    private Long departmentId;
    // private String departmentName;

    public static CandidateGetAllResponseDTO fromEntity(Candidate candidate) {
        CandidateGetAllResponseDTO dto = new CandidateGetAllResponseDTO();
        dto.setId(candidate.getId());
        dto.setName(candidate.getName());
        dto.setEmail(candidate.getEmail());
        dto.setPhone(candidate.getPhone());
        dto.setDateOfBirth(candidate.getDateOfBirth());
        dto.setGender(candidate.getGender());
        dto.setNationality(candidate.getNationality());
        dto.setIdNumber(candidate.getIdNumber());
        dto.setAddress(candidate.getAddress());
        dto.setAvatarUrl(candidate.getAvatarUrl());
        dto.setHighestEducation(candidate.getHighestEducation());
        dto.setUniversity(candidate.getUniversity());
        dto.setGraduationYear(candidate.getGraduationYear());
        dto.setGpa(candidate.getGpa());
        dto.setNotes(candidate.getNotes());
        dto.setAppliedDate(candidate.getAppliedDate());
        dto.setStatus(candidate.getStatus());
        dto.setResumeUrl(candidate.getResumeUrl());
        dto.setJobPositionId(candidate.getJobPositionId());
        return dto;
    }
}
