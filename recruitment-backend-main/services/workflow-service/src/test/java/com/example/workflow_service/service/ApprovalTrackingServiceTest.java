package com.example.workflow_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.example.workflow_service.dto.PaginationDTO;
import com.example.workflow_service.dto.Response;
import com.example.workflow_service.dto.approval.ApprovalTrackingResponseDTO;
import com.example.workflow_service.dto.approval.ApproveStepDTO;
import com.example.workflow_service.dto.approval.CreateApprovalTrackingDTO;
import com.example.workflow_service.dto.approval.RequestWorkflowInfoDTO;
import com.example.workflow_service.exception.CustomException;
import com.example.workflow_service.exception.IdInvalidException;
import com.example.workflow_service.messaging.NotificationProducer;
import com.example.workflow_service.messaging.RecruitmentWorkflowEvent;
import com.example.workflow_service.messaging.RecruitmentWorkflowProducer;
import com.example.workflow_service.model.ApprovalTracking;
import com.example.workflow_service.model.Workflow;
import com.example.workflow_service.model.WorkflowStep;
import com.example.workflow_service.repository.ApprovalTrackingRepository;
import com.example.workflow_service.repository.WorkflowRepository;
import com.example.workflow_service.repository.WorkflowStepRepository;
import com.example.workflow_service.utils.SecurityUtil;
import com.example.workflow_service.utils.enums.ApprovalStatus;
import com.example.workflow_service.utils.enums.WorkflowType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalTrackingServiceTest {

    @Mock
    private ApprovalTrackingRepository approvalTrackingRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private RecruitmentWorkflowProducer workflowProducer;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private UserService userService;

    @Mock
    private CandidateService candidateService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ApprovalTrackingService approvalTrackingService;

    private MockedStatic<SecurityUtil> mockedSecurityUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockedSecurityUtil = mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::extractEmployeeId).thenReturn(100L);
        mockedSecurityUtil.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("mock-token"));

        ReflectionTestUtils.setField(approvalTrackingService, "userServiceBaseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(approvalTrackingService, "jobServiceBaseUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(approvalTrackingService, "objectMapper", objectMapper);

        when(approvalTrackingRepository.save(any(ApprovalTracking.class))).thenAnswer(invocation -> {
            ApprovalTracking tracking = invocation.getArgument(0);
            if (tracking.getId() == null) {
                tracking.setId(999L);
            }
            return tracking;
        });

        when(userService.getUserNamesByIds(anyList(), anyString())).thenReturn(Map.of(100L, "Requester User"));
        when(userService.getPositionNamesByIds(anyList(), anyString())).thenReturn(Map.of(10L, "Truong phong"));
        when(userService.findUserByPositionIdAndDepartmentId(anyLong(), anyLong(), anyString())).thenReturn(700L);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtil.close();
    }

    private Workflow workflow(Long id, WorkflowType type) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setType(type);
        return workflow;
    }

    private WorkflowStep step(Long id, int order, Long approverPositionId, Workflow workflow) {
        WorkflowStep step = new WorkflowStep();
        step.setId(id);
        step.setStepOrder(order);
        step.setApproverPositionId(approverPositionId);
        step.setWorkflow(workflow);
        step.setIsActive(true);
        return step;
    }

    private ApprovalTracking tracking(Long id, Long requestId, WorkflowStep step, ApprovalStatus status) {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(id);
        tracking.setRequestId(requestId);
        tracking.setStep(step);
        tracking.setStatus(status);
        if (step != null) {
            tracking.setApproverPositionId(step.getApproverPositionId());
        }
        return tracking;
    }

    private ResponseEntity<Response<List<Object>>> userPositionResponse(Long userId, Long positionId) throws Exception {
        Response<List<Object>> response = new Response<>();
        if (userId == null) {
            response.setData(Collections.emptyList());
        } else {
            Class<?> dtoClass = Class.forName("com.example.workflow_service.service.ApprovalTrackingService$UserPositionDTO");
            var constructor = dtoClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object dto = constructor.newInstance();
            ReflectionTestUtils.setField(dto, "userId", userId);
            ReflectionTestUtils.setField(dto, "positionId", positionId);
            response.setData(List.of(dto));
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private ResponseEntity<Response<JsonNode>> employeeResponse(Long positionId, Long departmentId, boolean useNestedDepartment) {
        ObjectNode employee = objectMapper.createObjectNode();
        if (positionId != null) {
            employee.set("position", objectMapper.createObjectNode().put("id", positionId));
        }
        if (departmentId != null) {
            if (useNestedDepartment) {
                employee.set("department", objectMapper.createObjectNode().put("id", departmentId));
            } else {
                employee.put("departmentId", departmentId);
            }
        }
        Response<JsonNode> response = new Response<>();
        response.setData(employee);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Test
    @DisplayName("TC_ATS_001 - Khởi tạo tracking thành công khi tìm được workflow và người duyệt bước đầu")
    void tcAts001_initializeApproval_success() throws Exception {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        dto.setDepartmentId(5L);
        dto.setRequestId(99L);
        dto.setLevelId(10L);

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        when(restTemplate.exchange(contains("user-positions"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(userPositionResponse(200L, 10L));

        ApprovalTrackingResponseDTO result = approvalTrackingService.initializeApproval(dto);

        assertNotNull(result);
        assertEquals(ApprovalStatus.PENDING, result.getStatus());
        assertEquals(99L, result.getRequestId());
        assertEquals(11L, result.getStepId());

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository).save(savedCaptor.capture());
        ApprovalTracking saved = savedCaptor.getValue();
        assertEquals(99L, saved.getRequestId());
        assertEquals(ApprovalStatus.PENDING, saved.getStatus());
        assertEquals(200L, saved.getApproverPositionId());
        assertEquals(700L, saved.getActionUserId());

        verify(notificationProducer).sendNotificationToDepartment(eq(5L), eq(10L), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("TC_ATS_002 - Khởi tạo tracking thất bại khi không có workflow phù hợp")
    void tcAts002_initializeApproval_workflowNotFound() {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        dto.setDepartmentId(5L);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> approvalTrackingService.initializeApproval(dto));

        assertTrue(exception.getMessage().contains("Không tìm thấy workflow"));
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_003 - Khởi tạo tracking thất bại khi bước đầu không có người duyệt")
    void tcAts003_initializeApproval_approverNotFound() throws Exception {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        dto.setDepartmentId(5L);
        dto.setRequestId(88L);

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        when(restTemplate.exchange(contains("user-positions"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(userPositionResponse(null, 10L));

        CustomException exception = assertThrows(CustomException.class,
                () -> approvalTrackingService.initializeApproval(dto));

        assertTrue(exception.getMessage().contains("Không tìm thấy người dùng"));
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_004 - Người được gán mới được quyền phê duyệt và bước kế tiếp được tạo")
    void tcAts004_approve_successWithNextStep() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, workflow);
        WorkflowStep nextStep = step(22L, 2, 20L, workflow);
        ApprovalTracking currentTracking = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(currentTracking));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(nextStep));
        when(userService.findUserByPositionIdAndDepartmentId(anyLong(), anyLong(), anyString())).thenReturn(300L);

        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(true);
        dto.setApprovalNotes("Dong y");

        ApprovalTrackingResponseDTO result = approvalTrackingService.approve(1L, dto);

        assertEquals(ApprovalStatus.APPROVED, result.getStatus());
        assertEquals("Dong y", result.getNotes());
        assertEquals(100L, result.getActionUserId());

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository, times(2)).save(savedCaptor.capture());
        List<ApprovalTracking> savedTrackings = savedCaptor.getAllValues();

        ApprovalTracking updatedCurrent = savedTrackings.get(0);
        ApprovalTracking createdNext = savedTrackings.get(1);

        assertEquals(ApprovalStatus.APPROVED, updatedCurrent.getStatus());
        assertEquals("Dong y", updatedCurrent.getNotes());
        assertNotNull(updatedCurrent.getActionAt());

        assertEquals(ApprovalStatus.PENDING, createdNext.getStatus());
        assertEquals(101L, createdNext.getRequestId());
        assertEquals(22L, createdNext.getStep().getId());
        assertEquals(20L, createdNext.getApproverPositionId());
    }

    @Test
    @DisplayName("TC_ATS_005 - Từ chối yêu cầu phải dừng tại bước hiện tại và không tạo bước kế tiếp")
    void tcAts005_approve_reject() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, workflow);
        ApprovalTracking currentTracking = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(currentTracking));

        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(false);
        dto.setApprovalNotes("Khong dat");

        ApprovalTrackingResponseDTO result = approvalTrackingService.approve(1L, dto);

        assertEquals(ApprovalStatus.REJECTED, result.getStatus());
        assertEquals("Khong dat", result.getNotes());

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository).save(savedCaptor.capture());
        ApprovalTracking saved = savedCaptor.getValue();
        assertEquals(ApprovalStatus.REJECTED, saved.getStatus());
        assertEquals("Khong dat", saved.getNotes());
    }

    @Test
    @DisplayName("TC_ATS_006 - Phê duyệt bị từ chối khi không đúng người được giao hoặc bước không còn pending")
    void tcAts006_approve_rejectedForUnauthorizedOrProcessedStep() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 999L, workflow);
        ApprovalTracking unauthorizedTracking = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(unauthorizedTracking));

        CustomException unauthorized = assertThrows(CustomException.class,
                () -> approvalTrackingService.approve(1L, new ApproveStepDTO()));
        assertTrue(unauthorized.getMessage().contains("không có quyền"));
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));

        ApprovalTracking processedTracking = tracking(2L, 101L, step(22L, 1, 100L, workflow), ApprovalStatus.APPROVED);
        when(approvalTrackingRepository.findById(2L)).thenReturn(Optional.of(processedTracking));

        CustomException processed = assertThrows(CustomException.class,
                () -> approvalTrackingService.approve(2L, new ApproveStepDTO()));
        assertTrue(processed.getMessage().contains("đã được xử lý"));
    }

    @Test
    @DisplayName("TC_ATS_007 - Lấy chi tiết tracking và danh sách pending của người duyệt")
    void tcAts007_getByIdAndPendingApprovals_success() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 10L, workflow);
        ApprovalTracking item = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);
        item.setActionUserId(100L);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(item));
        when(approvalTrackingRepository.findByApproverPositionIdAndStatus(10L, ApprovalStatus.PENDING))
                .thenReturn(List.of(item));

        ApprovalTrackingResponseDTO byId = approvalTrackingService.getById(1L);
        List<ApprovalTrackingResponseDTO> pending = approvalTrackingService.getPendingApprovalsForUser(10L);

        assertEquals(1L, byId.getId());
        assertEquals(ApprovalStatus.PENDING, byId.getStatus());
        assertEquals(1, pending.size());
        assertEquals(1L, pending.get(0).getId());

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_008 - Lấy danh sách tracking theo bộ lọc phải trả meta phân trang đúng")
    void tcAts008_getAll_success() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 10L, workflow);
        ApprovalTracking item = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);
        item.setActionUserId(100L);

        when(approvalTrackingRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        PaginationDTO pagination = approvalTrackingService.getAll(null, null, null, PageRequest.of(0, 10));

        assertEquals(1L, pagination.getMeta().getTotal());
        assertEquals(1, pagination.getMeta().getPage());
        assertEquals(1, ((List<?>) pagination.getResult()).size());
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_009 - Lịch sử workflow theo request type phải lọc đúng REQUEST và OFFER")
    void tcAts009_getWorkflowInfoByRequestId_filtersByRequestType() {
        Workflow requestWorkflow = workflow(1L, WorkflowType.REQUEST);
        Workflow offerWorkflow = workflow(2L, WorkflowType.OFFER);

        WorkflowStep requestStep = step(11L, 1, 10L, requestWorkflow);
        WorkflowStep offerStep = step(12L, 1, 20L, offerWorkflow);
        requestWorkflow.setSteps(new HashSet<>(Set.of(requestStep)));
        offerWorkflow.setSteps(new HashSet<>(Set.of(offerStep)));

        ApprovalTracking requestTracking = tracking(1L, 99L, requestStep, ApprovalStatus.PENDING);
        ApprovalTracking offerTracking = tracking(2L, 99L, offerStep, ApprovalStatus.APPROVED);

        when(approvalTrackingRepository.findByRequestId(99L)).thenReturn(List.of(requestTracking, offerTracking));
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(requestWorkflow));
        when(workflowRepository.findById(2L)).thenReturn(Optional.of(offerWorkflow));

        RequestWorkflowInfoDTO requestInfo = approvalTrackingService.getWorkflowInfoByRequestId(99L, 1L, "REQUEST");
        RequestWorkflowInfoDTO offerInfo = approvalTrackingService.getWorkflowInfoByRequestId(99L, 2L, "OFFER");
        RequestWorkflowInfoDTO rawInfo = approvalTrackingService.getWorkflowInfoByRequestId(99L, 1L, "UNKNOWN");

        assertEquals(1, requestInfo.getApprovalTrackings().size());
        assertEquals(1, offerInfo.getApprovalTrackings().size());
        assertEquals(2, rawInfo.getApprovalTrackings().size());
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_010 - Sự kiện không hợp lệ hoặc Offer thiếu dữ liệu bắt buộc phải bị bỏ qua an toàn")
    void tcAts010_handleWorkflowEvent_ignoreInvalidEvents() {
        approvalTrackingService.handleWorkflowEvent(null);

        RecruitmentWorkflowEvent missingType = new RecruitmentWorkflowEvent();
        missingType.setRequestId(1L);
        approvalTrackingService.handleWorkflowEvent(missingType);

        RecruitmentWorkflowEvent missingWorkflowId = new RecruitmentWorkflowEvent();
        missingWorkflowId.setEventType("REQUEST_SUBMITTED");
        missingWorkflowId.setRequestId(1L);
        approvalTrackingService.handleWorkflowEvent(missingWorkflowId);

        RecruitmentWorkflowEvent offerMissingDepartment = new RecruitmentWorkflowEvent();
        offerMissingDepartment.setEventType("REQUEST_SUBMITTED");
        offerMissingDepartment.setRequestId(2L);
        offerMissingDepartment.setWorkflowId(9L);
        offerMissingDepartment.setRequestType("OFFER");
        approvalTrackingService.handleWorkflowEvent(offerMissingDepartment);

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_011 - Offer submit phải tự resolve department từ candidate khi có dữ liệu")
    void tcAts011_handleWorkflowEvent_offerResolvesDepartmentFromCandidate() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("UNSUPPORTED");
        event.setRequestId(3L);
        event.setRequestType("OFFER");
        event.setCandidateId(88L);

        when(candidateService.getDepartmentIdFromCandidate(88L, null)).thenReturn(6L);

        approvalTrackingService.handleWorkflowEvent(event);

        assertEquals(6L, event.getDepartmentId());
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_012 - Submit lần đầu tạo bước đầu pending khi requester không trùng approver bước 1")
    void tcAts012_handleWorkflowEvent_submitFirstTime_createsPendingStep1() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(5L);
        event.setWorkflowId(1L);
        event.setRequestType("REQUEST");
        event.setRequesterId(100L);
        event.setDepartmentId(5L);
        event.setAuthToken("mock-token");

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);

        when(approvalTrackingRepository.findByRequestIdAndStatus(5L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(5L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(employeeResponse(99L, 5L, true), employeeResponse(99L, 5L, true));

        approvalTrackingService.handleWorkflowEvent(event);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository).save(savedCaptor.capture());
        ApprovalTracking saved = savedCaptor.getValue();
        assertEquals(ApprovalStatus.PENDING, saved.getStatus());
        assertEquals(11L, saved.getStep().getId());
        assertEquals(5L, saved.getRequestId());
    }

    @Test
    @DisplayName("TC_ATS_013 - Submit lần đầu tự duyệt bước 1 khi requester chính là approver đầu tiên")
    void tcAts013_handleWorkflowEvent_submitFirstTime_autoApproveStep1() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(6L);
        event.setWorkflowId(1L);
        event.setRequestType("REQUEST");
        event.setRequesterId(100L);
        event.setDepartmentId(5L);
        event.setAuthToken("mock-token");

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);
        WorkflowStep secondStep = step(12L, 2, 20L, workflow);

        when(approvalTrackingRepository.findByRequestIdAndStatus(6L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(6L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(secondStep));
        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(employeeResponse(10L, 5L, true), employeeResponse(10L, 5L, true));

        approvalTrackingService.handleWorkflowEvent(event);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository, times(3)).save(savedCaptor.capture());
        List<ApprovalTracking> savedTrackings = savedCaptor.getAllValues();

        assertEquals(ApprovalStatus.APPROVED, savedTrackings.get(1).getStatus());
        assertEquals("APPROVE", savedTrackings.get(1).getActionType());
        assertEquals(12L, savedTrackings.get(2).getStep().getId());
        assertEquals(ApprovalStatus.PENDING, savedTrackings.get(2).getStatus());
    }

    @Test
    @DisplayName("TC_ATS_014 - Submit lại sau khi bị trả về phải hủy placeholder cũ và tạo tracking mới")
    void tcAts014_handleWorkflowEvent_resubmitAfterReturn() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(7L);
        event.setWorkflowId(1L);
        event.setRequestType("REQUEST");
        event.setDepartmentId(5L);
        event.setActorUserId(100L);
        event.setAuthToken("mock-token");

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep returnedStep = step(31L, 2, 20L, workflow);

        ApprovalTracking placeholder = tracking(1L, 7L, returnedStep, ApprovalStatus.PENDING);
        placeholder.setNotes("Đang đợi cập nhật sau khi trả về");

        ApprovalTracking returnedTracking = tracking(2L, 7L, returnedStep, ApprovalStatus.RETURNED);
        returnedTracking.setReturnedToStepId(31L);

        when(approvalTrackingRepository.findByRequestIdAndStatus(7L, ApprovalStatus.PENDING)).thenReturn(List.of(placeholder));
        when(approvalTrackingRepository.findByRequestId(7L)).thenReturn(List.of(returnedTracking));
        when(workflowStepRepository.findById(31L)).thenReturn(Optional.of(returnedStep));

        approvalTrackingService.handleWorkflowEvent(event);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository, times(3)).save(savedCaptor.capture());
        List<ApprovalTracking> savedTrackings = savedCaptor.getAllValues();

        assertEquals(ApprovalStatus.CANCELLED, savedTrackings.get(0).getStatus());
        assertEquals("RESUBMIT", savedTrackings.get(0).getActionType());
        assertEquals(ApprovalStatus.PENDING, savedTrackings.get(1).getStatus());
        assertTrue(savedTrackings.get(2).getNotes().contains("Đã chỉnh sửa"));
    }

    @Test
    @DisplayName("TC_ATS_015 - Sự kiện duyệt bước hiện tại phải chuyển sang bước kế tiếp hoặc hoàn tất workflow")
    void tcAts015_handleWorkflowEvent_requestApproved() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);
        WorkflowStep secondStep = step(12L, 2, 20L, workflow);

        ApprovalTracking current = tracking(1L, 8L, firstStep, ApprovalStatus.PENDING);

        RecruitmentWorkflowEvent withNextStep = new RecruitmentWorkflowEvent();
        withNextStep.setEventType("REQUEST_APPROVED");
        withNextStep.setRequestId(8L);
        withNextStep.setRequestType("REQUEST");
        withNextStep.setDepartmentId(5L);
        withNextStep.setActorUserId(100L);
        withNextStep.setRequesterId(900L);
        withNextStep.setAuthToken("mock-token");

        when(approvalTrackingRepository.findByRequestIdAndStatus(8L, ApprovalStatus.PENDING))
                .thenReturn(List.of(current))
                .thenReturn(List.of(current));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(secondStep));
        when(userService.findUserByPositionIdAndDepartmentId(anyLong(), anyLong(), anyString())).thenReturn(801L);

        approvalTrackingService.handleWorkflowEvent(withNextStep);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository, times(2)).save(savedCaptor.capture());
        assertEquals(ApprovalStatus.APPROVED, savedCaptor.getAllValues().get(0).getStatus());
        assertEquals(ApprovalStatus.PENDING, savedCaptor.getAllValues().get(1).getStatus());

        ApprovalTracking completedCurrent = tracking(2L, 9L, firstStep, ApprovalStatus.PENDING);
        RecruitmentWorkflowEvent noNextStep = new RecruitmentWorkflowEvent();
        noNextStep.setEventType("REQUEST_APPROVED");
        noNextStep.setRequestId(9L);
        noNextStep.setRequestType("REQUEST");
        noNextStep.setActorUserId(100L);
        noNextStep.setRequesterId(900L);
        noNextStep.setAuthToken("mock-token");

        when(approvalTrackingRepository.findByRequestIdAndStatus(9L, ApprovalStatus.PENDING))
                .thenReturn(List.of(completedCurrent))
                .thenReturn(List.of());
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.empty());

        approvalTrackingService.handleWorkflowEvent(noNextStep);

        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    @DisplayName("TC_ATS_016 - Sự kiện từ chối phải cập nhật bước hiện tại và hủy các bước tương lai")
    void tcAts016_handleWorkflowEvent_requestRejected() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);
        WorkflowStep secondStep = step(12L, 2, 20L, workflow);

        ApprovalTracking current = tracking(1L, 10L, firstStep, ApprovalStatus.PENDING);
        ApprovalTracking future = tracking(2L, 10L, secondStep, ApprovalStatus.PENDING);

        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_REJECTED");
        event.setRequestId(10L);
        event.setRequestType("REQUEST");
        event.setActorUserId(100L);
        event.setNotes("Khong phu hop");
        event.setRequesterId(1000L);
        event.setAuthToken("mock-token");

        when(approvalTrackingRepository.findByRequestIdAndStatus(10L, ApprovalStatus.PENDING)).thenReturn(List.of(current));
        when(workflowStepRepository.findById(11L)).thenReturn(Optional.of(firstStep));
        when(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(1L)).thenReturn(List.of(firstStep, secondStep));
        when(approvalTrackingRepository.findByRequestId(10L)).thenReturn(List.of(current, future));

        approvalTrackingService.handleWorkflowEvent(event);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository, times(2)).save(savedCaptor.capture());
        List<ApprovalTracking> savedTrackings = savedCaptor.getAllValues();
        assertEquals(ApprovalStatus.REJECTED, savedTrackings.get(0).getStatus());
        assertEquals("REJECT", savedTrackings.get(0).getActionType());
        assertEquals(ApprovalStatus.CANCELLED, savedTrackings.get(1).getStatus());
    }

    @Test
    @DisplayName("TC_ATS_017 - Sự kiện trả về phải ghi nhận bước bị trả và fallback về bước đầu khi không chỉ định")
    void tcAts017_handleWorkflowEvent_requestReturned() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);
        WorkflowStep currentStep = step(12L, 2, 20L, workflow);

        ApprovalTracking current = tracking(1L, 11L, currentStep, ApprovalStatus.PENDING);

        RecruitmentWorkflowEvent explicitTarget = new RecruitmentWorkflowEvent();
        explicitTarget.setEventType("REQUEST_RETURNED");
        explicitTarget.setRequestId(11L);
        explicitTarget.setRequestType("REQUEST");
        explicitTarget.setReturnedToStepId(11L);
        explicitTarget.setActorUserId(100L);
        explicitTarget.setReason("Can bo sung");
        explicitTarget.setRequesterId(1000L);
        explicitTarget.setAuthToken("mock-token");

        when(approvalTrackingRepository.findByRequestIdAndStatus(11L, ApprovalStatus.PENDING)).thenReturn(List.of(current));
        when(workflowStepRepository.findById(12L)).thenReturn(Optional.of(currentStep));
        when(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(1L)).thenReturn(List.of(firstStep, currentStep));

        approvalTrackingService.handleWorkflowEvent(explicitTarget);

        assertEquals(ApprovalStatus.RETURNED, current.getStatus());
        assertEquals(11L, current.getReturnedToStepId());

        ApprovalTracking fallbackCurrent = tracking(2L, 12L, currentStep, ApprovalStatus.PENDING);
        RecruitmentWorkflowEvent fallbackTarget = new RecruitmentWorkflowEvent();
        fallbackTarget.setEventType("REQUEST_RETURNED");
        fallbackTarget.setRequestId(12L);
        fallbackTarget.setRequestType("REQUEST");
        fallbackTarget.setWorkflowId(1L);
        fallbackTarget.setActorUserId(100L);
        fallbackTarget.setReason("Lam lai");

        when(approvalTrackingRepository.findByRequestIdAndStatus(12L, ApprovalStatus.PENDING)).thenReturn(List.of(fallbackCurrent));
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));

        approvalTrackingService.handleWorkflowEvent(fallbackTarget);

        assertEquals(11L, fallbackCurrent.getReturnedToStepId());
    }

    @Test
    @DisplayName("TC_ATS_018 - Hủy hoặc rút yêu cầu phải cancel toàn bộ bước pending cùng loại workflow")
    void tcAts018_handleWorkflowEvent_cancelAndWithdraw() {
        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep step = step(11L, 1, 10L, workflow);

        ApprovalTracking cancelTracking = tracking(1L, 13L, step, ApprovalStatus.PENDING);
        ApprovalTracking withdrawTracking = tracking(2L, 14L, step, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findByRequestIdAndStatus(13L, ApprovalStatus.PENDING)).thenReturn(List.of(cancelTracking));
        when(approvalTrackingRepository.findByRequestIdAndStatus(14L, ApprovalStatus.PENDING)).thenReturn(List.of(withdrawTracking));

        RecruitmentWorkflowEvent cancelEvent = new RecruitmentWorkflowEvent();
        cancelEvent.setEventType("REQUEST_CANCELLED");
        cancelEvent.setRequestId(13L);
        cancelEvent.setRequestType("REQUEST");
        cancelEvent.setActorUserId(100L);
        cancelEvent.setReason("Dung tuyen");

        RecruitmentWorkflowEvent withdrawEvent = new RecruitmentWorkflowEvent();
        withdrawEvent.setEventType("REQUEST_WITHDRAWN");
        withdrawEvent.setRequestId(14L);
        withdrawEvent.setRequestType("REQUEST");
        withdrawEvent.setActorUserId(100L);
        withdrawEvent.setReason("Nguoi tao rut");

        approvalTrackingService.handleWorkflowEvent(cancelEvent);
        approvalTrackingService.handleWorkflowEvent(withdrawEvent);

        assertEquals(ApprovalStatus.CANCELLED, cancelTracking.getStatus());
        assertEquals("CANCEL", cancelTracking.getActionType());
        assertEquals(ApprovalStatus.CANCELLED, withdrawTracking.getStatus());
        assertEquals("WITHDRAW", withdrawTracking.getActionType());
    }

    @Test
    @DisplayName("TC_ATS_019 - Lỗi khi truy vấn thông tin requester không được làm sập luồng submit")
    void tcAts019_handleWorkflowEvent_requesterLookupFails_gracefully() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(15L);
        event.setWorkflowId(1L);
        event.setRequestType("REQUEST");
        event.setRequesterId(100L);
        event.setDepartmentId(5L);
        event.setAuthToken("mock-token");

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);

        when(approvalTrackingRepository.findByRequestIdAndStatus(15L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(15L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("user-service down"));

        assertDoesNotThrow(() -> approvalTrackingService.handleWorkflowEvent(event));
        verify(approvalTrackingRepository).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_020 | Khởi tạo tracking với workflow không có steps -> Ném CustomException")
    void tcAts020_initializeApproval_workflowWithoutSteps_throwsCustomException() {
        CreateApprovalTrackingDTO createTrackingDto = new CreateApprovalTrackingDTO();
        createTrackingDto.setDepartmentId(5L);
        createTrackingDto.setRequestId(99L);

        Workflow workflowWithoutSteps = workflow(1L, WorkflowType.REQUEST);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.of(workflowWithoutSteps));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.empty());

        CustomException thrownException = assertThrows(CustomException.class,
                () -> approvalTrackingService.initializeApproval(createTrackingDto));

        assertTrue(thrownException.getMessage().contains("Không tìm thấy bước đầu tiên")
                || thrownException.getMessage().contains("step"));

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName("TC_ATS_021 | Phê duyệt với tracking ID không tồn tại -> Ném IdInvalidException")
    void tcAts021_approve_trackingNotFound_throwsIdInvalidException() {
        Long nonExistentTrackingId = 999L;
        when(approvalTrackingRepository.findById(nonExistentTrackingId)).thenReturn(Optional.empty());

        ApproveStepDTO approveDto = new ApproveStepDTO();
        approveDto.setApproved(true);
        approveDto.setApprovalNotes("Dong y");

        IdInvalidException thrownException = assertThrows(IdInvalidException.class,
                () -> approvalTrackingService.approve(nonExistentTrackingId, approveDto));

        assertTrue(thrownException.getMessage().contains("999")
                || thrownException.getMessage().contains("không tìm thấy"));

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName(" TC_ATS_022 | Phê duyệt bước đã bị REJECTED -> Ném CustomException")
    void tcAts022_approve_alreadyRejectedStep_throwsCustomException() {
        Workflow testWorkflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, testWorkflow);
        ApprovalTracking rejectedTracking = tracking(1L, 101L, currentStep, ApprovalStatus.REJECTED);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(rejectedTracking));

        ApproveStepDTO approveDto = new ApproveStepDTO();
        approveDto.setApproved(true);

        CustomException thrownException = assertThrows(CustomException.class,
                () -> approvalTrackingService.approve(1L, approveDto));

        assertTrue(thrownException.getMessage().contains("đã được xử lý")
                || thrownException.getMessage().contains("REJECTED"));

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName(" TC_ATS_023 | Phê duyệt bước đã bị CANCELLED -> Ném CustomException")
    void tcAts023_approve_cancelledStep_throwsCustomException() {
        Workflow testWorkflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, testWorkflow);
        ApprovalTracking cancelledTracking = tracking(1L, 101L, currentStep, ApprovalStatus.CANCELLED);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(cancelledTracking));

        ApproveStepDTO approveDto = new ApproveStepDTO();
        approveDto.setApproved(true);

        CustomException thrownException = assertThrows(CustomException.class,
                () -> approvalTrackingService.approve(1L, approveDto));

        assertTrue(thrownException.getMessage().contains("đã được xử lý")
                || thrownException.getMessage().contains("CANCELLED"));

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName(" TC_ATS_024 | Phê duyệt thành công nhưng không tìm thấy người duyệt bước kế -> Ném CustomException")
    void tcAts024_approve_nextStepApproverNotFound_throwsCustomException() {
        Workflow testWorkflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, testWorkflow);
        WorkflowStep nextStep = step(22L, 2, 20L, testWorkflow);
        ApprovalTracking currentTracking = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(currentTracking));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(nextStep));
        when(userService.findUserByPositionIdAndDepartmentId(anyLong(), anyLong(), anyString()))
                .thenReturn(null);

        ApproveStepDTO approveDto = new ApproveStepDTO();
        approveDto.setApproved(true);
        approveDto.setApprovalNotes("Dong y");

        CustomException thrownException = assertThrows(CustomException.class,
                () -> approvalTrackingService.approve(1L, approveDto));

        assertTrue(thrownException.getMessage().contains("Không tìm thấy người dùng")
                || thrownException.getMessage().contains("approver"));

        verify(approvalTrackingRepository, times(1)).save(any(ApprovalTracking.class));
    }

    @Test
    @DisplayName(" TC_ATS_025 | getById() với ID không tồn tại -> Ném IdInvalidException")
    void tcAts025_getById_notFound_throwsIdInvalidException() {
        Long nonExistentId = 999L;
        when(approvalTrackingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IdInvalidException thrownException = assertThrows(IdInvalidException.class,
                () -> approvalTrackingService.getById(nonExistentId));

        assertTrue(thrownException.getMessage().contains("999")
                || thrownException.getMessage().contains("không tìm thấy"));
    }

    // ============================================================
    // CÁC TESTCASE BỔ SUNG ĐỂ TĂNG COVERAGE (TC_ATS_026 -> TC_ATS_032)
    // ============================================================

    /**
     * Test Case ID: TC_ATS_026
     * Mục đích: Lấy danh sách tracking (getAll) với việc truyền đầy đủ các tham số bộ lọc (tăng branch coverage trong Controller/Service filter).
     * Input: requestId=101L, status=APPROVED, actionUserId=100L.
     * Expected: Service xử lý thành công, gọi đúng hàm repository và trả về DTO phân trang hợp lệ.
     * CheckDB: Kiểm tra DB không bị thay đổi (never save).
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_026 - Lấy danh sách tracking với đầy đủ tham số bộ lọc (tăng branch coverage)")
    void tcAts026_getAll_withFullFilters() {
        Workflow workflow = workflow(1L, WorkflowType.OFFER);
        WorkflowStep step = step(21L, 1, 10L, workflow);
        ApprovalTracking item = tracking(1L, 101L, step, ApprovalStatus.APPROVED);
        item.setActionUserId(100L);

        when(approvalTrackingRepository.findByFilters(eq(101L), eq(ApprovalStatus.APPROVED), eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        PaginationDTO pagination = approvalTrackingService.getAll(101L, ApprovalStatus.APPROVED, 100L, PageRequest.of(0, 10));

        assertNotNull(pagination);
        assertEquals(1, pagination.getMeta().getTotal());
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    /**
     * Test Case ID: TC_ATS_027
     * Mục đích: Xử lý ngoại lệ khi gọi getWorkflowInfoByRequestId nhưng không tìm thấy workflow tương ứng trong DB (tăng branch missing).
     * Input: tracking tồn tại nhưng workflow liên kết bị null (findById trả về empty).
     * Expected: Service xử lý không crash, bỏ qua hoặc tiếp tục map dữ liệu tùy logic (ở đây đảm bảo không bị lỗi NullPointerException).
     * CheckDB: Không ghi nhận sự thay đổi trong DB.
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_027 - getWorkflowInfoByRequestId khi WorkflowId trong DB không tồn tại")
    void tcAts027_getWorkflowInfoByRequestId_workflowNotFound() {
        WorkflowStep step = step(11L, 1, 10L, null); // workflow=null giả lập bị mất dữ liệu liên kết
        ApprovalTracking tracking = tracking(1L, 99L, step, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findByRequestId(99L)).thenReturn(List.of(tracking));
        when(workflowRepository.findById(anyLong())).thenReturn(Optional.empty()); // Mock không tìm thấy workflow

        RequestWorkflowInfoDTO info = approvalTrackingService.getWorkflowInfoByRequestId(99L, 999L, "REQUEST");

        assertNotNull(info);
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    /**
     * Test Case ID: TC_ATS_028
     * Mục đích: Kiểm tra xử lý sự kiện Event: OFFER_SUBMITTED (giúp coverage các nhánh switch-case/if-else với requestType="OFFER").
     * Input: RecruitmentWorkflowEvent với type=OFFER_SUBMITTED.
     * Expected: Hệ thống tạo ra Tracking Status=PENDING và lưu vào DB thành công.
     * CheckDB: Gọi save() 1 lần.
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_028 - Xử lý Event OFFER_SUBMITTED (tạo mới tracking cho Offer)")
    void tcAts028_handleWorkflowEvent_offerSubmitted() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("OFFER_SUBMITTED");
        event.setRequestId(200L);
        event.setWorkflowId(2L);
        event.setRequestType("OFFER");
        event.setRequesterId(100L);
        event.setDepartmentId(5L);

        Workflow workflow = workflow(2L, WorkflowType.OFFER);
        WorkflowStep firstStep = step(22L, 1, 10L, workflow);

        when(approvalTrackingRepository.findByRequestIdAndStatus(200L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(200L)).thenReturn(List.of());
        when(workflowRepository.findById(2L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(2L, 1)).thenReturn(Optional.of(firstStep));

        approvalTrackingService.handleWorkflowEvent(event);

        ArgumentCaptor<ApprovalTracking> savedCaptor = ArgumentCaptor.forClass(ApprovalTracking.class);
        verify(approvalTrackingRepository).save(savedCaptor.capture());
        assertEquals(ApprovalStatus.PENDING, savedCaptor.getValue().getStatus());
        assertEquals(200L, savedCaptor.getValue().getRequestId());
    }

    /**
     * Test Case ID: TC_ATS_029
     * Mục đích: Kiểm tra luồng REQUEST_SUBMITTED nhưng event gửi lên bị thiếu workflowId (tăng coverage guards logic).
     * Input: Event type REQUEST_SUBMITTED, workflowId = null.
     * Expected: Hệ thống bypass an toàn (không crash) và không tạo bất kỳ Tracking nào.
     * CheckDB: never() save.
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_029 - Bỏ qua Event REQUEST_SUBMITTED do thiếu workflowId")
    void tcAts029_handleWorkflowEvent_requestSubmitted_missingWorkflowId() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(200L);
        event.setWorkflowId(null); // Trường bị thiếu
        event.setRequestType("REQUEST");

        approvalTrackingService.handleWorkflowEvent(event);

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    /**
     * Test Case ID: TC_ATS_030
     * Mục đích: Phê duyệt (Approve) với ghi chú rỗng/null (tăng coverage xử lý text rỗng).
     * Input: approveDto có notes = null.
     * Expected: Vẫn cho phép Approve, notes trong DB được lưu null hoặc map an toàn.
     * CheckDB: save() được gọi để cập nhật DB.
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_030 - Approve thành công khi notes bị bỏ trống (null)")
    void tcAts030_approve_nullNotes() {
        Workflow testWorkflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep currentStep = step(21L, 1, 100L, testWorkflow);
        ApprovalTracking currentTracking = tracking(1L, 101L, currentStep, ApprovalStatus.PENDING);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(currentTracking));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.empty()); // Không có bước tiếp theo

        ApproveStepDTO approveDto = new ApproveStepDTO();
        approveDto.setApproved(true);
        approveDto.setApprovalNotes(null); // Rỗng

        ApprovalTrackingResponseDTO result = approvalTrackingService.approve(1L, approveDto);

        assertEquals(ApprovalStatus.APPROVED, result.getStatus());
        assertNull(result.getNotes());
        verify(approvalTrackingRepository, atLeastOnce()).save(any(ApprovalTracking.class));
    }

    /**
     * Test Case ID: TC_ATS_031
     * Mục đích: Kiểm tra ngoại lệ khi call RestTemplate trong hàm initializeApproval bị fail (ví dụ: Service User bị sập).
     * Input: Yêu cầu khởi tạo Tracking hợp lệ, nhưng khi gọi API User để lấy Approver thì RestClientException bắn ra.
     * Expected: Hàm sẽ ném lỗi CustomException và ngừng tạo Tracking.
     * CheckDB: save() KHÔNG được gọi (đảm bảo tính toàn vẹn dữ liệu).
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_031 | Khởi tạo tracking thất bại do API ngoại lai (RestTemplate) ném Exception")
    void tcAts031_initializeApproval_restTemplateThrowsException() {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        dto.setDepartmentId(5L);
        dto.setRequestId(99L);
        dto.setLevelId(10L);

        Workflow workflow = workflow(1L, WorkflowType.REQUEST);
        WorkflowStep firstStep = step(11L, 1, 10L, workflow);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));

        // Giả lập ngoại lệ khi gọi RestTemplate
        when(restTemplate.exchange(contains("user-positions"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection Refused"));

        assertThrows(CustomException.class, () -> approvalTrackingService.initializeApproval(dto));
        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }

    /**
     * Test Case ID: TC_ATS_032
     * Mục đích: Kiểm tra hành vi khi CandidateService trả về DepartmentId = null trong Event xử lý Offer.
     * Input: Event type OFFER_* nhưng candidateService không lấy được DepartmentId.
     * Expected: Workflow event bỏ qua xử lý an toàn (do không định tuyến được bộ phận duyệt) -> bảo vệ DB khỏi rác data.
     * CheckDB: never() save.
     * Rollback: Mock tự động reset.
     */
    @Test
    @DisplayName("TC_ATS_032 - Xử lý Offer Event nhưng Candidate không tìm thấy phòng ban")
    void tcAts032_handleWorkflowEvent_offerCandidateDepartmentNotFound() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("OFFER_APPROVED");
        event.setRequestId(3L);
        event.setRequestType("OFFER");
        event.setCandidateId(88L);
        event.setDepartmentId(null);

        // CandidateService trả về rỗng (null)
        when(candidateService.getDepartmentIdFromCandidate(88L, null)).thenReturn(null);

        approvalTrackingService.handleWorkflowEvent(event);

        verify(approvalTrackingRepository, never()).save(any(ApprovalTracking.class));
    }
}
