package com.example.user_service.service;

import com.example.user_service.dto.role.CreateRoleDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Permission;
import com.example.user_service.model.Role;
import com.example.user_service.model.User;
import com.example.user_service.repository.RoleRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RoleService.java / RolesService.java
 *
 * Đúng theo docs: 18 test case
 * TC-ROLE-SER-001 -> TC-ROLE-SER-018
 */
@ExtendWith(MockitoExtension.class)
class RolesServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleService roleService;

    private final Pageable pageable = PageRequest.of(0, 5);

    @Test
    @DisplayName("TC-ROLE-SER-001 - getAll - Danh sách có dữ liệu")
    void TC_ROLE_SER_001() {
        Role role = role(1L, "ADMIN");

        when(roleRepository.findByFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(role), pageable, 1));

        var result = roleService.getAllWithFilters(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getPage());
        assertEquals(5, result.getMeta().getPageSize());
        assertEquals(1, result.getMeta().getTotal());
        assertEquals(1, ((List<?>) result.getResult()).size());

        verify(roleRepository, times(1))
                .findByFilters(eq(null), eq(null), eq(pageable));
    }

    @Test
    @DisplayName("TC-ROLE-SER-002 - getById - ID tồn tại")
    void TC_ROLE_SER_002() {
        Role role = role(1L, "HR");
        Permission permission = permission(1L, "USER_VIEW");
        role.setPermissions(new HashSet<>(Set.of(permission)));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        Role result = roleService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("HR", result.getName());
        assertEquals(1, result.getPermissions().size());

        verify(roleRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-ROLE-SER-003 - getById - ID không tồn tại")
    void TC_ROLE_SER_003() {
        when(roleRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> roleService.getById(999L));

        verify(roleRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-ROLE-SER-004 - create - Tạo role hợp lệ không có permission")
    void TC_ROLE_SER_004() {
        CreateRoleDTO request = roleDto("INTERVIEWER", "Người phỏng vấn", List.of());

        when(permissionService.findByIds(List.of()))
                .thenReturn(List.of());

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> {
                    Role saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        Role result = roleService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INTERVIEWER", result.getName());
        assertEquals("Người phỏng vấn", result.getDescription());
        assertTrue(result.getPermissions().isEmpty());

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();
        assertEquals("INTERVIEWER", saved.getName());
        assertTrue(saved.getPermissions().isEmpty());

        verify(permissionService, times(1)).findByIds(List.of());
    }

    @Test
    @DisplayName("TC-ROLE-SER-005 - create - Tạo role hợp lệ có permission")
    void TC_ROLE_SER_005() {
        Permission p1 = permission(1L, "USER_CREATE");
        Permission p2 = permission(2L, "USER_READ");
        Permission p3 = permission(3L, "USER_UPDATE");

        CreateRoleDTO request = roleDto("HR", "Phòng nhân sự", List.of(1L, 2L, 3L));

        when(permissionService.findByIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(p1, p2, p3));

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> {
                    Role saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        Role result = roleService.create(request);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("HR", result.getName());
        assertEquals(3, result.getPermissions().size());

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();
        assertEquals("HR", saved.getName());
        assertEquals(3, saved.getPermissions().size());

        verify(permissionService, times(1)).findByIds(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("TC-ROLE-SER-006 - create - Tên role bị trùng")
    void TC_ROLE_SER_006() {
        CreateRoleDTO request = roleDto("ADMIN", "Role bị trùng", List.of());

        lenient().when(permissionService.findByIds(List.of()))
                .thenReturn(List.of());

        lenient().when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(CustomException.class,
                () -> roleService.create(request));

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("TC-ROLE-SER-007 - create - PermissionId không tồn tại")
    void TC_ROLE_SER_007() {
        CreateRoleDTO request = roleDto("ROLE_X", "Role có permission lỗi", List.of(1L, 999L));

        Permission validPermission = permission(1L, "USER_VIEW");

        when(permissionService.findByIds(List.of(1L, 999L)))
                .thenReturn(List.of(validPermission));

        lenient().when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(CustomException.class,
                () -> roleService.create(request));

        verify(permissionService, times(1)).findByIds(List.of(1L, 999L));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("TC-ROLE-SER-008 - update - Cập nhật tên role hợp lệ")
    void TC_ROLE_SER_008() {
        Role existing = role(1L, "OLD_ROLE");

        CreateRoleDTO request = roleDto("HR_MANAGER", "Mô tả mới", null);

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Role result = roleService.update(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("HR_MANAGER", result.getName());
        assertEquals("Mô tả mới", result.getDescription());

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();
        assertEquals("HR_MANAGER", saved.getName());
        assertEquals("Mô tả mới", saved.getDescription());

        verify(roleRepository, times(1)).findById(1L);
        verify(permissionService, never()).findByIds(any());
    }

    @Test
    @DisplayName("TC-ROLE-SER-009 - update - Role không tồn tại")
    void TC_ROLE_SER_009() {
        CreateRoleDTO request = roleDto("ROLE_NOT_FOUND", "Không tồn tại", null);

        when(roleRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> roleService.update(999L, request));

        verify(roleRepository, times(1)).findById(999L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("TC-ROLE-SER-010 - update - Tên mới trùng role khác")
    void TC_ROLE_SER_010() {
        Role existing = role(1L, "HR");

        CreateRoleDTO request = roleDto("ADMIN", "Tên bị trùng", null);

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        lenient().when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(CustomException.class,
                () -> roleService.update(1L, request));

        verify(roleRepository, times(1)).findById(1L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("TC-ROLE-SER-011 - updatePermissions - Gán danh sách quyền hợp lệ")
    void TC_ROLE_SER_011() {
        Role existing = role(1L, "HR");

        Permission p1 = permission(10L, "USER_CREATE");
        Permission p2 = permission(11L, "USER_UPDATE");

        CreateRoleDTO request = roleDto("HR", "Mô tả", List.of(10L, 11L));

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(permissionService.findByIds(List.of(10L, 11L)))
                .thenReturn(List.of(p1, p2));

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Role result = roleService.update(1L, request);

        assertNotNull(result);
        assertEquals(2, result.getPermissions().size());

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();
        assertEquals(2, saved.getPermissions().size());

        verify(permissionService, times(1)).findByIds(List.of(10L, 11L));
    }

    @Test
    @DisplayName("TC-ROLE-SER-012 - updatePermissions - Danh sách quyền rỗng")
    void TC_ROLE_SER_012() {
        Role existing = role(1L, "HR");
        existing.setPermissions(new HashSet<>(List.of(permission(1L, "OLD_PERMISSION"))));

        CreateRoleDTO request = roleDto("HR", "Mô tả", List.of());

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(permissionService.findByIds(List.of()))
                .thenReturn(List.of());

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Role result = roleService.update(1L, request);

        assertNotNull(result);
        assertTrue(result.getPermissions().isEmpty());

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        assertTrue(captor.getValue().getPermissions().isEmpty());

        verify(permissionService, times(1)).findByIds(List.of());
    }

    @Test
    @DisplayName("TC-ROLE-SER-013 - updatePermissions - Có permission không tồn tại")
    void TC_ROLE_SER_013() {
        Role existing = role(1L, "HR");

        CreateRoleDTO request = roleDto("HR", "Mô tả", List.of(1L, 999L));

        Permission validPermission = permission(1L, "USER_VIEW");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(permissionService.findByIds(List.of(1L, 999L)))
                .thenReturn(List.of(validPermission));

        lenient().when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(CustomException.class,
                () -> roleService.update(1L, request));

        verify(roleRepository, times(1)).findById(1L);
        verify(permissionService, times(1)).findByIds(List.of(1L, 999L));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("TC-ROLE-SER-014 - hasPermission - Role có quyền yêu cầu")
    void TC_ROLE_SER_014() {
        Permission permission = permission(1L, "USER_CREATE");

        CreateRoleDTO request = roleDto("ROLE_P", "Có quyền", List.of(1L));

        when(permissionService.findByIds(List.of(1L)))
                .thenReturn(List.of(permission));

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        roleService.create(request);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();

        assertTrue(saved.getPermissions()
                .stream()
                .anyMatch(item -> "USER_CREATE".equals(item.getName())));

        verify(permissionService, times(1)).findByIds(List.of(1L));
    }

    @Test
    @DisplayName("TC-ROLE-SER-015 - hasPermission - Role không có quyền yêu cầu")
    void TC_ROLE_SER_015() {
        CreateRoleDTO request = roleDto("ROLE_NOPERM", "Không có quyền", List.of());

        when(permissionService.findByIds(List.of()))
                .thenReturn(List.of());

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        roleService.create(request);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(1)).save(captor.capture());

        Role saved = captor.getValue();

        assertTrue(saved.getPermissions()
                .stream()
                .noneMatch(item -> "USER_DELETE".equals(item.getName())));

        verify(permissionService, times(1)).findByIds(List.of());
    }

    @Test
    @DisplayName("TC-ROLE-SER-016 - delete - Role chưa được user sử dụng")
    void TC_ROLE_SER_016() {
        Role role = role(3L, "INTERVIEWER");
        role.setUsers(new HashSet<>());

        lenient().when(roleRepository.findById(3L))
                .thenReturn(Optional.of(role));

        roleService.delete(3L);

        verify(roleRepository, times(1)).deleteById(3L);
    }

    @Test
    @DisplayName("TC-ROLE-SER-017 - delete - Role đang được user sử dụng")
    void TC_ROLE_SER_017() {
        Role role = role(1L, "HR");

        User user = new User();
        user.setId(10L);

        role.setUsers(new HashSet<>(List.of(user)));

        lenient().when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(CustomException.class,
                () -> roleService.delete(1L));

        verify(roleRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("TC-ROLE-SER-018 - delete - Không cho xóa role hệ thống ADMIN")
    void TC_ROLE_SER_018() {
        Role role = role(1L, "ADMIN");
        role.setUsers(new HashSet<>());

        lenient().when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        assertThrows(CustomException.class,
                () -> roleService.delete(1L));

        verify(roleRepository, never()).deleteById(1L);
    }

    private static Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription("desc");
        role.setPermissions(new HashSet<>());
        role.setUsers(new HashSet<>());
        return role;
    }

    private static Permission permission(Long id, String name) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setName(name);
        permission.setRoles(new HashSet<>());
        permission.setActive(true);
        return permission;
    }

    private static CreateRoleDTO roleDto(String name, String description, List<Long> permissionIds) {
        CreateRoleDTO dto = new CreateRoleDTO();
        dto.setName(name);
        dto.setDescription(description != null ? description : "desc");
        dto.setPermissionIds(permissionIds);
        return dto;
    }
}