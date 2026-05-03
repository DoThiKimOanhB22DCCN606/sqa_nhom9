package com.example.job_service.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.offer.ApproveOfferDTO;
import com.example.job_service.dto.offer.CancelOfferDTO;
import com.example.job_service.dto.offer.CreateOfferDTO;
import com.example.job_service.dto.offer.OfferDetailDTO;
import com.example.job_service.dto.offer.RejectOfferDTO;
import com.example.job_service.dto.offer.ReturnOfferDTO;
import com.example.job_service.dto.offer.UpdateOfferDTO;
import com.example.job_service.dto.offer.WithdrawOfferDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.Offer;
import com.example.job_service.service.OfferService;
import com.example.job_service.utils.SecurityUtil;
import com.example.job_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/job-service/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    @ApiMessage("Tạo offer tiền lương")
    public ResponseEntity<Offer> create(@Validated @RequestBody CreateOfferDTO dto) throws IdInvalidException {
        Long requesterId = SecurityUtil.extractEmployeeId();
        Offer offer = offerService.create(dto);
        // Set requesterId directly in create method instead
        offer.setRequesterId(requesterId);
        offer = offerService.update(offer.getId(), new UpdateOfferDTO()); // Save requesterId
        return ResponseEntity.ok(offer);
    }

    @PostMapping("/submit/{id}")
    @ApiMessage("Submit offer tiền lương")
    public ResponseEntity<Offer> submit(@PathVariable Long id) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.submit(id, actorId, token));
    }

    @PostMapping("/approve/{id}")
    @ApiMessage("Phê duyệt bước hiện tại của offer tiền lương")
    public ResponseEntity<Offer> approveStep(
            @PathVariable Long id,
            @Validated @RequestBody ApproveOfferDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.approveStep(id, dto, actorId, token));
    }

    @PostMapping("/reject/{id}")
    @ApiMessage("Từ chối offer tiền lương")
    public ResponseEntity<Offer> rejectStep(
            @PathVariable Long id,
            @Validated @RequestBody RejectOfferDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.rejectStep(id, dto, actorId, token));
    }

    @PostMapping("/return/{id}")
    @ApiMessage("Trả về offer tiền lương để chỉnh sửa")
    public ResponseEntity<Offer> returnOffer(
            @PathVariable Long id,
            @Validated @RequestBody ReturnOfferDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.returnOffer(id, dto, actorId, token));
    }

    @PostMapping("/cancel/{id}")
    @ApiMessage("Hủy offer tiền lương")
    public ResponseEntity<Offer> cancel(
            @PathVariable Long id,
            @Validated @RequestBody CancelOfferDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.cancel(id, dto, actorId, token));
    }

    @PostMapping("/withdraw/{id}")
    @ApiMessage("Rút lại offer tiền lương")
    public ResponseEntity<Offer> withdraw(
            @PathVariable Long id,
            @Validated @RequestBody WithdrawOfferDTO dto) throws IdInvalidException {
        Long actorId = SecurityUtil.extractEmployeeId();
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return ResponseEntity.ok(offerService.withdraw(id, dto, actorId, token));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy offer tiền lương theo id (chi tiết)")
    public ResponseEntity<OfferDetailDTO> getById(@PathVariable Long id)
            throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        OfferDetailDTO detail = offerService.getByIdDetail(id, token);
        return ResponseEntity.ok(detail);
    }

    // @GetMapping("/department/{departmentId}")
    // @ApiMessage("Lấy danh sách offer tiền lương theo phòng ban")
    // public ResponseEntity<PaginationDTO> getAllByDepartmentId(
    // @PathVariable Long departmentId,
    // @RequestParam(name = "currentPage", defaultValue = "1", required = false)
    // Optional<String> currentPageOptional,
    // @RequestParam(name = "pageSize", defaultValue = "10", required = false)
    // Optional<String> pageSizeOptional) {
    // String sCurrentPage = currentPageOptional.orElse("1");
    // String sPageSize = pageSizeOptional.orElse("10");

    // int current = Integer.parseInt(sCurrentPage);
    // int pageSize = Integer.parseInt(sPageSize);
    // Pageable pageable = PageRequest.of(current - 1, pageSize);
    // String token = SecurityUtil.getCurrentUserJWT().orElse(null);
    // if (token == null) {
    // throw new RuntimeException("Token không hợp lệ");
    // }
    // return
    // ResponseEntity.ok(offerService.getAllByDepartmentIdWithUser(departmentId,
    // token, pageable));
    // }

    @GetMapping
    @ApiMessage("Lấy danh sách offer tiền lương với bộ lọc, phân trang và sắp xếp")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdBy", required = false) Long createdBy,
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

        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ");
        }

        return ResponseEntity.ok(offerService.getAllWithFilters(status, createdBy, keyword, token,
                pageable));
    }

    @GetMapping("/simple")
    @ApiMessage("Lấy danh sách offer tiền lương đơn giản")
    public ResponseEntity<List<Offer>> getAllSimple(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "candidateId", required = false) Long candidateId,
            @RequestParam(name = "workflowId", required = false) Long workflowId,
            @RequestParam(name = "ownerUserId", required = false) Long ownerUserId,
            @RequestParam(name = "minSalary", required = false) Long minSalary,
            @RequestParam(name = "maxSalary", required = false) Long maxSalary,
            @RequestParam(name = "onboardingDateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onboardingDateFrom,
            @RequestParam(name = "onboardingDateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onboardingDateTo,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(offerService.findAllWithFilters(status, candidateId,
                workflowId, ownerUserId, minSalary, maxSalary, onboardingDateFrom, onboardingDateTo,
                keyword));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật offer tiền lương")
    public ResponseEntity<Offer> update(@PathVariable Long id,
            @Validated @RequestBody UpdateOfferDTO dto) throws IdInvalidException {
        return ResponseEntity.ok(offerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa offer tiền lương")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        offerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
