package com.example.job_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.job_service.model.Offer;
import com.example.job_service.utils.enums.OfferStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
        Page<Offer> findByIsActiveTrue(Pageable pageable);

        @Query("SELECT o FROM Offer o WHERE " +
                        "o.isActive = true AND " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:createdBy IS NULL OR o.requesterId = :createdBy) AND " +
                        "(:keyword IS NULL OR LOWER(o.notes) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Offer> findByFilters(@Param("status") OfferStatus status,
                        @Param("createdBy") Long createdBy,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT o FROM Offer o WHERE " +
                        "o.isActive = true AND " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:candidateId IS NULL OR o.candidateId = :candidateId) AND " +
                        "(:workflowId IS NULL OR o.workflowId = :workflowId) AND " +
                        "(:ownerUserId IS NULL OR o.ownerUserId = :ownerUserId) AND " +
                        "(:minSalary IS NULL OR o.basicSalary >= :minSalary) AND " +
                        "(:maxSalary IS NULL OR o.basicSalary <= :maxSalary) AND " +
                        "(:onboardingDateFrom IS NULL OR o.onboardingDate >= :onboardingDateFrom) AND " +
                        "(:onboardingDateTo IS NULL OR o.onboardingDate <= :onboardingDateTo) AND " +
                        "(:keyword IS NULL OR LOWER(o.notes) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<Offer> findByFiltersList(@Param("status") OfferStatus status,
                        @Param("candidateId") Long candidateId,
                        @Param("workflowId") Long workflowId,
                        @Param("ownerUserId") Long ownerUserId,
                        @Param("minSalary") Long minSalary,
                        @Param("maxSalary") Long maxSalary,
                        @Param("onboardingDateFrom") LocalDate onboardingDateFrom,
                        @Param("onboardingDateTo") LocalDate onboardingDateTo,
                        @Param("keyword") String keyword);
}
