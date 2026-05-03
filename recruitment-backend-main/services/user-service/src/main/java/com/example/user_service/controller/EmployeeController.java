package com.example.user_service.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.employee.CreateEmployeeDTO;
import com.example.user_service.dto.employee.CreateEmployeeFromCandidateDTO;
import com.example.user_service.dto.employee.UpdateEmployeeDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Employee;
import com.example.user_service.service.DepartmentService;
import com.example.user_service.service.EmployeeService;
import com.example.user_service.service.PositionService;
import com.example.user_service.utils.annotation.ApiMessage;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user-service/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final PositionService positionService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService employeeService, PositionService positionService,
            DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.positionService = positionService;
        this.departmentService = departmentService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách nhân viên")
    public ResponseEntity<PaginationDTO> findAll(
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "positionId", required = false) Long positionId,
            @RequestParam(name = "status", required = false) String status,
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

        return ResponseEntity.status(HttpStatus.OK)
                .body(this.employeeService.getAllWithFilters(departmentId, positionId, status, keyword, pageable));
    }

    @PostMapping
    @ApiMessage("Tạo mới nhân viên")
    public ResponseEntity<Employee> create(@Valid @RequestBody CreateEmployeeDTO createEmployeeDTO) {
        Employee employee = this.employeeService.create(createEmployeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật nhân viên")
    public ResponseEntity<Employee> update(@PathVariable Long id,
            @RequestBody UpdateEmployeeDTO updateEmployeeDTO) {
        Employee employee = this.employeeService.update(id, updateEmployeeDTO);
        return ResponseEntity.ok(employee);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa nhân viên")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy nhân viên theo ID")
    public ResponseEntity<Employee> findById(@PathVariable Long id) {
        Employee employee = this.employeeService.getById(id);
        return ResponseEntity.ok(employee);
    }

    @GetMapping(params = "ids")
    @ApiMessage("Lấy danh sách nhân viên theo IDs")
    public ResponseEntity<List<Employee>> findByIds(@RequestParam("ids") String ids) {
        List<Long> employeeIds = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.employeeService.getByIds(employeeIds));
    }

    @GetMapping(params = "departmentIds")
    @ApiMessage("Lấy danh sách nhân viên theo departmentIds")
    public ResponseEntity<List<Employee>> findByDepartmentIds(@RequestParam("departmentIds") String departmentIds) {
        List<Long> departmentIdsList = List.of(departmentIds.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(this.employeeService.getByDepartmentIds(departmentIdsList));
    }

    @PostMapping("/upload-avatar")
    @ApiMessage("Tải lên ảnh đại diện")
    public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = this.employeeService.uploadAvatar(file);
        return ResponseEntity.ok(avatarUrl);
    }

    @PostMapping("/from-candidate")
    @ApiMessage("Tạo nhân viên từ ứng viên")
    public ResponseEntity<Employee> createFromCandidate(@Valid @RequestBody CreateEmployeeFromCandidateDTO dto) {
        Employee employee = this.employeeService.createFromCandidate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }
}
