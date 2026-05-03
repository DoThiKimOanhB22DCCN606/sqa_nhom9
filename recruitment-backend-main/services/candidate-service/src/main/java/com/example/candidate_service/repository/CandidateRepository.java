package com.example.candidate_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.utils.enums.CandidateStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
        Optional<Candidate> findByEmail(String email);

        boolean existsByEmail(String email);

        boolean existsByEmailAndJobPositionId(String email, Long jobPositionId);

        List<Candidate> findByJobPositionId(Long jobPositionId);

        @Query("SELECT COUNT(c) FROM Candidate c WHERE c.jobPositionId = :jobPositionId")
        long countByJobPositionId(@Param("jobPositionId") Long jobPositionId);

        @Query("SELECT DISTINCT c FROM Candidate c WHERE " +
                        "(:jobPositionId IS NULL OR c.jobPositionId = :jobPositionId) AND " +
                        "(:status IS NULL OR c.status = :status) AND " +
                        "(:candidateId IS NULL OR c.id = :candidateId) AND " +
                        "(:startDate IS NULL OR c.appliedDate >= :startDate) AND " +
                        "(:endDate IS NULL OR c.appliedDate <= :endDate) AND " +
                        "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
                        +
                        "(:jobPositionIds IS NULL OR c.jobPositionId IN :jobPositionIds)")
        Page<Candidate> findByFilters(
                        @Param("jobPositionId") Long jobPositionId,
                        @Param("status") CandidateStatus status,
                        @Param("candidateId") Long candidateId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("keyword") String keyword,
                        @Param("jobPositionIds") List<Long> jobPositionIds,
                        Pageable pageable);
}
