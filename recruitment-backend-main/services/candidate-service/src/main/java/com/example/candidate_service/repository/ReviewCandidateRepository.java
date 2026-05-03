package com.example.candidate_service.repository;

import com.example.candidate_service.model.ReviewCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewCandidateRepository extends JpaRepository<ReviewCandidate, Long> {

        List<ReviewCandidate> findByCandidate_Id(Long candidateId);

        List<ReviewCandidate> findByReviewerId(Long reviewerId);

        List<ReviewCandidate> findByCandidate_IdAndReviewerId(Long candidateId, Long reviewerId);

        @Query("SELECT r FROM ReviewCandidate r WHERE " +
                        "(:candidateId IS NULL OR r.candidate.id = :candidateId) AND " +
                        "(:reviewerId IS NULL OR r.reviewerId = :reviewerId) AND " +
                        "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR r.createdAt <= :endDate)")
        Page<ReviewCandidate> findByFilters(@Param("candidateId") Long candidateId,
                        @Param("reviewerId") Long reviewerId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);
}
