package com.example.job_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.recruitment.ApproveRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CancelRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CreateRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.RecruitmentRequestWithUserDTO;
import com.example.job_service.dto.recruitment.RejectRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.ReturnRecruitmentRequestDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.service.RecruitmentRequestService;
import com.example.job_service.utils.SecurityUtil;
import com.example.job_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/job-service/recruitment-requests")
public class RecruitmentRequestController {
    private final RecruitmentRequestService recruitmentRequestService;

    public RecruitmentRequestController(RecruitmentRequestService recruitmentRequestService) {
        this.recruitmentRequestService = recruitmentRequestService;
    }

    @PostMapping
    @ApiMessage("Tạo yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> create(@Validated @RequestBody CreateRecruitmentRequestDTO dto) {
        Long requesterId = SecurityUtil.extractEmployeeId();
        dto.setRequesterId(requesterId);
        return ResponseEntity.ok(recruitmentRequestService.create(dto));
    }

    @PostMapping("/submit/{id}")
    @ApiMessage("Submit yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> submit(@PathVariable Long id) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.submit(id, actorId, token));
    }

    @PostMapping("approve/{id}")
    @ApiMessage("Phê duyệt bước hiện tại của yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> approveStep(
            @PathVariable Long id,
            @Validated @RequestBody ApproveRecruitmentRequestDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.approveStep(id, dto, actorId, token));
    }

    @PostMapping("reject/{id}")
    @ApiMessage("Từ chối bước hiện tại của yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> rejectStep(
            @PathVariable Long id,
            @Validated @RequestBody RejectRecruitmentRequestDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.rejectStep(id, dto, actorId, token));
    }

    @PostMapping("/return/{id}")
    @ApiMessage("Trả lại yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> returnRequest(
            @PathVariable Long id,
            @Validated @RequestBody ReturnRecruitmentRequestDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.returnRequest(id, dto, actorId, token));
    }

    @PostMapping("/cancel/{id}")
    @ApiMessage("Hủy yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> cancelRequest(
            @PathVariable Long id,
            @Validated @RequestBody CancelRecruitmentRequestDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.cancel(id, dto, actorId, token));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy yêu cầu tuyển dụng theo id")
    public ResponseEntity<RecruitmentRequestWithUserDTO> getById(@PathVariable Long id) throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.getByIdWithUser(id, token));
    }

    @GetMapping
    @ApiMessage("Lấy danh sách yêu cầu tuyển dụng với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAllDetail(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdBy", required = false) Long createdBy,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {

        // Validate pagination parameters
        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;

        // Create sort object
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Create pageable object
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }

        return ResponseEntity.ok(recruitmentRequestService.getAllWithFilters(
                departmentId, status, createdBy, keyword, token, pageable));
    }

    @GetMapping("/simple")
    @ApiMessage("Lấy danh sách yêu cầu tuyển dụng đơn giản")
    public ResponseEntity<List<RecruitmentRequest>> getAllSimple(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdBy", required = false) Long createdBy,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(recruitmentRequestService.findAllWithFilters(
                departmentId, status, createdBy, keyword));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật yêu cầu tuyển dụng")
    public ResponseEntity<RecruitmentRequest> update(@PathVariable Long id,
            @Validated @RequestBody CreateRecruitmentRequestDTO dto) throws IdInvalidException {
        return ResponseEntity.ok(recruitmentRequestService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa yêu cầu tuyển dụng")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        recruitmentRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw/{id}")
    @ApiMessage("Rút lại yêu cầu tuyển dụng (chỉ submitter/owner mới có thể rút)")
    public ResponseEntity<RecruitmentRequest> withdrawRequest(
            @PathVariable Long id) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(recruitmentRequestService.withdraw(id, actorId, token));
    }

}
