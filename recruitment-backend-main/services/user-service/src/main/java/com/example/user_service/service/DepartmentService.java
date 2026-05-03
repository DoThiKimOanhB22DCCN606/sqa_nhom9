package com.example.user_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.department.CreateDepartmentDTO;
import com.example.user_service.dto.department.UpdateDepartmentDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Department;
import com.example.user_service.repository.DepartmentRepository;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public Department getById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Không tìm thấy phòng ban với ID: " + id));
    }

    public PaginationDTO getAll(Pageable pageable) {
        Page<Department> pageDepartment = this.departmentRepository.findAll(pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageDepartment.getNumber() + 1);
        mt.setPageSize(pageDepartment.getSize());

        mt.setPages(pageDepartment.getTotalPages());
        mt.setTotal(pageDepartment.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageDepartment.getContent());
        return rs;
    }

    public PaginationDTO getAllWithFilters(Boolean isActive, String keyword, Pageable pageable) {
        Page<Department> pageDepartment = this.departmentRepository.findByFilters(isActive, keyword, pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageDepartment.getNumber() + 1);
        mt.setPageSize(pageDepartment.getSize());
        mt.setPages(pageDepartment.getTotalPages());
        mt.setTotal(pageDepartment.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pageDepartment.getContent());
        return rs;
    }

    public List<Department> getByIds(List<Long> ids) {
        return departmentRepository.findAllById(ids);
    }

    public Department create(CreateDepartmentDTO createDepartmentDTO) {
        // Kiểm tra code đã tồn tại chưa
        if (departmentRepository.findByCode(createDepartmentDTO.getCode().toUpperCase()).isPresent()) {
            throw new CustomException("Mã phòng ban '" + createDepartmentDTO.getCode() + "' đã tồn tại");
        }

        Department department = new Department();
        department.setCode(createDepartmentDTO.getCode().toUpperCase());
        department.setName(createDepartmentDTO.getName());
        department.setDescription(createDepartmentDTO.getDescription());
        department.set_active(createDepartmentDTO.getIsActive() != null ? createDepartmentDTO.getIsActive() : true);

        return this.departmentRepository.save(department);
    }

    public Department update(Long id, UpdateDepartmentDTO updateDepartmentDTO) {
        Department department = this.getById(id);

        // Kiểm tra code mới có trùng với code của phòng ban khác không
        if (updateDepartmentDTO.getCode() != null && !updateDepartmentDTO.getCode().isEmpty()) {
            String newCode = updateDepartmentDTO.getCode().toUpperCase();
            departmentRepository.findByCode(newCode).ifPresent(existingDepartment -> {
                if (!existingDepartment.getId().equals(id)) {
                    throw new CustomException("Mã phòng ban '" + newCode + "' đã tồn tại");
                }
            });
            department.setCode(newCode);
        }

        if (updateDepartmentDTO.getName() != null) {
            department.setName(updateDepartmentDTO.getName());
        }
        if (updateDepartmentDTO.getDescription() != null) {
            department.setDescription(updateDepartmentDTO.getDescription());
        }
        if (updateDepartmentDTO.getIsActive() != null) {
            department.set_active(updateDepartmentDTO.getIsActive());
        }

        return this.departmentRepository.save(department);
    }

    public void delete(Long id) {
        Department department = this.getById(id);
        // Kiểm tra xem phòng ban có nhân viên không
        if (department.getEmployees() != null && !department.getEmployees().isEmpty()) {
            throw new CustomException("Không thể xóa phòng ban vì còn nhân viên trong phòng ban này");
        }
        this.departmentRepository.deleteById(id);
    }

}
