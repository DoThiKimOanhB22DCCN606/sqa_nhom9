package com.example.workflow_service.controller;

import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.workflow.CreateWorkflowDTO;
import com.example.workflow_service.dto.workflow.UpdateWorkflowDTO;
import com.example.workflow_service.dto.workflow.WorkflowResponseDTO;
import com.example.workflow_service.service.WorkflowService;
import com.example.workflow_service.utils.annotation.ApiMessage;
import com.example.workflow_service.utils.enums.WorkflowType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflow-service/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    @ApiMessage("Lấy danh sách workflow với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "type", required = false) WorkflowType type,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
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

        return ResponseEntity.ok(workflowService.getAll(type, isActive, keyword, departmentId, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin workflow theo ID")
    public ResponseEntity<WorkflowResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getById(id));
    }

    @PostMapping
    @ApiMessage("Tạo mới workflow")
    public ResponseEntity<WorkflowResponseDTO> create(@Valid @RequestBody CreateWorkflowDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(dto));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật workflow")
    public ResponseEntity<WorkflowResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowDTO dto) {
        return ResponseEntity.ok(workflowService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa workflow")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
