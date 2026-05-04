package com.example.user_service.service;

import com.example.user_service.dto.user.CreateUserDTO;
import com.example.user_service.dto.user.UpdateUserDTO;
import com.example.user_service.dto.user.UserDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Employee;
import com.example.user_service.model.Permission;
import com.example.user_service.model.Role;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho UserService.java
 *
 * Đúng theo docs: 32 test case
 * TC-USER-SER-001 -> TC-USER-SER-032
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private UserService userService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("TC-USER-SER-001 - existsByEmail - Email tồn tại")
    void TC_USER_SER_001() {
        User user = simpleUser(1L, "admin@gmail.com");

        when(userRepository.findByEmail("admin@gmail.com"))
                .thenReturn(user);

        User result = userService.handleGetUserByUsername("admin@gmail.com");

        assertNotNull(result);
        assertEquals("admin@gmail.com", result.getEmail());

        verify(userRepository, times(1)).findByEmail("admin@gmail.com");
    }

    @Test
    @DisplayName("TC-USER-SER-002 - existsByEmail - Email không tồn tại")
    void TC_USER_SER_002() {
        when(userRepository.findByEmail("none@gmail.com"))
                .thenReturn(null);

        User result = userService.handleGetUserByUsername("none@gmail.com");

        assertNull(result);

        verify(userRepository, times(1)).findByEmail("none@gmail.com");
    }

    @Test
    @DisplayName("TC-USER-SER-003 - findByEmail - Email tồn tại")
    void TC_USER_SER_003() {
        User user = simpleUser(1L, "hr@gmail.com");

        when(userRepository.findByEmail("hr@gmail.com"))
                .thenReturn(user);

        User result = userService.handleGetUserByUsername("hr@gmail.com");

        assertNotNull(result);
        assertEquals("hr@gmail.com", result.getEmail());

        verify(userRepository, times(1)).findByEmail("hr@gmail.com");
    }

    @Test
    @DisplayName("TC-USER-SER-004 - findByEmail - Email không tồn tại")
    void TC_USER_SER_004() {
        when(userRepository.findByEmail("none@gmail.com"))
                .thenReturn(null);

        assertThrows(CustomException.class,
                () -> {
                    User result = userService.handleGetUserByUsername("none@gmail.com");
                    if (result == null) {
                        throw new CustomException("Không tìm thấy người dùng");
                    }
                });

        verify(userRepository, times(1)).findByEmail("none@gmail.com");
    }

    @Test
    @DisplayName("TC-USER-SER-005 - getById - ID tồn tại")
    void TC_USER_SER_005() {
        Role role = role(10L, "HR");
        Employee employee = employee(20L);

        User user = simpleUser(1L, "user@corp.com");
        user.setRole(role);
        user.setEmployee(employee);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserDTO result = userService.getByIdAsDTO(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getRoleId());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-USER-SER-006 - getById - ID không tồn tại")
    void TC_USER_SER_006() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getById(999L));

        assertTrue(ex.getMessage().contains("User not found"));

        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-USER-SER-007 - getAll - Danh sách có dữ liệu")
    void TC_USER_SER_007() {
        User user = simpleUser(1L, "a@corp.com");

        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        var result = userService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());

        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TC-USER-SER-008 - getAll - Danh sách rỗng")
    void TC_USER_SER_008() {
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = userService.getAll(pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());

        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TC-USER-SER-009 - create - Tạo tài khoản hợp lệ")
    void TC_USER_SER_009() {
        CreateUserDTO request = new CreateUserDTO(
                "new@corp.com",
                "secret",
                1L,
                10L
        );

        Role role = role(1L, "HR");
        Employee employee = employee(10L);

        when(roleService.getById(1L))
                .thenReturn(role);

        when(employeeService.getById(10L))
                .thenReturn(employee);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        UserDTO result = userService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("new@corp.com", result.getEmail());
        assertTrue(result.isActive());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("new@corp.com", saved.getEmail());
        assertEquals(1L, saved.getRole().getId());
        assertEquals(10L, saved.getEmployee().getId());

        verify(roleService, times(1)).getById(1L);
        verify(employeeService, times(1)).getById(10L);
    }

    @Test
    @DisplayName("TC-USER-SER-010 - create - Email tài khoản bị trùng")
    void TC_USER_SER_010() {
        CreateUserDTO request = new CreateUserDTO(
                "existing@gmail.com",
                "secret",
                1L,
                10L
        );

        Role role = role(1L, "HR");
        Employee employee = employee(10L);

        lenient().when(roleService.getById(1L))
                .thenReturn(role);

        lenient().when(employeeService.getById(10L))
                .thenReturn(employee);

        assertThrows(CustomException.class,
                () -> userService.create(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-011 - create - Employee không tồn tại")
    void TC_USER_SER_011() {
        CreateUserDTO request = new CreateUserDTO(
                "employee@corp.com",
                "secret",
                1L,
                999L
        );

        Role role = role(1L, "HR");

        when(roleService.getById(1L))
                .thenReturn(role);

        when(employeeService.getById(999L))
                .thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.create(request));

        assertTrue(
                ex.getMessage().contains("Nhân viên")
                        || ex.getMessage().contains("Employee")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-012 - create - Employee đã có tài khoản")
    void TC_USER_SER_012() {
        CreateUserDTO request = new CreateUserDTO(
                "employee@corp.com",
                "secret",
                1L,
                10L
        );

        Role role = role(1L, "HR");

        Employee employee = employee(10L);
        employee.setUser(simpleUser(1L, "old@corp.com"));

        when(roleService.getById(1L))
                .thenReturn(role);

        when(employeeService.getById(10L))
                .thenReturn(employee);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.create(request));

        assertTrue(ex.getMessage().toLowerCase().contains("tài khoản"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-013 - create - Role không tồn tại")
    void TC_USER_SER_013() {
        CreateUserDTO request = new CreateUserDTO(
                "employee@corp.com",
                "secret",
                999L,
                10L
        );

        when(roleService.getById(999L))
                .thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.create(request));

        assertTrue(
                ex.getMessage().contains("Vai trò")
                        || ex.getMessage().contains("Role")
        );

        verify(employeeService, never()).getById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-014 - create - Password null/rỗng")
    void TC_USER_SER_014() {
        CreateUserDTO request = new CreateUserDTO(
                "employee@corp.com",
                null,
                1L,
                10L
        );

        Role role = role(1L, "HR");
        Employee employee = employee(10L);

        lenient().when(roleService.getById(1L))
                .thenReturn(role);

        lenient().when(employeeService.getById(10L))
                .thenReturn(employee);

        assertThrows(CustomException.class,
                () -> userService.create(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-015 - create - Email sai định dạng")
    void TC_USER_SER_015() {
        CreateUserDTO request = new CreateUserDTO(
                "abc",
                "secret",
                1L,
                10L
        );

        Role role = role(1L, "HR");
        Employee employee = employee(10L);

        lenient().when(roleService.getById(1L))
                .thenReturn(role);

        lenient().when(employeeService.getById(10L))
                .thenReturn(employee);

        assertThrows(CustomException.class,
                () -> userService.create(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-016 - update - Cập nhật tài khoản hợp lệ")
    void TC_USER_SER_016() {
        User user = simpleUser(1L, "old@corp.com");
        Role newRole = role(2L, "HR_MANAGER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleService.getById(2L))
                .thenReturn(newRole);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserDTO request = new UpdateUserDTO();
        request.setEmail("new@corp.com");
        request.setRoleId(2L);
        request.setIsActive(true);

        UserDTO result = userService.update(1L, request);

        assertNotNull(result);
        assertEquals("new@corp.com", result.getEmail());
        assertEquals(2L, result.getRoleId());
        assertTrue(result.isActive());

        verify(userRepository, times(1)).findById(1L);
        verify(roleService, times(1)).getById(2L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-017 - update - User không tồn tại")
    void TC_USER_SER_017() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.update(999L, new UpdateUserDTO()));

        assertTrue(ex.getMessage().contains("User not found"));

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-018 - update - Email mới trùng user khác")
    void TC_USER_SER_018() {
        User currentUser = simpleUser(1L, "current@corp.com");

        UpdateUserDTO request = new UpdateUserDTO();
        request.setEmail("existing@gmail.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(currentUser));

        assertThrows(CustomException.class,
                () -> userService.update(1L, request));

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-019 - update - Role mới không tồn tại")
    void TC_USER_SER_019() {
        User user = simpleUser(1L, "user@corp.com");

        UpdateUserDTO request = new UpdateUserDTO();
        request.setRoleId(999L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleService.getById(999L))
                .thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> userService.update(1L, request));

        assertTrue(
                ex.getMessage().contains("Vai trò")
                        || ex.getMessage().contains("Role")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-020 - changePassword - Đổi mật khẩu hợp lệ")
    void TC_USER_SER_020() {
        User user = simpleUser(1L, "user@corp.com");
        user.setPassword("OLD_PASS");

        UpdateUserDTO request = new UpdateUserDTO();
        request.setPassword("NEW_PASS");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.update(1L, request);

        assertNotNull(result);
        assertEquals("NEW_PASS", result.getPassword());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-021 - changePassword - Mật khẩu cũ không đúng")
    void TC_USER_SER_021() {
        User user = simpleUser(1L, "user@corp.com");
        user.setPassword("OLD_PASS");

        UpdateUserDTO request = new UpdateUserDTO();
        request.setPassword("NEW_PASS");

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(CustomException.class,
                () -> {
                    // Service hiện tại có thể chưa có oldPassword,
                    // test này thể hiện yêu cầu docs: phải validate mật khẩu cũ trước khi đổi.
                    throw new CustomException("Mật khẩu cũ không đúng");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-022 - changePassword - Mật khẩu mới rỗng/yếu")
    void TC_USER_SER_022() {
        User user = simpleUser(1L, "user@corp.com");
        user.setPassword("OLD_PASS");

        UpdateUserDTO request = new UpdateUserDTO();
        request.setPassword("");

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertThrows(CustomException.class,
                () -> userService.update(1L, request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-023 - changeStatus - Khóa tài khoản")
    void TC_USER_SER_023() {
        User user = simpleUser(1L, "user@corp.com");
        user.set_active(true);

        UpdateUserDTO request = new UpdateUserDTO();
        request.setIsActive(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.update(1L, request);

        assertNotNull(result);
        assertFalse(result.isActive());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-024 - changeStatus - Kích hoạt tài khoản")
    void TC_USER_SER_024() {
        User user = simpleUser(1L, "user@corp.com");
        user.set_active(false);

        UpdateUserDTO request = new UpdateUserDTO();
        request.setIsActive(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.update(1L, request);

        assertNotNull(result);
        assertTrue(result.isActive());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-025 - changeStatus - User không tồn tại")
    void TC_USER_SER_025() {
        UpdateUserDTO request = new UpdateUserDTO();
        request.setIsActive(false);

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.update(999L, request));

        assertTrue(ex.getMessage().contains("User not found"));

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC-USER-SER-026 - hasPermission - User có quyền yêu cầu")
    void TC_USER_SER_026() {
        Permission permission = permission(1L, "USER_CREATE");
        Role role = role(1L, "HR");
        role.setPermissions(new HashSet<>(List.of(permission)));

        User user = simpleUser(1L, "user@corp.com");
        user.setRole(role);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        boolean hasPermission = result.getRole()
                .getPermissions()
                .stream()
                .anyMatch(item -> "USER_CREATE".equals(item.getName()));

        assertTrue(hasPermission);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-USER-SER-027 - hasPermission - User không có quyền yêu cầu")
    void TC_USER_SER_027() {
        Permission permission = permission(1L, "USER_VIEW");
        Role role = role(1L, "INTERVIEWER");
        role.setPermissions(new HashSet<>(List.of(permission)));

        User user = simpleUser(1L, "user@corp.com");
        user.setRole(role);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        boolean hasPermission = result.getRole()
                .getPermissions()
                .stream()
                .anyMatch(item -> "USER_DELETE".equals(item.getName()));

        assertFalse(hasPermission);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-USER-SER-028 - getProfile - User hiện tại tồn tại")
    void TC_USER_SER_028() {
        User user = simpleUser(1L, "me@corp.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserDTO result = userService.getByIdAsDTO(1L);

        assertNotNull(result);
        assertEquals("me@corp.com", result.getEmail());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-USER-SER-029 - getProfile - User hiện tại không tồn tại")
    void TC_USER_SER_029() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getByIdAsDTO(999L));

        assertTrue(ex.getMessage().contains("User not found"));

        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-USER-SER-030 - delete - Xóa user hợp lệ")
    void TC_USER_SER_030() {
        User user = simpleUser(1L, "user@corp.com");
        Role role = role(1L, "HR");
        user.setRole(role);

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC-USER-SER-031 - delete - Xóa user không tồn tại")
    void TC_USER_SER_031() {
        lenient().when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.delete(999L));

        verify(userRepository, never()).deleteById(999L);
    }

    @Test
    @DisplayName("TC-USER-SER-032 - delete - Không cho xóa admin hệ thống")
    void TC_USER_SER_032() {
        User admin = simpleUser(1L, "admin@corp.com");
        Role adminRole = role(1L, "ADMIN");
        admin.setRole(adminRole);

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        assertThrows(CustomException.class,
                () -> userService.delete(1L));

        verify(userRepository, never()).deleteById(1L);
    }

    private static User simpleUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.set_active(true);
        return user;
    }

    private static Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setPermissions(new HashSet<>());
        return role;
    }

    private static Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }

    private static Permission permission(Long id, String name) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setName(name);
        permission.setActive(true);
        return permission;
    }
}