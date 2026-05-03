package com.example.job_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.jobposition.CreateJobPositionDTO;
import com.example.job_service.dto.jobposition.JobPositionResponseDTO;
import com.example.job_service.dto.jobposition.UpdateJobPositionDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.JobPosition;
import com.example.job_service.service.JobPositionService;
import com.example.job_service.utils.SecurityUtil;
import com.example.job_service.utils.annotation.ApiMessage;
import com.example.job_service.utils.enums.JobPositionStatus;

@RestController
@RequestMapping("/api/v1/job-service/job-positions")
public class JobPositionController {
    private final JobPositionService jobPositionService;

    public JobPositionController(JobPositionService jobPositionService) {
        this.jobPositionService = jobPositionService;
    }

    @PostMapping
    @ApiMessage("Tạo vị trí tuyển dụng từ yêu cầu đã duyệt")
    public ResponseEntity<JobPosition> create(@Validated @RequestBody CreateJobPositionDTO dto)
            throws IdInvalidException {
        JobPosition position = jobPositionService.create(dto);
        return ResponseEntity.ok(position);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin vị trí tuyển dụng theo ID")
    public ResponseEntity<JobPositionResponseDTO> getById(@PathVariable Long id) throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(jobPositionService.getByIdWithDepartmentName(id, token));
    }

    @GetMapping("/simple/{id}")
    @ApiMessage("Lấy thông tin vị trí tuyển dụng đơn giản theo ID (không gọi service khác)")
    public ResponseEntity<JobPosition> getByIdSimple(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(jobPositionService.getByIdSimple(id));
    }

    @GetMapping
    @ApiMessage("Lấy danh sách vị trí tuyển dụng với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAllDetail(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "status", required = false) JobPositionStatus status,
            @RequestParam(name = "published", required = false) Boolean published,
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

        return ResponseEntity.ok(jobPositionService.findAllWithFilters(
                departmentId, status, published, keyword, pageable, token));
    }

    @GetMapping("/simple")
    @ApiMessage("Lấy danh sách vị trí tuyển dụng đơn giản")
    public ResponseEntity<PaginationDTO> getAllSimple(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "status", required = false) JobPositionStatus status,
            @RequestParam(name = "published", required = false) Boolean published,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "ids", required = false) String ids,
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

        return ResponseEntity.ok(jobPositionService.findAllWithFiltersSimplePaged(
                departmentId, status, published, keyword, ids, pageable));
    }

    @GetMapping(params = "ids")
    @ApiMessage("Lấy danh sách vị trí tuyển dụng theo IDs với thông tin phòng ban")
    public ResponseEntity<List<JobPositionResponseDTO>> getByIds(
            @RequestParam(name = "ids") String ids) {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Token không hợp lệ");
        }

        List<Long> idList = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();

        return ResponseEntity.ok(jobPositionService.getByIdsWithDepartmentName(idList, token));
    }

    @GetMapping("/published")
    @ApiMessage("Lấy danh sách vị trí tuyển dụng đã xuất bản (phân trang)")
    public ResponseEntity<PaginationDTO> getPublishedPaged(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "keyword", required = false) String keyword,
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

        return ResponseEntity.ok(
                jobPositionService.findAllWithFiltersSimplePaged(
                        departmentId,
                        JobPositionStatus.PUBLISHED,
                        true,
                        keyword,
                        null, // ids
                        pageable));
    }

    @GetMapping("/published/{id}")
    @ApiMessage("Lấy thông tin vị trí tuyển dụng đã được xuất bản theo ID")
    public ResponseEntity<JobPosition> getPublishedById(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(jobPositionService.getByIdWithPublished(id));
    }

    // @GetMapping("/department/{departmentId}")
    // @ApiMessage("Lấy danh sách vị trí tuyển dụng theo phòng ban")
    // public ResponseEntity<PaginationDTO> getByDepartmentId(@PathVariable Long
    // departmentId,
    // @RequestParam(name = "currentPage", defaultValue = "1", required = false)
    // Optional<String> currentPageOptional,
    // @RequestParam(name = "pageSize", defaultValue = "10", required = false)
    // Optional<String> pageSizeOptional) {
    // String sCurrentPage = currentPageOptional.orElse("1");
    // String sPageSize = pageSizeOptional.orElse("10");

    // int current = Integer.parseInt(sCurrentPage);
    // int pageSize = Integer.parseInt(sPageSize);
    // Pageable pageable = PageRequest.of(current - 1, pageSize);

    // PaginationDTO positions = jobPositionService.findByDepartmentId(departmentId,
    // pageable);
    // return ResponseEntity.ok(positions);
    // }

    // @GetMapping("/department/{departmentId}/status/{status}")
    // @ApiMessage("Lấy danh sách vị trí tuyển dụng theo phòng ban và trạng thái")
    // public ResponseEntity<List<JobPositionResponseDTO>>
    // getByDepartmentIdAndStatus(
    // @PathVariable Long departmentId,
    // @PathVariable JobPositionStatus status) {
    // List<JobPosition> positions =
    // jobPositionService.findByDepartmentIdAndStatus(departmentId, status);
    // List<JobPositionResponseDTO> response = positions.stream()
    // .map(JobPositionResponseDTO::fromEntity)
    // .toList();
    // return ResponseEntity.ok(response);
    // }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật vị trí tuyển dụng")
    public ResponseEntity<JobPositionResponseDTO> update(@PathVariable Long id,
            @Validated @RequestBody UpdateJobPositionDTO dto) throws IdInvalidException {
        JobPosition position = jobPositionService.update(id, dto);
        return ResponseEntity.ok(JobPositionResponseDTO.fromEntity(position));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa vị trí tuyển dụng")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        jobPositionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @ApiMessage("Publish vị trí tuyển dụng")
    public ResponseEntity<JobPosition> publish(@PathVariable Long id) throws IdInvalidException {
        JobPosition position = jobPositionService.publish(id);
        return ResponseEntity.ok(position);
    }

    @PostMapping("/{id}/close")
    @ApiMessage("Đóng vị trí tuyển dụng")
    public ResponseEntity<JobPositionResponseDTO> close(@PathVariable Long id) throws IdInvalidException {
        JobPosition position = jobPositionService.close(id);
        return ResponseEntity.ok(JobPositionResponseDTO.fromEntity(position));
    }

    @PostMapping("/{id}/reopen")
    @ApiMessage("Mở lại vị trí tuyển dụng")
    public ResponseEntity<JobPositionResponseDTO> reopen(@PathVariable Long id) throws IdInvalidException {
        JobPosition position = jobPositionService.reopen(id);
        return ResponseEntity.ok(JobPositionResponseDTO.fromEntity(position));
    }

}
