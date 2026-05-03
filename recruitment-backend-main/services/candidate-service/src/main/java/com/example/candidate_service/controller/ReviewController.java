package com.example.candidate_service.controller;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.candidate_service.dto.PaginationDTO;
import com.example.candidate_service.dto.review.CreateReviewCandidateDTO;
import com.example.candidate_service.dto.review.ReviewCandidateResponseDTO;
import com.example.candidate_service.dto.review.UpdateReviewCandidateDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.service.ReviewCandidateService;
import com.example.candidate_service.utils.SecurityUtil;
import com.example.candidate_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/candidate-service/reviews")
public class ReviewController {

    private final ReviewCandidateService reviewCandidateService;

    public ReviewController(ReviewCandidateService reviewCandidateService) {
        this.reviewCandidateService = reviewCandidateService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách đánh giá với filter và pagination")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "candidateId", required = false) Long candidateId,
            @RequestParam(name = "reviewerId", required = false) Long reviewerId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        PaginationDTO result = reviewCandidateService.getAllWithFilters(
                candidateId, reviewerId, startDate, endDate, page, limit, sortBy, sortOrder, token);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết đánh giá")
    public ResponseEntity<ReviewCandidateResponseDTO> getById(@PathVariable Long id) throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(reviewCandidateService.getById(id, token));
    }

    @PostMapping
    @ApiMessage("Tạo đánh giá")
    public ResponseEntity<ReviewCandidateResponseDTO> create(@Validated @RequestBody CreateReviewCandidateDTO dto)
            throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(reviewCandidateService.create(dto, reviewerId));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật đánh giá")
    public ResponseEntity<ReviewCandidateResponseDTO> update(@PathVariable Long id,
            @Validated @RequestBody UpdateReviewCandidateDTO dto) throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(reviewCandidateService.update(id, dto, reviewerId));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa đánh giá")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        reviewCandidateService.delete(id, reviewerId);
        return ResponseEntity.noContent().build();
    }
}
