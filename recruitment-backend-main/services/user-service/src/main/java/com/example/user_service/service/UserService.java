package com.example.user_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.user.CreateUserDTO;
import com.example.user_service.dto.user.UpdateUserDTO;
import com.example.user_service.dto.user.UserDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Employee;
import com.example.user_service.model.Role;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final EmployeeService employeeService;

    public UserService(UserRepository userRepository, RoleService roleService, EmployeeService employeeService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.employeeService = employeeService;
    }

    public PaginationDTO getAll(Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageUser.getNumber() + 1);
        mt.setPageSize(pageUser.getSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageUser.getContent());
        return rs;
    }

    public UserDTO create(CreateUserDTO createUserDTO) {
        User user = new User();
        user.setEmail(createUserDTO.getEmail());
        user.setPassword(createUserDTO.getPassword());
        Role role = this.roleService.getById(createUserDTO.getRoleId());
        if (role == null) {
            throw new CustomException("Vai trò không tồn tại");
        }
        user.setRole(role);
        user.set_active(true);

        // Link với Employee đã tồn tại
        Employee employee = this.employeeService.getById(createUserDTO.getEmployeeId());
        if (employee == null) {
            throw new CustomException("Nhân viên không tồn tại");
        }
        if (employee.getUser() != null) {
            throw new CustomException("Nhân viên đã có tài khoản");
        }
        user.setEmployee(employee);
        employee.setUser(user);

        User savedUser = this.userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO update(Long id, UpdateUserDTO updateUserDTO) {

        User user = this.getById(id);
        if (user == null) {
            throw new CustomException("Người dùng không tồn tại");
        }
        if (updateUserDTO.getEmail() != null) {
            user.setEmail(updateUserDTO.getEmail());
        }
        if (updateUserDTO.getPassword() != null) {
            user.setPassword(updateUserDTO.getPassword());
        }
        if (updateUserDTO.getRoleId() != null) {
            Role role = this.roleService.getById(updateUserDTO.getRoleId());
            if (role == null) {
                throw new CustomException("Vai trò không tồn tại");
            }
            user.setRole(role);
        }
        if (updateUserDTO.getIsActive() != null) {
            user.set_active(updateUserDTO.getIsActive());
        }

        // Link với Employee đã tồn tại
        if (updateUserDTO.getEmployeeId() != null) {
            Employee employee = this.employeeService.getById(updateUserDTO.getEmployeeId());
            if (employee == null) {
                throw new CustomException("Nhân viên không tồn tại");
            }
            if (employee.getUser() != null) {
                throw new CustomException("Nhân viên đã có tài khoản");
            }
            user.setEmployee(employee);
            employee.setUser(user);
        }

        User updatedUser = this.userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public User update(User u) {
        return this.userRepository.save(u);
    }

    public void delete(Long id) {
        this.userRepository.deleteById(id);
        ;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public void updateUserRefreshToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    public User getById(Long id) {
        return this.userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserDTO getByIdAsDTO(Long id) {
        User user = this.getById(id);
        return convertToDTO(user);
    }

    public PaginationDTO getAllWithFilters(Long departmentId, String role, Boolean isActive, String keyword,
            Pageable pageable) {
        Page<User> pageUser = this.userRepository.findByFilters(departmentId, role, isActive, keyword, pageable);
        PaginationDTO rs = new PaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageUser.getNumber() + 1);
        mt.setPageSize(pageUser.getSize());
        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());
        rs.setMeta(mt);
        // Convert User entities to UserDTO
        List<UserDTO> userDTOs = pageUser.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        rs.setResult(userDTOs);
        return rs;
    }

    public List<UserDTO> getByIds(List<Long> ids) {
        List<User> users = this.userRepository.findAllById(ids);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getByDepartmentIds(List<Long> departmentIds) {
        List<User> users = this.userRepository.findByDepartmentIds(departmentIds);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Helper method to convert User entity to UserDTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setActive(user.is_active());
        dto.setCreateBy(user.getCreateBy());
        dto.setUpdateBy(user.getUpdateBy());
        dto.setRefreshToken(user.getRefreshToken());
        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getId());
        }
        dto.setEmployee(user.getEmployee());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
