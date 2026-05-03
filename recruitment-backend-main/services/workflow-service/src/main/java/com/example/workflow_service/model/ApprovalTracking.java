package com.example.workflow_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.workflow_service.utils.enums.ApprovalStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_trackings")
@Getter
@Setter
public class ApprovalTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID của yêu cầu (vd: recruitment_request.id = 123)
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    // Liên kết với bước workflow
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private WorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * Loại hành động: APPROVE, REJECT, RETURN, CANCEL
     */
    @Column(name = "action_type", length = 20)
    private String actionType;

    /**
     * Bước được trả về (khi actionType = 'RETURN')
     */
    @Column(name = "returned_to_step_id")
    private Long returnedToStepId;

    // User được gán để phê duyệt (tìm từ User_Positions dựa trên
    // approver_position_id)
    @Column(name = "approver_position_id")
    private Long approverPositionId;

    // User thực hiện hành động (approve/reject)
    @Column(name = "action_user_id")
    private Long actionUserId;

    // Thời gian thực hiện hành động
    @Column(name = "action_at")
    private LocalDateTime actionAt;

    // Ghi chú phê duyệt/từ chối
    @Column(columnDefinition = "MEDIUMTEXT")
    private String notes;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
