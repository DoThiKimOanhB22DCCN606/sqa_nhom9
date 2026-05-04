package com.example.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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

import com.example.user_service.dto.department.CreateDepartmentDTO;
import com.example.user_service.dto.department.UpdateDepartmentDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Department;
import com.example.user_service.model.Employee;
import com.example.user_service.repository.DepartmentRepository;

/** Unit tests theo tài liệu TC-DEPT-SER-* (ánh xạ hành vi {@link DepartmentService}). */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("TC-DEPT-SER-001 - getAll: danh sách có dữ liệu (3)")
    void TC_DEPT_SER_001() {
        Department a = dept(1L, "A");
        Department b = dept(2L, "B");
        Department c = dept(3L, "C");
        when(departmentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(a, b, c), pageable, 3));

        var rs = departmentService.getAll(pageable);

        verify(departmentRepository).findAll(eq(pageable));
        assertEquals(1, rs.getMeta().getPage());
        assertEquals(10, rs.getMeta().getPageSize());
        assertEquals(3, ((List<?>) rs.getResult()).size());
        assertEquals(3, rs.getMeta().getTotal());
    }

    @Test
    @DisplayName("TC-DEPT-SER-002 - getAll: danh sách rỗng")
    void TC_DEPT_SER_002() {
        when(departmentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var rs = departmentService.getAll(pageable);

        verify(departmentRepository).findAll(pageable);
        assertTrue(((List<?>) rs.getResult()).isEmpty());
        assertEquals(0, rs.getMeta().getTotal());
    }

    @Test
    @DisplayName("TC-DEPT-SER-003 - getById: ID tồn tại")
    void TC_DEPT_SER_003() {
        Department d = dept(1L, "IT");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d));

        assertEquals("IT", departmentService.getById(1L).getName());
        verify(departmentRepository).findById(1L);
    }

    @Test
    @DisplayName("TC-DEPT-SER-004 - getById: ID không tồn tại")
    void TC_DEPT_SER_004() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> departmentService.getById(999L));
        assertTrue(ex.getMessage().contains("999"));
        verify(departmentRepository).findById(999L);
    }

    @Test
    @DisplayName("TC-DEPT-SER-005 - create: mã/name hợp lệ, chưa tồn tại (theo logic trùng mã)")
    void TC_DEPT_SER_005() {
        CreateDepartmentDTO dto = new CreateDepartmentDTO();
        dto.setCode("it");
        dto.setName("IT");
        dto.setDescription("Phòng IT");
        when(departmentRepository.findByCode("IT")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        Department saved = departmentService.create(dto);

        ArgumentCaptor<Department> cap = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(cap.capture());
        assertEquals("IT", saved.getCode());
        assertEquals("IT", cap.getValue().getCode());
        assertEquals("IT", cap.getValue().getName());
        assertEquals("Phòng IT", cap.getValue().getDescription());
    }

    @Test
    @DisplayName("TC-DEPT-SER-006 - create: mã đã tồn tại")
    void TC_DEPT_SER_006() {
        CreateDepartmentDTO dto = new CreateDepartmentDTO();
        dto.setCode("HR");
        dto.setName("HR");
        when(departmentRepository.findByCode("HR")).thenReturn(Optional.of(dept(9L, "HR")));

        CustomException ex = assertThrows(CustomException.class, () -> departmentService.create(dto));
        assertTrue(ex.getMessage().contains("HR"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEPT-SER-007 - create: dữ liệu không hợp lệ → NPE/null code")
    void TC_DEPT_SER_007() {
        CreateDepartmentDTO dto = new CreateDepartmentDTO();
        dto.setCode(null);
        dto.setName(null);

        assertThrows(NullPointerException.class, () -> departmentService.create(dto));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEPT-SER-008 - update: cập nhật hợp lệ")
    void TC_DEPT_SER_008() {
        Department existing = dept(1L, "Old");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(existing)).thenReturn(existing);

        UpdateDepartmentDTO u = new UpdateDepartmentDTO();
        u.setName("IT Updated");

        Department out = departmentService.update(1L, u);

        assertEquals("IT Updated", out.getName());
        verify(departmentRepository).findById(1L);
        verify(departmentRepository).save(existing);
    }

    @Test
    @DisplayName("TC-DEPT-SER-009 - update: ID không tồn tại")
    void TC_DEPT_SER_009() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> departmentService.update(999L, new UpdateDepartmentDTO()));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("TC-DEPT-SER-010 - update: mã mới trùng phòng ban khác")
    void TC_DEPT_SER_010() {
        Department current = dept(1L, "A");
        current.setCode("OLD");
        Department other = dept(2L, "B");
        other.setCode("NEW");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(current));
        when(departmentRepository.findByCode("NEW")).thenReturn(Optional.of(other));

        UpdateDepartmentDTO u = new UpdateDepartmentDTO();
        u.setCode("new");

        CustomException ex = assertThrows(CustomException.class, () -> departmentService.update(1L, u));
        assertTrue(ex.getMessage().contains("NEW"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEPT-SER-011 - delete: chưa có nhân sự")
    void TC_DEPT_SER_011() {
        Department d = dept(1L, "X");
        d.setEmployees(new HashSet<>());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d));

        departmentService.delete(1L);

        verify(departmentRepository).findById(1L);
        verify(departmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("TC-DEPT-SER-012 - delete: còn nhân sự")
    void TC_DEPT_SER_012() {
        Department d = dept(1L, "X");
        Employee emp = new Employee();
        emp.setId(99L);
        d.setEmployees(new HashSet<>(List.of(emp)));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d));

        CustomException ex = assertThrows(CustomException.class, () -> departmentService.delete(1L));
        assertTrue(ex.getMessage().contains("nhân viên"));
        verify(departmentRepository, never()).deleteById(any());
    }

    private static Department dept(Long id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setEmployees(new HashSet<>());
        return d;
    }
}