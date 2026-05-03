package com.example.user_service.controller;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.review.CreateReviewEmployeeDTO;
import com.example.user_service.dto.review.ReviewEmployeeResponseDTO;
import com.example.user_service.dto.review.UpdateReviewEmployeeDTO;
import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.service.ReviewEmployeeService;
import com.example.user_service.utils.SecurityUtil;
import com.example.user_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/user-service/reviews")
public class ReviewEmployeeController {

    private final ReviewEmployeeService reviewEmployeeService;

    public ReviewEmployeeController(ReviewEmployeeService reviewEmployeeService) {
        this.reviewEmployeeService = reviewEmployeeService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách đánh giá với filter và pagination")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "employeeId", required = false) Long employeeId,
            @RequestParam(name = "reviewerId", required = false) Long reviewerId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {
        PaginationDTO result = reviewEmployeeService.getAllWithFilters(
                employeeId, reviewerId, startDate, endDate, page, limit, sortBy, sortOrder);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết đánh giá")
    public ResponseEntity<ReviewEmployeeResponseDTO> getById(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(reviewEmployeeService.getById(id));
    }

    @PostMapping
    @ApiMessage("Tạo đánh giá")
    public ResponseEntity<ReviewEmployeeResponseDTO> create(@Validated @RequestBody CreateReviewEmployeeDTO dto)
            throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(reviewEmployeeService.create(dto, reviewerId));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật đánh giá")
    public ResponseEntity<ReviewEmployeeResponseDTO> update(@PathVariable Long id,
            @Validated @RequestBody UpdateReviewEmployeeDTO dto) throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(reviewEmployeeService.update(id, dto, reviewerId));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa đánh giá")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        Long reviewerId = SecurityUtil.extractEmployeeId();
        reviewEmployeeService.delete(id, reviewerId);
        return ResponseEntity.noContent().build();
    }
}
