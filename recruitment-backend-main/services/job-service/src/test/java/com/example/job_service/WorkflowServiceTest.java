package com.example.workflow_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.workflow.CreateStepDTO;
import com.example.workflow_service.dto.workflow.CreateWorkflowDTO;
import com.example.workflow_service.dto.workflow.UpdateWorkflowDTO;
import com.example.workflow_service.dto.workflow.WorkflowResponseDTO;
import com.example.workflow_service.exception.CustomException;
import com.example.workflow_service.exception.IdInvalidException;
import com.example.workflow_service.model.Workflow;
import com.example.workflow_service.model.WorkflowStep;
import com.example.workflow_service.repository.WorkflowRepository;
import com.example.workflow_service.repository.WorkflowStepRepository;
import com.example.workflow_service.utils.SecurityUtil;
import com.example.workflow_service.utils.enums.WorkflowType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private WorkflowService workflowService;

    private MockedStatic<SecurityUtil> mockedSecurityUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("mocked_token");
        when(jwtAuth.getToken()).thenReturn(jwt);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("employeeId", 100L);
        when(jwtAuth.getTokenAttributes()).thenReturn(Map.of("user", userMap));
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        mockedSecurityUtil = mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::extractEmployeeId).thenReturn(100L);
        mockedSecurityUtil.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("mocked_token"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        mockedSecurityUtil.close();
    }

    private CreateStepDTO buildCreateStepDTO(int order, Long positionId) {
        CreateStepDTO step = new CreateStepDTO();
        step.setStepOrder(order);
        step.setApproverPositionId(positionId);
        return step;
    }

    // Helper tạo Map an toàn với null key/value để bypass lỗi Immutable Collections của Java
    private Map<Long, Integer> mockHierarchyMap(Long id1, Integer val1, Long id2, Integer val2) {
        Map<Long, Integer> map = new HashMap<>();
        if (id1 != null) map.put(id1, val1);
        if (id2 != null) map.put(id2, val2);
        return map;
    }

    private Map<Long, String> mockNamesMap(Long id1, String name1) {
        Map<Long, String> map = new HashMap<>();
        if (id1 != null) map.put(id1, name1);
        return map;
    }

    // ==================== CREATE ====================

    @Test
    void create_NameAlreadyExists_ThrowsException() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("Existing");
        when(workflowRepository.findByName("Existing")).thenReturn(Optional.of(new Workflow()));

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.create(dto));
        assertTrue(ex.getMessage().contains("Tên workflow đã tồn tại"));
    }

    @Test
    void create_Success_StepsNull() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(null); // Cover nhánh dto.getSteps() == null

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.create(dto);
        assertNotNull(result);
        verify(workflowStepRepository, never()).saveAll(any());
    }

    @Test
    void create_Success_StepsEmpty() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of()); // Cover nhánh dto.getSteps().isEmpty() == true

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.create(dto);
        assertNotNull(result);
    }

    @Test
    void create_Success_AllPositionIdsNull() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of(buildCreateStepDTO(1, null))); // positionId null sẽ bị filter loại bỏ

        // Cover nhánh !positionIds.isEmpty() == false (Bỏ qua hoàn toàn đoạn validate hierarchy)
        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.create(dto);
        assertNotNull(result);
        verify(userService, never()).getPositionHierarchyOrdersByIds(anyList(), anyString());
    }

    @Test
    void create_Success_ValidHierarchy() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 10L),
                buildCreateStepDTO(2, 20L)
        ));

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(10L, 4, 20L, 2)); // 4 giảm xuống 2 (hợp lệ)

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.create(dto);
        assertNotNull(result);
        verify(workflowStepRepository, times(1)).saveAll(anyList());
    }

    @Test
    void create_MixedList_WithNullPositionId_ThrowsExceptionInOriginalCode() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 10L),
                buildCreateStepDTO(2, null) // Bẫy coverage: Lọt qua filter nhưng chết ở vòng lặp
        ));

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(10L, 4, null, null));

        // Lúc này positionId = null, map.get(null) trả về null (vì dùng HashMap thay vì Map.of)
        // Code gốc sẽ throw CustomException("Không tìm thấy hierarchyOrder cho position ID: null")
        CustomException ex = assertThrows(CustomException.class, () -> workflowService.create(dto));
        assertTrue(ex.getMessage().contains("cho position ID: null"));
    }

    @Test
    void create_HierarchyMissing_ThrowsException() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of(buildCreateStepDTO(1, 10L)));

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(new HashMap<>()); // Không có data

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.create(dto));
        assertTrue(ex.getMessage().contains("Không tìm thấy hierarchyOrder cho position ID: 10"));
    }

    @Test
    void create_InvalidHierarchyOrder_ThrowsException() {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName("New WF");
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 20L),
                buildCreateStepDTO(2, 10L)
        ));

        when(workflowRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(20L, 2, 10L, 4)); // 2 tăng lên 4 (Lỗi logic)

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.create(dto));
        assertTrue(ex.getMessage().contains("Thứ tự hierarchy không hợp lệ"));
    }

    // ==================== GET BY ID ====================

    @Test
    void getById_Success() {
        Workflow workflow = new Workflow();
        workflow.setId(1L);
        workflow.setName("Test WF");

        WorkflowStep step1 = new WorkflowStep();
        step1.setApproverPositionId(5L);
        step1.setStepOrder(1);

        WorkflowStep step2 = new WorkflowStep();
        step2.setApproverPositionId(null); // Bẫy coverage cho hàm toStepResponseDTO
        step2.setStepOrder(2);

        workflow.setSteps(Set.of(step1, step2));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(userService.getPositionNamesByIds(anyList(), anyString())).thenReturn(mockNamesMap(5L, "Manager"));

        WorkflowResponseDTO result = workflowService.getById(1L);
        assertNotNull(result);
        assertEquals("Manager", result.getSteps().get(0).getApproverPositionName());
        assertNull(result.getSteps().get(1).getApproverPositionName());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> workflowService.getById(99L));
    }

    // ==================== GET ALL ====================

    @Test
    void getAll_Success_WithNullType() {
        Workflow w1 = new Workflow();
        w1.setId(1L);

        WorkflowStep step1 = new WorkflowStep();
        step1.setApproverPositionId(5L);
        WorkflowStep step2 = new WorkflowStep();
        step2.setApproverPositionId(null); // Cover nhanh filter(id -> id != null)
        w1.setSteps(Set.of(step1, step2));

        Workflow w2 = new Workflow();
        w2.setId(2L);
        w2.setSteps(null); // Cover nhanh filter(w -> w.getSteps() != null)

        Page<Workflow> page = new PageImpl<>(List.of(w1, w2), PageRequest.of(0, 10), 2);

        when(workflowRepository.findByFilters(isNull(), anyBoolean(), anyString(), anyLong(), any(Pageable.class))).thenReturn(page);
        when(userService.getPositionNamesByIds(anyList(), anyString())).thenReturn(mockNamesMap(5L, "Manager"));

        // Truyền type = null để cover toán tử 3 ngôi (type != null ? type.name() : null)
        PaginationDTO result = workflowService.getAll(null, true, "kw", 1L, PageRequest.of(0, 10));

        assertNotNull(result.getMeta());
        assertEquals(2, result.getMeta().getTotal());
    }

    @Test
    void getAll_Success_WithTypeNotNull() {
        Page<Workflow> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(workflowRepository.findByFilters(eq("OFFER"), anyBoolean(), anyString(), anyLong(), any(Pageable.class))).thenReturn(page);

        // Truyền type = OFFER để cover nhánh true của toán tử 3 ngôi
        PaginationDTO result = workflowService.getAll(WorkflowType.OFFER, true, "kw", 1L, PageRequest.of(0, 10));
        assertEquals(0, result.getMeta().getTotal());
    }

    // ==================== UPDATE ====================

    @Test
    void update_NotFound_ThrowsException() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> workflowService.update(99L, new UpdateWorkflowDTO()));
    }

    @Test
    void update_WorkflowStepsNull_DtoAllFieldsNull() {
        Workflow existing = new Workflow();
        existing.setId(1L);
        existing.setName("Original");
        existing.setSteps(null); // Cover nhánh workflow.getSteps() == null -> khởi tạo HashSet

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO(); // Toàn bộ field = null

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.update(1L, dto);
        assertEquals("Original", result.getName());
        assertNotNull(existing.getSteps());
    }

    @Test
    void update_NameCollision_ThrowsException() {
        Workflow existing = new Workflow();
        existing.setId(1L);
        existing.setName("Old Name");

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setName("New Name");

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workflowRepository.findByName("New Name")).thenReturn(Optional.of(new Workflow()));

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.update(1L, dto));
        assertTrue(ex.getMessage().contains("Tên workflow đã tồn tại"));
    }

    @Test
    void update_Success_UpdateAllFields_AndStepsEmpty() {
        Workflow existing = new Workflow();
        existing.setId(1L);
        existing.setName("Old");
        existing.setSteps(new HashSet<>(Set.of(new WorkflowStep()))); // Có step cũ

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setName("New");
        dto.setDescription("Desc");
        dto.setType(WorkflowType.OFFER);
        dto.setDepartmentId(2L);
        dto.setIsActive(false);
        dto.setSteps(List.of()); // Empty list -> Không chạy hierarchy, nhưng chạy clear steps

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workflowRepository.findByName("New")).thenReturn(Optional.empty());
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.update(1L, dto);

        assertEquals("New", result.getName());
        assertEquals("Desc", result.getDescription());
        assertEquals(WorkflowType.OFFER, result.getType());
        assertFalse(result.getIsActive());
        assertTrue(existing.getSteps().isEmpty()); // Step cũ đã bị clear
    }

    @Test
    void update_Success_ValidHierarchy() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(buildCreateStepDTO(1, 10L)));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(10L, 1, null, null));
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        workflowService.update(1L, dto);
        assertEquals(1, existing.getSteps().size());
    }

    @Test
    void update_MixedList_WithNullPositionId_ThrowsExceptionInOriginalCode() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 10L),
                buildCreateStepDTO(2, null) // Bẫy vòng lặp for check positionId null
        ));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(10L, 4, null, null));

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.update(1L, dto));
        assertTrue(ex.getMessage().contains("cho position ID: null"));
    }

    @Test
    void update_InvalidHierarchyOrder_ThrowsException() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 20L),
                buildCreateStepDTO(2, 10L)  // 4 > 2 (Lỗi)
        ));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(20L, 2, 10L, 4));

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.update(1L, dto));
        assertTrue(ex.getMessage().contains("Thứ tự hierarchy không hợp lệ"));
    }

    @Test
    void update_HierarchyOrderMissing_ThrowsException() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(buildCreateStepDTO(1, 10L)));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString())).thenReturn(new HashMap<>());

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.update(1L, dto));
        assertTrue(ex.getMessage().contains("Không tìm thấy hierarchyOrder cho position ID: 10"));
    }

    @Test
    void update_AllPositionIdsNull() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(buildCreateStepDTO(1, null)));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        workflowService.update(1L, dto);
        verify(userService, never()).getPositionHierarchyOrdersByIds(anyList(), anyString());
    }

    @Test
    void update_NameSameAsExisting_NoNameChange() {
        Workflow existing = new Workflow();
        existing.setId(1L);
        existing.setName("Same Name");

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setName("Same Name"); // dto.getName() != null nhưng equals workflow.getName() -> không đổi tên

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.update(1L, dto);
        assertEquals("Same Name", result.getName());
        verify(workflowRepository, never()).findByName(anyString());
    }

    @Test
    void update_ValidHierarchy_MultipleSteps_PreviousOrderNotNull() {
        Workflow existing = new Workflow();
        existing.setId(1L);

        UpdateWorkflowDTO dto = new UpdateWorkflowDTO();
        dto.setSteps(List.of(
                buildCreateStepDTO(1, 10L),
                buildCreateStepDTO(2, 20L) // previousHierarchyOrder != null && hierarchyOrder <= previousHierarchyOrder
        ));

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getPositionHierarchyOrdersByIds(anyList(), anyString()))
                .thenReturn(mockHierarchyMap(10L, 4, 20L, 2)); // 4 -> 2: giảm dần, hợp lệ
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WorkflowResponseDTO result = workflowService.update(1L, dto);
        assertNotNull(result);
        assertEquals(2, existing.getSteps().size());
    }

    // ==================== DELETE ====================

    @Test
    void delete_Success() {
        Workflow workflow = new Workflow();
        workflow.setId(1L);
        workflow.setIsActive(true);

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));

        workflowService.delete(1L);

        assertFalse(workflow.getIsActive());
        verify(workflowRepository, times(1)).save(workflow);
    }

    @Test
    void delete_NotFound_ThrowsException() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> workflowService.delete(99L));
    }
}