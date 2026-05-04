package com.example.user_service.service;

import com.example.user_service.dto.employee.CreateEmployeeDTO;
import com.example.user_service.dto.employee.UpdateEmployeeDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Department;
import com.example.user_service.model.Employee;
import com.example.user_service.model.Position;
import com.example.user_service.repository.EmployeeRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho EmployeeService.java / EmployeesService.java
 *
 * Đúng theo docs: 24 test case
 * TC-EMP-SER-001 -> TC-EMP-SER-024
 */
@ExtendWith(MockitoExtension.class)
class EmployeesServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private PositionService positionService;

    @InjectMocks
    private EmployeeService employeeService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("TC-EMP-SER-001 - getAll - Danh sách có dữ liệu")
    void TC_EMP_SER_001() {
        Employee employee = employee(1L, "Nguyen Van A");

        when(employeeRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = employeeService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());

        verify(employeeRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TC-EMP-SER-002 - getAll - Danh sách rỗng")
    void TC_EMP_SER_002() {
        when(employeeRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = employeeService.getAll(pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());

        verify(employeeRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TC-EMP-SER-003 - search - Tìm theo keyword có kết quả")
    void TC_EMP_SER_003() {
        Employee employee = employee(1L, "Nguyen Van A");

        when(employeeRepository.findByFilters(null, null, null, "Nguyen", pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = employeeService.getAllWithFilters(null, null, null, "Nguyen", pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());

        verify(employeeRepository, times(1))
                .findByFilters(null, null, null, "Nguyen", pageable);
    }

    @Test
    @DisplayName("TC-EMP-SER-004 - search - Keyword không có kết quả")
    void TC_EMP_SER_004() {
        when(employeeRepository.findByFilters(null, null, null, "unknown", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = employeeService.getAllWithFilters(null, null, null, "unknown", pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());

        verify(employeeRepository, times(1))
                .findByFilters(null, null, null, "unknown", pageable);
    }

    @Test
    @DisplayName("TC-EMP-SER-005 - getByDepartmentId - Department tồn tại có nhân sự")
    void TC_EMP_SER_005() {
        Employee employee = employee(1L, "Nguyen Van A");

        when(employeeRepository.findByDepartmentIds(List.of(1L)))
                .thenReturn(List.of(employee));

        List<Employee> result = employeeService.getByDepartmentIds(List.of(1L));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(employeeRepository, times(1)).findByDepartmentIds(List.of(1L));
    }

    @Test
    @DisplayName("TC-EMP-SER-006 - getByDepartmentId - Department không tồn tại")
    void TC_EMP_SER_006() {
        assertThrows(CustomException.class,
                () -> employeeService.getByDepartmentIds(List.of(999L)));

        verify(employeeRepository, never()).findByDepartmentIds(anyList());
    }

    @Test
    @DisplayName("TC-EMP-SER-007 - getByPositionId - Position tồn tại có nhân sự")
    void TC_EMP_SER_007() {
        Employee employee = employee(1L, "Nguyen Van A");

        when(employeeRepository.findByFilters(null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = employeeService.getAllWithFilters(null, 1L, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());

        verify(employeeRepository, times(1))
                .findByFilters(null, 1L, null, null, pageable);
    }

    @Test
    @DisplayName("TC-EMP-SER-008 - getByPositionId - Position không tồn tại")
    void TC_EMP_SER_008() {
        assertThrows(CustomException.class,
                () -> employeeService.getAllWithFilters(null, 999L, null, null, pageable));

        verify(employeeRepository, never())
                .findByFilters(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-EMP-SER-009 - getById - ID tồn tại")
    void TC_EMP_SER_009() {
        Employee employee = employee(1L, "Nguyen Van A");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Employee result = employeeService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nguyen Van A", result.getName());

        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-EMP-SER-010 - getById - ID không tồn tại")
    void TC_EMP_SER_010() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> employeeService.getById(999L));

        verify(employeeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-EMP-SER-011 - create - Tạo nhân sự hợp lệ")
    void TC_EMP_SER_011() {
        CreateEmployeeDTO request = baseCreateDto();

        Department department = department(1L, "IT", "Phòng Công nghệ");
        Position position = position(1L, "Backend Developer");

        when(departmentService.getById(1L)).thenReturn(department);
        when(positionService.getById(1L)).thenReturn(position);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(1L);
            return employee;
        });

        Employee result = employeeService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, times(1)).save(captor.capture());

        Employee saved = captor.getValue();
        assertEquals("Full Name", saved.getName());
        assertEquals("employee@corp.com", saved.getEmail());
        assertEquals(1L, saved.getDepartment().getId());
        assertEquals(1L, saved.getPosition().getId());
    }

    @Test
    @DisplayName("TC-EMP-SER-012 - create - Email bị trùng")
    void TC_EMP_SER_012() {
        CreateEmployeeDTO request = baseCreateDto();
        request.setEmail("duplicate@corp.com");

        Department department = department(1L, "IT", "Phòng Công nghệ");
        Position position = position(1L, "Backend Developer");

        when(departmentService.getById(1L)).thenReturn(department);
        when(positionService.getById(1L)).thenReturn(position);

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.create(request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("email")
                        || ex.getMessage().toLowerCase().contains("trùng")
                        || ex.getMessage().toLowerCase().contains("duplicate")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-013 - create - Email sai định dạng")
    void TC_EMP_SER_013() {
        CreateEmployeeDTO request = baseCreateDto();
        request.setEmail("abc");

        Department department = department(1L, "IT", "Phòng Công nghệ");
        Position position = position(1L, "Backend Developer");

        when(departmentService.getById(1L)).thenReturn(department);
        when(positionService.getById(1L)).thenReturn(position);

        assertThrows(CustomException.class, () -> employeeService.create(request));

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-014 - create - Thiếu họ tên")
    void TC_EMP_SER_014() {
        CreateEmployeeDTO request = baseCreateDto();
        request.setName(null);

        Department department = department(1L, "IT", "Phòng Công nghệ");
        Position position = position(1L, "Backend Developer");

        when(departmentService.getById(1L)).thenReturn(department);
        when(positionService.getById(1L)).thenReturn(position);

        assertThrows(CustomException.class, () -> employeeService.create(request));

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-015 - create - Department không tồn tại")
    void TC_EMP_SER_015() {
        CreateEmployeeDTO request = baseCreateDto();
        request.setDepartmentId(999L);

        when(departmentService.getById(999L)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.create(request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("phòng ban")
                        || ex.getMessage().toLowerCase().contains("department")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-016 - create - Position không tồn tại")
    void TC_EMP_SER_016() {
        CreateEmployeeDTO request = baseCreateDto();
        request.setPositionId(999L);

        Department department = department(1L, "IT", "Phòng Công nghệ");

        when(departmentService.getById(1L)).thenReturn(department);
        when(positionService.getById(999L)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.create(request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("vị trí")
                        || ex.getMessage().toLowerCase().contains("position")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-017 - update - Cập nhật hợp lệ")
    void TC_EMP_SER_017() {
        Employee existing = employee(1L, "Old Name");

        UpdateEmployeeDTO request = new UpdateEmployeeDTO();
        request.setName("New Name");
        request.setEmail("new@corp.com");
        request.setPhone("0988000111");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = employeeService.update(1L, request);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("new@corp.com", result.getEmail());
        assertEquals("0988000111", result.getPhone());

        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-018 - update - Nhân sự không tồn tại")
    void TC_EMP_SER_018() {
        UpdateEmployeeDTO request = new UpdateEmployeeDTO();
        request.setName("New Name");

        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> employeeService.update(999L, request));

        verify(employeeRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-019 - update - Email mới trùng nhân sự khác")
    void TC_EMP_SER_019() {
        Employee existing = employee(1L, "Nguyen Van A");

        UpdateEmployeeDTO request = new UpdateEmployeeDTO();
        request.setEmail("existing@company.com");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.update(1L, request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("email")
                        || ex.getMessage().toLowerCase().contains("trùng")
                        || ex.getMessage().toLowerCase().contains("duplicate")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-020 - update - Đổi sang department không tồn tại")
    void TC_EMP_SER_020() {
        Employee existing = employee(1L, "Nguyen Van A");

        UpdateEmployeeDTO request = new UpdateEmployeeDTO();
        request.setDepartmentId(999L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentService.getById(999L)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.update(1L, request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("phòng ban")
                        || ex.getMessage().toLowerCase().contains("department")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-021 - update - Đổi sang position không tồn tại")
    void TC_EMP_SER_021() {
        Employee existing = employee(1L, "Nguyen Van A");

        UpdateEmployeeDTO request = new UpdateEmployeeDTO();
        request.setPositionId(999L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionService.getById(999L)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> employeeService.update(1L, request));

        assertTrue(
                ex.getMessage().toLowerCase().contains("vị trí")
                        || ex.getMessage().toLowerCase().contains("position")
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("TC-EMP-SER-022 - delete - Xóa nhân sự chưa có ràng buộc")
    void TC_EMP_SER_022() {
        employeeService.delete(1L);

        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC-EMP-SER-023 - delete - Nhân sự đã có tài khoản user")
    void TC_EMP_SER_023() {
        assertThrows(CustomException.class,
                () -> employeeService.delete(1L));

        verify(employeeRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("TC-EMP-SER-024 - delete - Nhân sự đang có review/phỏng vấn")
    void TC_EMP_SER_024() {
        assertThrows(CustomException.class,
                () -> employeeService.delete(99L));

        verify(employeeRepository, never()).deleteById(99L);
    }

    private static Employee employee(Long id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        return employee;
    }

    private static Department department(Long id, String code, String name) {
        Department department = new Department();
        department.setId(id);
        department.setCode(code);
        department.setName(name);
        return department;
    }

    private static Position position(Long id, String name) {
        Position position = new Position();
        position.setId(id);
        position.setName(name);
        return position;
    }

    private static CreateEmployeeDTO baseCreateDto() {
        return new CreateEmployeeDTO(
                "Full Name",
                "0909000111",
                "employee@corp.com",
                "MALE",
                "HN",
                "VN",
                LocalDate.of(1995, 1, 15),
                "ID123456",
                1L,
                1L,
                "ACTIVE"
        );
    }
}