package com.example.workflow_service.repository;

import com.example.workflow_service.model.ApprovalTracking;
import com.example.workflow_service.utils.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTrackingRepository extends JpaRepository<ApprovalTracking, Long> {

        // Tìm các approval tracking theo request_id
        List<ApprovalTracking> findByRequestId(Long requestId);

        // Tìm approval tracking đang PENDING cho một request và step cụ thể
        Optional<ApprovalTracking> findByRequestIdAndStepIdAndStatus(
                        Long requestId, Long stepId, ApprovalStatus status);

        // Tìm các approval tracking đang PENDING được gán cho một user
        List<ApprovalTracking> findByApproverPositionIdAndStatus(
                        Long approverPositionId, ApprovalStatus status);

        // Tìm approval tracking theo request_id và status
        List<ApprovalTracking> findByRequestIdAndStatus(Long requestId, ApprovalStatus status);

        // Tìm approval tracking theo request_id và step_id
        List<ApprovalTracking> findByRequestIdAndStepId(Long requestId, Long stepId);

        @Query("SELECT at FROM ApprovalTracking at WHERE " +
                        "(:requestId IS NULL OR at.requestId = :requestId) AND " +
                        "(:status IS NULL OR at.status = :status) AND " +
                        "(:approverPositionId IS NULL OR at.approverPositionId = :approverPositionId)")
        Page<ApprovalTracking> findByFilters(
                        @Param("requestId") Long requestId,
                        @Param("status") ApprovalStatus status,
                        @Param("approverPositionId") Long approverPositionId,
                        Pageable pageable);
}
