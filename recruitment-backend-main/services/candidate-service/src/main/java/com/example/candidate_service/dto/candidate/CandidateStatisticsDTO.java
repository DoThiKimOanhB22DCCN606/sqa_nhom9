package com.example.candidate_service.dto.candidate;

import java.time.LocalDate;

import com.example.candidate_service.utils.enums.CandidateStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateStatisticsDTO {
    private Long id;
    private LocalDate appliedDate;
    private CandidateStatus status;
    private Long jobPositionId;
    private Long departmentId;
    private Long candidateId;
}
