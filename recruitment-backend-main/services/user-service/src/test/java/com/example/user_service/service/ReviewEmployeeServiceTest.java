package com.example.user_service.service;

import com.example.user_service.dto.review.CreateReviewEmployeeDTO;
import com.example.user_service.dto.review.ReviewEmployeeResponseDTO;
import com.example.user_service.dto.review.UpdateReviewEmployeeDTO;
import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.model.Employee;
import com.example.user_service.model.ReviewEmployee;
import com.example.user_service.repository.EmployeeRepository;
import com.example.user_service.repository.ReviewEmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho ReviewEmployeeService.java
 *
 * Đúng theo docs: 14 test case
 * TC-REV-EMP-SER-001 -> TC-REV-EMP-SER-014
 */
@ExtendWith(MockitoExtension.class)
class ReviewEmployeeServiceTest {

    @Mock
    private ReviewEmployeeRepository reviewEmployeeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ReviewEmployeeService reviewEmployeeService;

    @Test
    @DisplayName("TC-REV-EMP-SER-001 - getAll - Danh sách có dữ liệu")
    void TC_REV_EMP_SER_001() {
        ReviewEmployee review = review(1L, 1L);

        when(reviewEmployeeRepository.findByFilters(
                eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        var result = reviewEmployeeService.getAllWithFilters(
                null,
                null,
                null,
                null,
                1,
                10,
                "createdAt",
                "desc"
        );

        assertNotNull(result);
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
        assertEquals(1, result.getMeta().getTotal());

        verify(reviewEmployeeRepository, times(1))
                .findByFilters(eq(null), eq(null), eq(null), eq(null), argThat(pageable ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 10
                ));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-002 - getById - ID tồn tại")
    void TC_REV_EMP_SER_002() throws IdInvalidException {
        ReviewEmployee review = review(1L, 2L);

        Employee reviewer = employee(2L, "Reviewer");

        when(reviewEmployeeRepository.findById(1L))
                .thenReturn(Optional.of(review));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(reviewer));

        ReviewEmployeeResponseDTO result = reviewEmployeeService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2L, result.getReviewerId());

        verify(reviewEmployeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1))
                .findAllById(argThat(ids -> containsId(ids, 2L)));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-003 - getById - ID không tồn tại")
    void TC_REV_EMP_SER_003() {
        when(reviewEmployeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.getById(999L));

        assertTrue(ex.getMessage().contains("Đánh giá"));

        verify(reviewEmployeeRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).findAllById(anyIterable());
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-004 - getByEmployeeId - Employee có review")
    void TC_REV_EMP_SER_004() {
        ReviewEmployee review = review(10L, 5L);

        Employee reviewedEmployee = employee(1L, "Employee A");
        review.setEmployee(reviewedEmployee);

        Employee reviewer = employee(5L, "Reviewer");

        when(reviewEmployeeRepository.findByEmployee_Id(1L))
                .thenReturn(List.of(review));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(reviewer));

        List<ReviewEmployeeResponseDTO> result = reviewEmployeeService.getByEmployeeId(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(10L, result.get(0).getId());

        verify(reviewEmployeeRepository, times(1)).findByEmployee_Id(1L);
        verify(employeeRepository, times(1))
                .findAllById(argThat(ids -> containsId(ids, 5L)));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-005 - getByEmployeeId - Employee không có review")
    void TC_REV_EMP_SER_005() {
        when(reviewEmployeeRepository.findByEmployee_Id(1L))
                .thenReturn(List.of());

        List<ReviewEmployeeResponseDTO> result = reviewEmployeeService.getByEmployeeId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(reviewEmployeeRepository, times(1)).findByEmployee_Id(1L);
        verify(employeeRepository, never()).findAllById(anyIterable());
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-006 - create - Tạo review hợp lệ")
    void TC_REV_EMP_SER_006() throws IdInvalidException {
        CreateReviewEmployeeDTO request = new CreateReviewEmployeeDTO();
        request.setEmployeeId(1L);
        request.setOnTimeCompletionScore(8);

        Employee reviewedEmployee = employee(1L, "Employee A");
        Employee reviewer = employee(2L, "Reviewer");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(reviewedEmployee));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(reviewer));

        when(reviewEmployeeRepository.save(any(ReviewEmployee.class)))
                .thenAnswer(invocation -> {
                    ReviewEmployee saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        ReviewEmployeeResponseDTO result = reviewEmployeeService.create(request, 2L);

        assertNotNull(result);
        assertEquals(100L, result.getId());

        ArgumentCaptor<ReviewEmployee> captor = ArgumentCaptor.forClass(ReviewEmployee.class);
        verify(reviewEmployeeRepository, times(1)).save(captor.capture());

        ReviewEmployee saved = captor.getValue();
        assertEquals(1L, saved.getEmployee().getId());
        assertEquals(2L, saved.getReviewerId());
        assertEquals(8, saved.getOnTimeCompletionScore());
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-007 - create - Employee được review không tồn tại")
    void TC_REV_EMP_SER_007() {
        CreateReviewEmployeeDTO request = new CreateReviewEmployeeDTO();
        request.setEmployeeId(999L);
        request.setOnTimeCompletionScore(8);

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.create(request, 1L));

        assertTrue(ex.getMessage().contains("Nhân viên"));

        verify(employeeRepository, times(1)).findById(999L);
        verify(reviewEmployeeRepository, never()).save(any(ReviewEmployee.class));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-008 - create - Reviewer không tồn tại")
    void TC_REV_EMP_SER_008() {
        CreateReviewEmployeeDTO request = new CreateReviewEmployeeDTO();
        request.setEmployeeId(1L);
        request.setOnTimeCompletionScore(8);

        Employee reviewedEmployee = employee(1L, "Employee A");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(reviewedEmployee));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(Collections.emptyList());

        when(reviewEmployeeRepository.save(any(ReviewEmployee.class)))
                .thenAnswer(invocation -> {
                    ReviewEmployee saved = invocation.getArgument(0);
                    saved.setId(5L);
                    return saved;
                });

        assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.create(request, 999L));

        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1))
                .findAllById(argThat(ids -> containsId(ids, 999L)));
        verify(reviewEmployeeRepository, never()).save(any(ReviewEmployee.class));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-009 - create - Reviewer tự review chính mình")
    void TC_REV_EMP_SER_009() {
        CreateReviewEmployeeDTO request = new CreateReviewEmployeeDTO();
        request.setEmployeeId(1L);
        request.setOnTimeCompletionScore(8);

        Employee employee = employee(1L, "Same Person");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(employee));

        when(reviewEmployeeRepository.save(any(ReviewEmployee.class)))
                .thenAnswer(invocation -> {
                    ReviewEmployee saved = invocation.getArgument(0);
                    saved.setId(77L);
                    return saved;
                });

        assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.create(request, 1L));

        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1))
                .findAllById(argThat(ids -> containsId(ids, 1L)));
        verify(reviewEmployeeRepository, never()).save(any(ReviewEmployee.class));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-010 - create - Điểm review ngoài khoảng hợp lệ")
    void TC_REV_EMP_SER_010() {
        CreateReviewEmployeeDTO request = new CreateReviewEmployeeDTO();
        request.setEmployeeId(1L);
        request.setOnTimeCompletionScore(11);

        Employee reviewedEmployee = employee(1L, "Employee A");
        Employee reviewer = employee(2L, "Reviewer");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(reviewedEmployee));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(reviewer));

        when(reviewEmployeeRepository.save(any(ReviewEmployee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(Exception.class,
                () -> reviewEmployeeService.create(request, 2L));

        verify(reviewEmployeeRepository, never()).save(any(ReviewEmployee.class));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-011 - update - Cập nhật review hợp lệ")
    void TC_REV_EMP_SER_011() throws IdInvalidException {
        ReviewEmployee stored = review(1L, 3L);

        when(reviewEmployeeRepository.findById(1L))
                .thenReturn(Optional.of(stored));

        when(employeeRepository.findAllById(anyIterable()))
                .thenReturn(Collections.emptyList());

        when(reviewEmployeeRepository.save(stored))
                .thenReturn(stored);

        UpdateReviewEmployeeDTO request = new UpdateReviewEmployeeDTO();
        request.setOnTimeCompletionScore(9);

        ReviewEmployeeResponseDTO result = reviewEmployeeService.update(1L, request, 3L);

        assertNotNull(result);
        assertEquals(9, result.getOnTimeCompletionScore());

        verify(reviewEmployeeRepository, times(1)).findById(1L);
        verify(reviewEmployeeRepository, times(1)).save(stored);
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-012 - update - Review không tồn tại")
    void TC_REV_EMP_SER_012() {
        when(reviewEmployeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.update(999L, new UpdateReviewEmployeeDTO(), 1L));

        assertTrue(ex.getMessage().contains("Đánh giá"));

        verify(reviewEmployeeRepository, times(1)).findById(999L);
        verify(reviewEmployeeRepository, never()).save(any(ReviewEmployee.class));
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-013 - delete - Xóa review hợp lệ")
    void TC_REV_EMP_SER_013() throws IdInvalidException {
        ReviewEmployee review = review(1L, 4L);

        when(reviewEmployeeRepository.findById(1L))
                .thenReturn(Optional.of(review));

        reviewEmployeeService.delete(1L, 4L);

        verify(reviewEmployeeRepository, times(1)).findById(1L);
        verify(reviewEmployeeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC-REV-EMP-SER-014 - delete - Xóa review không tồn tại")
    void TC_REV_EMP_SER_014() {
        when(reviewEmployeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class,
                () -> reviewEmployeeService.delete(999L, 1L));

        assertTrue(ex.getMessage().contains("Đánh giá"));

        verify(reviewEmployeeRepository, times(1)).findById(999L);
        verify(reviewEmployeeRepository, never()).deleteById(anyLong());
    }

    private static boolean containsId(Iterable<Long> ids, long id) {
        if (ids == null) {
            return false;
        }

        for (Long item : ids) {
            if (item != null && item == id) {
                return true;
            }
        }

        return false;
    }

    private static ReviewEmployee review(Long id, long reviewerId) {
        ReviewEmployee review = new ReviewEmployee();
        review.setId(id);
        review.setReviewerId(reviewerId);

        Employee employee = employee(10L, "Reviewed Employee");
        review.setEmployee(employee);

        return review;
    }

    private static Employee employee(Long id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        return employee;
    }
}