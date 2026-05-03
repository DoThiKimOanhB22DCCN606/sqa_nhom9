package com.example.candidate_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.candidate_service.dto.PaginationDTO;
import com.example.candidate_service.dto.candidate.CandidateDetailResponseDTO;
import com.example.candidate_service.dto.candidate.CandidateGetAllResponseDTO;
import com.example.candidate_service.dto.candidate.CandidateStatisticsDTO;
import com.example.candidate_service.dto.candidate.CreateCandidateDTO;
import com.example.candidate_service.dto.candidate.UpdateCandidateDTO;
import com.example.candidate_service.dto.review.ReviewCandidateResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.service.CandidateService;
import com.example.candidate_service.service.ReviewCandidateService;
import com.example.candidate_service.utils.SecurityUtil;
import com.example.candidate_service.utils.annotation.ApiMessage;
import com.example.candidate_service.utils.enums.CandidateStatus;

@RestController
@RequestMapping("/api/v1/candidate-service/candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final ReviewCandidateService reviewCandidateService;

    public CandidateController(CandidateService candidateService, ReviewCandidateService reviewCandidateService) {
        this.candidateService = candidateService;
        this.reviewCandidateService = reviewCandidateService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách ứng viên với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAllCandidates(
            @RequestParam(name = "candidateId", required = false) Long candidateId,
            @RequestParam(name = "jobPositionId", required = false) Long jobPositionId,
            @RequestParam(name = "status", required = false) CandidateStatus status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
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

        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(candidateService.getAllWithFilters(
                candidateId, jobPositionId, status, startDate, endDate, keyword, departmentId, pageable, token));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin ứng viên")
    public ResponseEntity<CandidateDetailResponseDTO> getById(@PathVariable Long id) throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        CandidateDetailResponseDTO candidate = candidateService.getCandidateDetailById(id, token);
        return ResponseEntity.ok(candidate);
    }

    @GetMapping("/{id}/department")
    @ApiMessage("Lấy departmentId của ứng viên")
    public ResponseEntity<Long> getDepartmentIdByCandidate(@PathVariable Long id) {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        Long departmentId = candidateService.getDepartmentIdByCandidateId(id, token);
        return ResponseEntity.ok(departmentId);
    }

    @GetMapping("/{id}/reviews")
    @ApiMessage("Lấy danh sách review của một ứng viên")
    public ResponseEntity<List<ReviewCandidateResponseDTO>> getReviewsByCandidate(@PathVariable Long id) {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(reviewCandidateService.getByCandidateId(id, token));
    }

    @PostMapping
    @ApiMessage("Tạo mới ứng viên")
    public ResponseEntity<Candidate> create(@Validated @RequestBody CreateCandidateDTO dto)
            throws IdInvalidException {
        Candidate saved = candidateService.create(dto);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật thông tin ứng viên")
    public ResponseEntity<CandidateDetailResponseDTO> update(@PathVariable Long id,
            @Validated @RequestBody UpdateCandidateDTO dto)
            throws IdInvalidException {
        CandidateDetailResponseDTO saved = candidateService.update(id, dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa ứng viên")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(params = "ids")
    public ResponseEntity<List<CandidateDetailResponseDTO>> findByIds(@RequestParam("ids") String ids) {
        List<Long> candidateIds = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.candidateService.getByIds(candidateIds));
    }

    @PutMapping("/status/{id}")
    @ApiMessage("Cập nhật trạng thái ứng viên")
    public ResponseEntity<CandidateDetailResponseDTO> updateCandidateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String feedback) throws IdInvalidException {
        return ResponseEntity.ok(candidateService.updateCandidateStatus(id, status, feedback,
                SecurityUtil.extractEmployeeId()));
    }

    @GetMapping("/count")
    @ApiMessage("Đếm số lượng ứng viên theo jobPositionId ")
    public ResponseEntity<Long> countCandidatesByJobPositionId(
            @RequestParam(name = "jobPositionId", required = false) Long jobPositionId) {
        return ResponseEntity.ok(candidateService.countCandidatesByJobPositionId(jobPositionId));
    }

    @GetMapping("/statistics")
    @ApiMessage("Lấy dữ liệu ứng viên cho thống kê")
    public ResponseEntity<List<CandidateStatisticsDTO>> getCandidatesForStatistics(
            @RequestParam(name = "status", required = false) CandidateStatus status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "jobPositionId", required = false) Long jobPositionId,
            @RequestParam(name = "departmentId", required = false) Long departmentId) {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(candidateService.getCandidatesForStatistics(
                status, startDate, endDate, jobPositionId, departmentId, token));
    }

    @GetMapping("/interviewed")
    @ApiMessage("Lấy danh sách ứng viên mà người request đã tham gia phỏng vấn")
    public ResponseEntity<List<CandidateGetAllResponseDTO>> getCandidatesInterviewedByMe() {
        Long employeeId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(candidateService.getCandidatesByInterviewer(employeeId, token));
    }

    @PostMapping("/convert/{id}")
    @ApiMessage("Chuyển ứng viên thành nhân viên")
    public ResponseEntity<JsonNode> convertToEmployee(
            @PathVariable Long id,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "positionId", required = false) Long positionId) throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        // Set positionId mặc định là 4 (nhân viên) nếu không được cung cấp
        Long finalPositionId = positionId != null ? positionId : 4L;
        JsonNode employee = candidateService.convertCandidateToEmployee(id, departmentId, finalPositionId, token);
        return ResponseEntity.ok(employee);
    }
}
