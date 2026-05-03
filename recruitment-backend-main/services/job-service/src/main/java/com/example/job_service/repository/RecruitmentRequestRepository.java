package com.example.job_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;

import java.util.List;

@Repository
public interface RecruitmentRequestRepository extends JpaRepository<RecruitmentRequest, Long> {
        List<RecruitmentRequest> findByRequesterId(Long requesterId);

        List<RecruitmentRequest> findByStatus(RecruitmentRequestStatus status);

        List<RecruitmentRequest> findByDepartmentId(Long departmentId);

        Page<RecruitmentRequest> findByDepartmentIdAndIsActiveTrue(Long departmentId, Pageable pageable);

        Page<RecruitmentRequest> findAllByIsActiveTrue(Pageable pageable);

        @Query("SELECT rr FROM RecruitmentRequest rr WHERE " +
                        "rr.isActive = true AND " +
                        "(:departmentId IS NULL OR rr.departmentId = :departmentId) AND " +
                        "(:status IS NULL OR rr.status = :status) AND " +
                        "(:createdBy IS NULL OR rr.requesterId = :createdBy) AND " +
                        "(:keyword IS NULL OR LOWER(rr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(rr.reason) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<RecruitmentRequest> findByFilters(@Param("departmentId") Long departmentId,
                        @Param("status") RecruitmentRequestStatus status,
                        @Param("createdBy") Long createdBy,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT rr FROM RecruitmentRequest rr WHERE " +
                        "rr.isActive = true AND " +
                        "(:departmentId IS NULL OR rr.departmentId = :departmentId) AND " +
                        "(:status IS NULL OR rr.status = :status) AND " +
                        "(:createdBy IS NULL OR rr.requesterId = :createdBy) AND " +
                        "(:keyword IS NULL OR LOWER(rr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(rr.reason) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<RecruitmentRequest> findByFiltersList(@Param("departmentId") Long departmentId,
                        @Param("status") RecruitmentRequestStatus status,
                        @Param("createdBy") Long createdBy,
                        @Param("keyword") String keyword);
}
