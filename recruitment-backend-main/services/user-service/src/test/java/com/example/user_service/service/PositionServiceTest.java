package com.example.user_service.service;

import com.example.user_service.dto.position.CreatePositionDTO;
import com.example.user_service.dto.position.UpdatePositionDTO;
import com.example.user_service.exception.CustomException;
import com.example.user_service.model.Employee;
import com.example.user_service.model.Position;
import com.example.user_service.repository.PositionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho PositionService.java
 *
 * Đúng theo docs: 12 test case
 * TC-POS-SER-001 -> TC-POS-SER-012
 */
@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("TC-POS-SER-001 - getAll - Danh sách có dữ liệu")
    void TC_POS_SER_001() {
        Position p1 = position(1L, "Backend Developer");
        Position p2 = position(2L, "Frontend Developer");
        Position p3 = position(3L, "QA Engineer");

        when(positionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(p1, p2, p3), pageable, 3));

        var result = positionService.getAll(pageable);

        assertNotNull(result);
        assertEquals(3, result.getMeta().getTotal());
        assertEquals(3, ((List<?>) result.getResult()).size());

        verify(positionRepository, times(1)).findAll(eq(pageable));
    }

    @Test
    @DisplayName("TC-POS-SER-002 - getAll - Danh sách rỗng")
    void TC_POS_SER_002() {
        when(positionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = positionService.getAll(pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());

        verify(positionRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("TC-POS-SER-003 - getById - ID tồn tại")
    void TC_POS_SER_003() {
        Position position = position(1L, "Senior Backend");

        when(positionRepository.findById(1L))
                .thenReturn(Optional.of(position));

        Position result = positionService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Senior Backend", result.getName());

        verify(positionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC-POS-SER-004 - getById - ID không tồn tại")
    void TC_POS_SER_004() {
        when(positionRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> positionService.getById(999L));

        verify(positionRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("TC-POS-SER-005 - create - Tên hợp lệ và chưa tồn tại")
    void TC_POS_SER_005() {
        CreatePositionDTO request = new CreatePositionDTO(
                "Backend Developer",
                "L2",
                2,
                true
        );

        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> {
                    Position saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        Position result = positionService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Backend Developer", result.getName());
        assertEquals("L2", result.getLevel());
        assertEquals(2, result.getHierarchyOrder());
        assertTrue(result.isActive());

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository, times(1)).save(captor.capture());

        Position saved = captor.getValue();
        assertEquals("Backend Developer", saved.getName());
        assertEquals("L2", saved.getLevel());
        assertEquals(2, saved.getHierarchyOrder());
        assertTrue(saved.isActive());
    }

    @Test
    @DisplayName("TC-POS-SER-006 - create - Tên bị trùng")
    void TC_POS_SER_006() {
        CreatePositionDTO request = new CreatePositionDTO(
                "Backend Developer",
                "L2",
                2,
                true
        );

        assertThrows(CustomException.class,
                () -> positionService.create(request));

        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("TC-POS-SER-007 - create - Tên null hoặc rỗng")
    void TC_POS_SER_007() {
        CreatePositionDTO request = new CreatePositionDTO(
                null,
                "L2",
                2,
                true
        );

        assertThrows(CustomException.class,
                () -> positionService.create(request));

        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("TC-POS-SER-008 - update - Cập nhật hợp lệ")
    void TC_POS_SER_008() {
        Position existing = position(1L, "Junior Backend");

        UpdatePositionDTO request = new UpdatePositionDTO();
        request.setName("Senior Backend");
        request.setLevel("L3");
        request.setHierarchyOrder(3);
        request.setIsActive(true);

        when(positionRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(positionRepository.save(existing))
                .thenReturn(existing);

        Position result = positionService.update(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Senior Backend", result.getName());
        assertEquals("L3", result.getLevel());
        assertEquals(3, result.getHierarchyOrder());
        assertTrue(result.isActive());

        verify(positionRepository, times(1)).findById(1L);
        verify(positionRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("TC-POS-SER-009 - update - ID không tồn tại")
    void TC_POS_SER_009() {
        UpdatePositionDTO request = new UpdatePositionDTO();
        request.setName("Senior Backend");

        when(positionRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> positionService.update(999L, request));

        verify(positionRepository, times(1)).findById(999L);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("TC-POS-SER-010 - update - Tên mới trùng chức vụ khác")
    void TC_POS_SER_010() {
        Position existing = position(1L, "Backend Developer");

        UpdatePositionDTO request = new UpdatePositionDTO();
        request.setName("HR Lead");

        when(positionRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(CustomException.class,
                () -> positionService.update(1L, request));

        verify(positionRepository, times(1)).findById(1L);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("TC-POS-SER-011 - delete - Xóa chức vụ chưa có nhân sự")
    void TC_POS_SER_011() {
        Position position = position(1L, "Backend Developer");
        position.setEmployees(new HashSet<>());

        lenient().when(positionRepository.findById(1L))
                .thenReturn(Optional.of(position));

        positionService.delete(1L);

        verify(positionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC-POS-SER-012 - delete - Chức vụ đang có nhân sự")
    void TC_POS_SER_012() {
        Position position = position(1L, "Backend Developer");
        Employee employee = new Employee();
        employee.setId(1L);
        position.setEmployees(new HashSet<>(List.of(employee)));

        lenient().when(positionRepository.findById(1L))
                .thenReturn(Optional.of(position));

        assertThrows(CustomException.class,
                () -> positionService.delete(1L));

        verify(positionRepository, never()).deleteById(1L);
    }

    private static Position position(Long id, String name) {
        Position position = new Position();
        position.setId(id);
        position.setName(name);
        position.setLevel("L2");
        position.setHierarchyOrder(2);
        position.setActive(true);
        position.setEmployees(new HashSet<>());
        return position;
    }
}