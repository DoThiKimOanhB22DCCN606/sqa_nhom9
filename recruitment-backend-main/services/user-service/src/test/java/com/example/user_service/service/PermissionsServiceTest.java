package com.example.user_service.service;

import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Permission;
import com.example.user_service.model.Role;
import com.example.user_service.repository.PermissionRepository;
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
 * Unit Test cho PermissionService.java / PermissionsService.java
 *
 * Đúng theo docs: 10 test case
 * TC-PERM-SER-001 -> TC-PERM-SER-010
 */
@ExtendWith(MockitoExtension.class)
class PermissionsServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    @DisplayName("TC-PERM-SER-001 - getAll - Danh sách có dữ liệu")
    void TC_PERM_SER_001() {
        Pageable pageable = PageRequest.of(0, 10);
        Permission permission = permission(1L, "USER_READ");

        when(permissionRepository.findByFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(permission), pageable, 1));

        var result = permissionService.getAllWithFilters(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
        assertEquals(1, ((List<?>) result.getResult()).size());

        verify(permissionRepository, times(1))
                .findByFilters(null, null, pageable);
    }

    @Test
    @DisplayName("TC-PERM-SER-002 - getAll - Danh sách rỗng")
    void TC_PERM_SER_002() {
        Pageable pageable = PageRequest.of(0, 10);

        when(permissionRepository.findByFilters(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = permissionService.getAllWithFilters(null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());

        verify(permissionRepository, times(1))
                .findByFilters(null, null, pageable);
    }

    @Test
    @DisplayName("TC-PERM-SER-003 - findById - ID tồn tại")
    void TC_PERM_SER_003() {
        Permission permission = permission(1L, "USER_VIEW");

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        Permission result = permissionService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("USER_VIEW", result.getName());

        verify(permissionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-PERM-SER-004 - findById - ID không tồn tại")
    void TC_PERM_SER_004() {
        when(permissionRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> permissionService.findById(999L));

        verify(permissionRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-PERM-SER-005 - create - Tạo permission hợp lệ")
    void TC_PERM_SER_005() {
        Permission request = permission(null, "USER_CREATE");
        request.setActive(false);

        when(permissionRepository.existsByName("USER_CREATE"))
                .thenReturn(false);

        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> {
                    Permission saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        Permission result = permissionService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("USER_CREATE", result.getName());
        assertTrue(result.isActive());

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository, times(1)).save(captor.capture());

        Permission saved = captor.getValue();
        assertEquals("USER_CREATE", saved.getName());
        assertTrue(saved.isActive());
    }

    @Test
    @DisplayName("TC-PERM-SER-006 - create - Code/name permission bị trùng")
    void TC_PERM_SER_006() {
        Permission request = permission(null, "USER_CREATE");

        when(permissionRepository.existsByName("USER_CREATE"))
                .thenReturn(true);

        assertThrows(CustomException.class,
                () -> permissionService.create(request));

        verify(permissionRepository, times(1)).existsByName("USER_CREATE");
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("TC-PERM-SER-007 - update - Cập nhật permission hợp lệ")
    void TC_PERM_SER_007() {
        Permission stored = permission(1L, "USER_READ");
        Permission request = permission(null, "USER_VIEW");
        request.setActive(true);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(stored));

        when(permissionRepository.save(stored))
                .thenReturn(stored);

        Permission result = permissionService.update(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("USER_VIEW", result.getName());
        assertTrue(result.isActive());

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).save(stored);
    }

    @Test
    @DisplayName("TC-PERM-SER-008 - update - ID không tồn tại")
    void TC_PERM_SER_008() {
        Permission request = permission(null, "USER_UPDATE");

        when(permissionRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> permissionService.update(999L, request));

        verify(permissionRepository, times(1)).findById(999L);
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("TC-PERM-SER-009 - delete - Permission chưa gán vào role")
    void TC_PERM_SER_009() {
        Permission permission = permission(1L, "USER_DELETE");
        permission.setRoles(new HashSet<>());

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        permissionService.delete(1L);

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).delete(permission);
    }

    @Test
    @DisplayName("TC-PERM-SER-010 - delete - Permission đang được role sử dụng")
    void TC_PERM_SER_010() {
        Permission permission = permission(1L, "USER_DELETE");

        Role role = new Role();
        role.setId(10L);
        role.setName("ADMIN");
        role.setPermissions(new HashSet<>(List.of(permission)));

        permission.setRoles(new HashSet<>(List.of(role)));

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        assertThrows(CustomException.class,
                () -> permissionService.delete(1L));

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, never()).delete(any(Permission.class));
    }

    private static Permission permission(Long id, String name) {
        Permission permission = new Permission();

        if (id != null) {
            permission.setId(id);
        }

        permission.setName(name);
        permission.setActive(true);
        permission.setRoles(new HashSet<>());

        return permission;
    }
}