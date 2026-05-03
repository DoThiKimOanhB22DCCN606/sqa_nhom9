package com.example.user_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.model.Permission;
import com.example.user_service.service.PermissionService;
import com.example.user_service.utils.annotation.ApiMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user-service/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách tất cả quyền")
    public ResponseEntity<?> getAll(
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {

        // Nếu không truyền page và limit thì lấy tất cả không phân trang
        if (page == null || limit == null) {
            return ResponseEntity.ok(permissionService.getAllWithFiltersNoPage(isActive, keyword));
        }

        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        return ResponseEntity.ok(permissionService.getAllWithFilters(isActive, keyword, pageable));
    }

    @PostMapping
    @ApiMessage("Tạo quyền")
    public ResponseEntity<Permission> create(@Valid @RequestBody Permission p) throws IdInvalidException {
        if (permissionService.isPermissionExistsByName(p.getName())) {
            throw new IdInvalidException("Quyền đã tồn tại");

        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.permissionService.create(p));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật quyền")
    public ResponseEntity<Permission> update(@PathVariable Long id, @Valid @RequestBody Permission p)
            throws IdInvalidException {

        if (permissionService.findById(id) == null) {
            throw new IdInvalidException("Quyền  với id " + id + " không tồn tại");
        }

        if (permissionService.isPermissionExistsByName(p.getName())) {
            throw new IdInvalidException("Quyền đã tồn tại");
        }

        return ResponseEntity.status(HttpStatus.OK).body(this.permissionService.update(id, p));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa quyền")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        if (permissionService.findById(id) == null) {
            throw new IdInvalidException("Quyền  với id " + id + " không tồn tại");
        }
        this.permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // @PostMapping("/check")
    // @ApiMessage("Kiểm tra quyền")
    // public ResponseEntity<Boolean> check(@Valid @RequestBody
    // PermissionCheckRequest request) {
    // boolean allowed = permissionService.check(request.getName());
    // return ResponseEntity.ok(allowed);
    // }

    // @PostMapping("/batch-check")
    // @ApiMessage("Kiểm tra nhiều quyền cùng lúc")
    // public ResponseEntity<List<Boolean>> batchCheck(@Valid @RequestBody
    // BatchPermissionCheckRequest request) {
    // return ResponseEntity.ok(permissionService.batchCheck(request));
    // }
}
