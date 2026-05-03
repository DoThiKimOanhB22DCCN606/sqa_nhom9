package com.example.user_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.role.CreateRoleDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.model.Role;
import com.example.user_service.service.RoleService;
import com.example.user_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/user-service/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách tất cả vai trò")
    public ResponseEntity<PaginationDTO> getAll(
            @RequestParam(name = "isActive", required = false) Boolean isActive,
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
        return ResponseEntity.ok(roleService.getAllWithFilters(isActive, keyword, pageable));
    }

    @PostMapping
    @ApiMessage("Tạo mới vai trò")
    public ResponseEntity<Role> create(@RequestBody CreateRoleDTO createRoleDTO) throws IdInvalidException {

        if (roleService.existsByName(createRoleDTO.getName())) {
            throw new IdInvalidException("Tên vai trò đã tồn tại");
        }
        return ResponseEntity.ok(roleService.create(createRoleDTO));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật vai trò")
    public ResponseEntity<Role> update(@PathVariable Long id, @RequestBody CreateRoleDTO createRoleDTO) {
        return ResponseEntity.ok(roleService.update(id, createRoleDTO));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa vai trò")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        if (this.roleService.getById(id) == null) {
            throw new IdInvalidException("Vai trò  với id " + id + " không tồn tại");
        }
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy vai trò theo ID")
    public ResponseEntity<Role> findById(@PathVariable Long id) {
        Role role = this.roleService.getById(id);
        if (role == null) {
            throw new CustomException("Vai trò với id " + id + " không tồn tại");
        }
        return ResponseEntity.ok(role);
    }

    @GetMapping(params = "ids")
    @ApiMessage("Lấy danh sách nhân viên theo IDs")
    public ResponseEntity<List<Role>> findByIds(@RequestParam("ids") String ids) {
        List<Long> roleIds = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.roleService.getByIds(roleIds));
    }
}
