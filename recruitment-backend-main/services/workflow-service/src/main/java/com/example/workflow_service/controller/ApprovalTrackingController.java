package com.example.workflow_service.controller;

import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.approval.ApprovalTrackingResponseDTO;
import com.example.workflow_service.dto.approval.ApproveStepDTO;
import com.example.workflow_service.dto.approval.CreateApprovalTrackingDTO;
import com.example.workflow_service.dto.approval.RequestWorkflowInfoDTO;
import com.example.workflow_service.service.ApprovalTrackingService;
import com.example.workflow_service.utils.annotation.ApiMessage;
import com.example.workflow_service.utils.enums.ApprovalStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-service/approval-trackings")
@RequiredArgsConstructor
public class ApprovalTrackingController {

    private final ApprovalTrackingService approvalTrackingService;

    @GetMapping
    @ApiMessage("Lấy danh sách approval tracking với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "requestId", required = false) Long requestId,
            @RequestParam(name = "status", required = false) ApprovalStatus status,
            @RequestParam(name = "approverPositionId", required = false) Long approverPositionId,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {
        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;

        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        return ResponseEntity.ok(approvalTrackingService.getAll(
                requestId, status, approverPositionId, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin approval tracking theo ID")
    public ResponseEntity<ApprovalTrackingResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(approvalTrackingService.getById(id));
    }

    @GetMapping("/pending/{userId}")
    @ApiMessage("Lấy danh sách approval tracking đang chờ phê duyệt của một user")
    public ResponseEntity<List<ApprovalTrackingResponseDTO>> getPendingApprovals(@PathVariable Long userId) {
        return ResponseEntity.ok(approvalTrackingService.getPendingApprovalsForUser(userId));
    }

    @PostMapping("/initialize")
    @ApiMessage("Khởi tạo approval tracking cho một yêu cầu (tự động tìm workflow và tạo bước đầu tiên)")
    public ResponseEntity<ApprovalTrackingResponseDTO> initializeApproval(
            @Valid @RequestBody CreateApprovalTrackingDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(approvalTrackingService.initializeApproval(dto));
    }

    @PostMapping("/{id}/approve")
    @ApiMessage("Phê duyệt hoặc từ chối approval tracking")
    public ResponseEntity<ApprovalTrackingResponseDTO> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveStepDTO dto) {
        return ResponseEntity.ok(approvalTrackingService.approve(id, dto));
    }

    @GetMapping("/by-request/{requestId}")
    @ApiMessage("Lấy thông tin workflow và approval tracking theo requestId")
    public ResponseEntity<RequestWorkflowInfoDTO> getWorkflowInfoByRequestId(
            @PathVariable Long requestId,
            @RequestParam(name = "workflowId", required = false) Long workflowId,
            @RequestParam(name = "requestType", required = false) String requestType) {
        return ResponseEntity
                .ok(approvalTrackingService.getWorkflowInfoByRequestId(requestId, workflowId, requestType));
    }

}
