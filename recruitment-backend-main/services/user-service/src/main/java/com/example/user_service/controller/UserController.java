package com.example.user_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.example.user_service.dto.user.CreateUserDTO;
import com.example.user_service.dto.user.UpdateUserDTO;
import com.example.user_service.dto.user.UserDTO;
import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/user-service/users")
public class UserController {
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // Unified GET endpoint for all users with filtering, pagination, and sorting
    @GetMapping
    @ApiMessage("Lấy danh sách tất cả người dùng")
    public ResponseEntity<PaginationDTO> findAll(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
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

        return ResponseEntity.status(HttpStatus.OK).body(this.userService.getAllWithFilters(
                departmentId, role, isActive, keyword, pageable));
    }

    @PostMapping
    @ApiMessage("Tạo mới người dùng")
    public ResponseEntity<UserDTO> create(@RequestBody CreateUserDTO u) {
        String hashPassword = this.passwordEncoder.encode(u.getPassword());
        u.setPassword(hashPassword);
        UserDTO user = this.userService.create(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật người dùng")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @RequestBody UpdateUserDTO updateUserDTO) {
        UserDTO user = this.userService.update(id, updateUserDTO);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa người dùng")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id >= 100) {
            throw new IdInvalidException("Id khong vuot qua 100");
        }
        this.userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy người dùng theo ID")
    public ResponseEntity<UserDTO> findById(@PathVariable Long id) {
        UserDTO user = this.userService.getByIdAsDTO(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping(params = "ids")
    @ApiMessage("Lấy danh sách người dùng theo IDs")
    public ResponseEntity<List<UserDTO>> findByIds(@RequestParam("ids") String ids) {
        List<Long> userIds = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.userService.getByIds(userIds));
    }

    @GetMapping(params = "departmentIds")
    @ApiMessage("Lấy danh sách người dùng theo departmentIds")
    public ResponseEntity<List<UserDTO>> findByDepartmentIds(@RequestParam("departmentIds") String departmentIds) {
        List<Long> departmentIdsList = List.of(departmentIds.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.userService.getByDepartmentIds(departmentIdsList));
    }
}
