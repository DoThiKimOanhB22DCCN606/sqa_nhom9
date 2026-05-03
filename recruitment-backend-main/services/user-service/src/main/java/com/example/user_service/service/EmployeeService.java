package com.example.user_service.service;

import java.util.List;

import com.example.user_service.model.Position;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.employee.CreateEmployeeDTO;
import com.example.user_service.dto.employee.CreateEmployeeFromCandidateDTO;
import com.example.user_service.dto.employee.UpdateEmployeeDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Department;
import com.example.user_service.model.Employee;
import com.example.user_service.repository.EmployeeRepository;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final CloudinaryService cloudinaryService;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService,
            PositionService positionService, CloudinaryService cloudinaryService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
        this.positionService = positionService;
        this.cloudinaryService = cloudinaryService;
    }

    public PaginationDTO getAll(Pageable pageable) {
        Page<Employee> pageEmployee = this.employeeRepository.findAll(pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageEmployee.getNumber() + 1);
        mt.setPageSize(pageEmployee.getSize());
        mt.setPages(pageEmployee.getTotalPages());
        mt.setTotal(pageEmployee.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pageEmployee.getContent());
        return rs;
    }

    public Employee create(CreateEmployeeDTO createEmployeeDTO) {
        Employee employee = new Employee();
        employee.setName(createEmployeeDTO.getName());
        employee.setPhone(createEmployeeDTO.getPhone());
        employee.setEmail(createEmployeeDTO.getEmail());
        employee.setGender(createEmployeeDTO.getGender());
        employee.setAddress(createEmployeeDTO.getAddress());
        employee.setNationality(createEmployeeDTO.getNationality());
        employee.setDateOfBirth(createEmployeeDTO.getDateOfBirth());
        employee.setIdNumber(createEmployeeDTO.getIdNumber());
        employee.setStatus(createEmployeeDTO.getStatus() != null ? createEmployeeDTO.getStatus() : "ACTIVE");

        if (createEmployeeDTO.getDepartmentId() != null) {
            Department department = this.departmentService.getById(createEmployeeDTO.getDepartmentId());
            if (department == null) {
                throw new CustomException("Phòng ban không tồn tại");
            }
            employee.setDepartment(department);
        }

        if (createEmployeeDTO.getPositionId() != null) {
            Position position = this.positionService.getById(createEmployeeDTO.getPositionId());
            if (position == null) {
                throw new CustomException("Vị trí không tồn tại");
            }
            employee.setPosition(position);
        }

        return this.employeeRepository.save(employee);
    }

    public Employee update(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
        Employee employee = this.getById(id);

        if (updateEmployeeDTO.getName() != null) {
            employee.setName(updateEmployeeDTO.getName());
        }
        if (updateEmployeeDTO.getPhone() != null) {
            employee.setPhone(updateEmployeeDTO.getPhone());
        }
        if (updateEmployeeDTO.getEmail() != null) {
            employee.setEmail(updateEmployeeDTO.getEmail());
        }
        if (updateEmployeeDTO.getGender() != null) {
            employee.setGender(updateEmployeeDTO.getGender());
        }
        if (updateEmployeeDTO.getAddress() != null) {
            employee.setAddress(updateEmployeeDTO.getAddress());
        }
        if (updateEmployeeDTO.getNationality() != null) {
            employee.setNationality(updateEmployeeDTO.getNationality());
        }
        if (updateEmployeeDTO.getDateOfBirth() != null) {
            employee.setDateOfBirth(updateEmployeeDTO.getDateOfBirth());
        }
        if (updateEmployeeDTO.getIdNumber() != null) {
            employee.setIdNumber(updateEmployeeDTO.getIdNumber());
        }
        if (updateEmployeeDTO.getStatus() != null) {
            employee.setStatus(updateEmployeeDTO.getStatus());
        }
        if (updateEmployeeDTO.getDepartmentId() != null) {
            employee.setDepartment(this.departmentService.getById(updateEmployeeDTO.getDepartmentId()));
        }
        if (updateEmployeeDTO.getPositionId() != null) {
            employee.setPosition(this.positionService.getById(updateEmployeeDTO.getPositionId()));
        }

        return this.employeeRepository.save(employee);
    }

    public void delete(Long id) {
        this.employeeRepository.deleteById(id);
    }

    public Employee getById(Long id) {
        return this.employeeRepository.findById(id)
                .orElse(null);
    }

    public PaginationDTO getAllWithFilters(Long departmentId, Long positionId, String status, String keyword,
            Pageable pageable) {
        if (positionId != null && positionId == 1L && departmentId != null) {
            departmentId = 1L;
        }
        Page<Employee> pageEmployee = this.employeeRepository.findByFilters(departmentId, positionId, status, keyword,
                pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageEmployee.getNumber() + 1);
        mt.setPageSize(pageEmployee.getSize());
        mt.setPages(pageEmployee.getTotalPages());
        mt.setTotal(pageEmployee.getTotalElements());
        rs.setMeta(mt);
        rs.setResult(pageEmployee.getContent());
        return rs;
    }

    public List<Employee> getByIds(List<Long> ids) {
        return this.employeeRepository.findAllById(ids);
    }

    public List<Employee> getByDepartmentIds(List<Long> departmentIds) {
        return this.employeeRepository.findByDepartmentIds(departmentIds);
    }

    public String uploadAvatar(MultipartFile file) {
        return this.cloudinaryService.uploadFile(file);
    }

    /**
     * Tạo Employee từ Candidate
     * Mapping thông tin từ Candidate sang Employee
     */
    public Employee createFromCandidate(CreateEmployeeFromCandidateDTO dto) {
        Employee employee = new Employee();

        // Mapping các trường cơ bản từ Candidate
        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setPhone(dto.getPhone());
        employee.setGender(dto.getGender());
        employee.setNationality(dto.getNationality());
        employee.setIdNumber(dto.getIdNumber());
        employee.setAddress(dto.getAddress());
        employee.setAvatarUrl(dto.getAvatarUrl());

        // Parse dateOfBirth từ String sang LocalDate
        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isEmpty()) {
            try {
                // Thử parse các format phổ biến
                LocalDate parsedDate = null;
                String[] formats = {
                        "yyyy-MM-dd",
                        "dd/MM/yyyy",
                        "dd-MM-yyyy",
                        "yyyy/MM/dd"
                };

                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                        parsedDate = LocalDate.parse(dto.getDateOfBirth(), formatter);
                        break;
                    } catch (DateTimeParseException e) {
                        // Thử format tiếp theo
                    }
                }

                if (parsedDate == null) {
                    // Nếu không parse được, thử ISO format
                    parsedDate = LocalDate.parse(dto.getDateOfBirth());
                }

                employee.setDateOfBirth(parsedDate);
            } catch (Exception e) {
                throw new CustomException("Định dạng ngày sinh không hợp lệ: " + dto.getDateOfBirth());
            }
        }

        // Set candidateId để track
        employee.setCandidateId(dto.getCandidateId());

        // Set status (mặc định PROBATION khi chuyển từ candidate)
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : "PROBATION");

        // Set Department
        if (dto.getDepartmentId() != null) {
            Department department = this.departmentService.getById(dto.getDepartmentId());
            if (department == null) {
                throw new CustomException("Phòng ban không tồn tại");
            }
            employee.setDepartment(department);
        }

        // Set Position
        if (dto.getPositionId() != null) {
            Position position = this.positionService.getById(dto.getPositionId());
            if (position == null) {
                throw new CustomException("Vị trí không tồn tại");
            }
            employee.setPosition(position);
        }

        return this.employeeRepository.save(employee);
    }
}
