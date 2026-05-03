package com.example.workflow_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockedStatic<SecurityUtil> mockedSecurityUtil;

    @BeforeEach
    void setUp() {
        mockedSecurityUtil = mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::extractEmployeeId).thenReturn(100L);
        mockedSecurityUtil.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("mock-token"));

        ReflectionTestUtils.setField(approvalTrackingService, "userServiceBaseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(approvalTrackingService, "jobServiceBaseUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(approvalTrackingService, "objectMapper", objectMapper);

        // Auto-return the entity passed to save()
        when(approvalTrackingRepository.save(any(ApprovalTracking.class))).thenAnswer(i -> {
            ApprovalTracking arg = i.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(new Random().nextLong());
            }
            return arg;
        });

        // Default mock for User Service
        when(userService.getUserNamesByIds(anyList(), anyString())).thenReturn(Map.of(100L, "Nguyễn Văn A"));
        when(userService.getPositionNamesByIds(anyList(), anyString())).thenReturn(Map.of(10L, "Trưởng phòng"));
        when(userService.findUserByPositionIdAndDepartmentId(anyLong(), anyLong(), anyString())).thenReturn(100L);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtil.close();
    }

    // ==================== HELPER METHODS ====================

    private Object createUserPositionDTO(Long userId, Long positionId) throws Exception {
        Class<?> clazz = Class.forName("com.example.workflow_service.service.ApprovalTrackingService$UserPositionDTO");
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        ReflectionTestUtils.setField(instance, "userId", userId);
        ReflectionTestUtils.setField(instance, "positionId", positionId);
        return instance;
    }

    private WorkflowStep createStep(Long id, int order, Long positionId, Workflow workflow) {
        WorkflowStep step = new WorkflowStep();
        step.setId(id);
        step.setStepOrder(order);
        step.setApproverPositionId(positionId);
        step.setWorkflow(workflow);
        return step;
    }

    private void mockRestTemplateForUserPosition(Long userId, Long positionId) throws Exception {
        Response<List<Object>> response = new Response<>();
        if (userId != null) {
            response.setData(List.of(createUserPositionDTO(userId, positionId)));
        } else {
            response.setData(Collections.emptyList());
        }
        when(restTemplate.exchange(contains("user-positions"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
    }

    // ==================== INITIALIZE APPROVAL ====================

    @Test
    void initializeApproval_Success() throws Exception {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        dto.setDepartmentId(5L);
        dto.setRequestId(99L);
        dto.setLevelId(10L);

        Workflow workflow = new Workflow();
        workflow.setId(1L);
        WorkflowStep firstStep = createStep(1L, 1, 10L, workflow);

        when(workflowRepository.findMatchingWorkflow(5L)).thenReturn(Optional.of(workflow));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(firstStep));
        mockRestTemplateForUserPosition(200L, 10L);

        ApprovalTrackingResponseDTO result = approvalTrackingService.initializeApproval(dto);

        assertNotNull(result);
        assertEquals(ApprovalStatus.PENDING, result.getStatus());
        // FIX LỖI 2: Đổi anyString() thành any() để có thể nhận giá trị null cho authToken
        verify(notificationProducer).sendNotificationToDepartment(eq(5L), eq(10L), anyString(), anyString(), any());
    }

    @Test
    void initializeApproval_WorkflowNotFound() {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        when(workflowRepository.findMatchingWorkflow(anyLong())).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> approvalTrackingService.initializeApproval(dto));
    }

    @Test
    void initializeApproval_FirstStepNotFound() {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        Workflow w = new Workflow(); w.setId(1L);
        when(workflowRepository.findMatchingWorkflow(anyLong())).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> approvalTrackingService.initializeApproval(dto));
    }

    @Test
    void initializeApproval_UserForPositionNotFound() throws Exception {
        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step = createStep(1L, 1, 10L, w);
        when(workflowRepository.findMatchingWorkflow(anyLong())).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step));

        // Mock API returns empty list
        mockRestTemplateForUserPosition(null, 10L);
        assertThrows(CustomException.class, () -> approvalTrackingService.initializeApproval(dto));
    }

    @Test
    void findUserByPositionId_ExceptionHandling() {
        // Trigger catch block in findUserByPositionId
        when(restTemplate.exchange(contains("user-positions"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("API Down"));

        CreateApprovalTrackingDTO dto = new CreateApprovalTrackingDTO();
        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step = createStep(1L, 1, 10L, w);
        when(workflowRepository.findMatchingWorkflow(anyLong())).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step));

        assertThrows(CustomException.class, () -> approvalTrackingService.initializeApproval(dto));
    }

    // ==================== APPROVE / REJECT ====================

    @Test
    void approve_Success_MoveToNextStep() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(100L); // Matches SecurityUtil mock
        tracking.setActionUserId(100L);

        Workflow workflow = new Workflow(); workflow.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, workflow);
        WorkflowStep step2 = createStep(2L, 2, 20L, workflow);
        tracking.setStep(step1);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(step2));

        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(true);
        dto.setApprovalNotes("Duyệt!");

        ApprovalTrackingResponseDTO result = approvalTrackingService.approve(1L, dto);

        assertEquals(ApprovalStatus.APPROVED, result.getStatus());
        assertEquals("Duyệt!", result.getNotes());
    }

    @Test
    void approve_Success_NoNextStep() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(100L);
        Workflow workflow = new Workflow(); workflow.setId(1L);
        tracking.setStep(createStep(1L, 1, 10L, workflow));

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.empty());

        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(true);
        approvalTrackingService.approve(1L, dto);

        assertEquals(ApprovalStatus.APPROVED, tracking.getStatus());
    }

    @Test
    void approve_Reject() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(100L);

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));

        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(false);
        approvalTrackingService.approve(1L, dto);

        assertEquals(ApprovalStatus.REJECTED, tracking.getStatus());
    }

    @Test
    void approve_TrackingNotFound() {
        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> approvalTrackingService.approve(1L, new ApproveStepDTO()));
    }

    @Test
    void approve_Unauthorized() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setApproverPositionId(999L); // Different from current user (100)
        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));

        assertThrows(CustomException.class, () -> approvalTrackingService.approve(1L, new ApproveStepDTO()));
    }

    @Test
    void approve_NotPending() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setApproverPositionId(100L);
        tracking.setStatus(ApprovalStatus.APPROVED);
        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));

        assertThrows(CustomException.class, () -> approvalTrackingService.approve(1L, new ApproveStepDTO()));
    }

    @Test
    void moveToNextStep_StepNull_ThrowsException() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setStatus(ApprovalStatus.PENDING);
        tracking.setApproverPositionId(100L);
        tracking.setStep(null); // Force error

        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));
        ApproveStepDTO dto = new ApproveStepDTO();
        dto.setApproved(true);

        assertThrows(CustomException.class, () -> approvalTrackingService.approve(1L, dto));
    }

    // ==================== GETTERS ====================

    @Test
    void getById_Success() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setActionUserId(100L);
        tracking.setApproverPositionId(10L);
        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.of(tracking));

        ApprovalTrackingResponseDTO result = approvalTrackingService.getById(1L);
        assertNotNull(result);
    }

    @Test
    void getById_NotFound() {
        when(approvalTrackingRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> approvalTrackingService.getById(1L));
    }

    @Test
    void getAll_Success() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        tracking.setActionUserId(100L);
        tracking.setApproverPositionId(10L);
        Page<ApprovalTracking> page = new PageImpl<>(List.of(tracking), PageRequest.of(0, 10), 1);

        when(approvalTrackingRepository.findByFilters(any(), any(), any(), any())).thenReturn(page);

        PaginationDTO res = approvalTrackingService.getAll(null, null, null, PageRequest.of(0, 10));
        assertEquals(1, res.getMeta().getTotal());
    }

    @Test
    void getPendingApprovalsForUser_Success() {
        ApprovalTracking tracking = new ApprovalTracking();
        tracking.setId(1L);
        when(approvalTrackingRepository.findByApproverPositionIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(tracking));
        assertFalse(approvalTrackingService.getPendingApprovalsForUser(1L).isEmpty());
    }

    // ==================== getWorkflowInfoByRequestId ====================

    @Test
    void getWorkflowInfoByRequestId_AllBranches() {
        Workflow wReq = new Workflow(); wReq.setId(1L); wReq.setType(WorkflowType.REQUEST);
        Workflow wOffer = new Workflow(); wOffer.setId(2L); wOffer.setType(WorkflowType.OFFER);

        WorkflowStep sReq = createStep(10L, 1, 5L, wReq);
        WorkflowStep sOffer = createStep(20L, 1, 6L, wOffer);
        wReq.setSteps(new HashSet<>(List.of(sReq)));

        ApprovalTracking tReq = new ApprovalTracking(); tReq.setStep(sReq); tReq.setStatus(ApprovalStatus.PENDING);
        ApprovalTracking tOffer = new ApprovalTracking(); tOffer.setStep(sOffer); tOffer.setStatus(ApprovalStatus.APPROVED);
        ApprovalTracking tNullStep = new ApprovalTracking(); tNullStep.setStep(null);

        when(approvalTrackingRepository.findByRequestId(99L)).thenReturn(List.of(tReq, tOffer, tNullStep));
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(wReq));

        // Branch: RECRUITMENT_REQUEST
        RequestWorkflowInfoDTO res1 = approvalTrackingService.getWorkflowInfoByRequestId(99L, null, "RECRUITMENT_REQUEST");
        assertEquals(1L, res1.getWorkflow().getId());
        assertEquals(1, res1.getApprovalTrackings().size());

        // Branch: REQUEST
        RequestWorkflowInfoDTO res2 = approvalTrackingService.getWorkflowInfoByRequestId(99L, 1L, "REQUEST");
        assertNotNull(res2.getWorkflow());

        // Branch: OFFER
        RequestWorkflowInfoDTO res3 = approvalTrackingService.getWorkflowInfoByRequestId(99L, null, "OFFER");
        assertEquals(1, res3.getApprovalTrackings().size());

        // Branch: UNKNOWN Type
        RequestWorkflowInfoDTO res4 = approvalTrackingService.getWorkflowInfoByRequestId(99L, 1L, "UNKNOWN");
        assertEquals(3, res4.getApprovalTrackings().size()); // No filter applied for unknown type

        // Branch: Type null or empty
        RequestWorkflowInfoDTO res5 = approvalTrackingService.getWorkflowInfoByRequestId(99L, 1L, "");
        assertEquals(3, res5.getApprovalTrackings().size());

        // Branch: Filtered trackings is Empty
        when(approvalTrackingRepository.findByRequestId(100L)).thenReturn(List.of(tOffer));
        RequestWorkflowInfoDTO res6 = approvalTrackingService.getWorkflowInfoByRequestId(100L, 1L, "REQUEST");
        assertTrue(res6.getApprovalTrackings().isEmpty());
    }

    // ==================== handleWorkflowEvent - Base ====================

    @Test
    void handleWorkflowEvent_NullEventOrTypes() {
        approvalTrackingService.handleWorkflowEvent(null);
        approvalTrackingService.handleWorkflowEvent(new RecruitmentWorkflowEvent()); // Missing type/id

        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        approvalTrackingService.handleWorkflowEvent(event); // Missing id

        event.setRequestId(1L);
        event.setEventType(null);
        approvalTrackingService.handleWorkflowEvent(event); // Missing type
    }

    @Test
    void handleWorkflowEvent_Offer_DepartmentIdResolution() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("UNKNOWN");
        event.setRequestId(1L);
        event.setRequestType("OFFER");

        // Case 1: candidateId is present and resolves
        event.setCandidateId(88L);
        when(candidateService.getDepartmentIdFromCandidate(eq(88L), any())).thenReturn(9L);
        approvalTrackingService.handleWorkflowEvent(event);
        assertEquals(9L, event.getDepartmentId());

        // Case 2: candidateService returns null
        event.setDepartmentId(null);
        when(candidateService.getDepartmentIdFromCandidate(eq(88L), any())).thenReturn(null);
        approvalTrackingService.handleWorkflowEvent(event);
        assertNull(event.getDepartmentId());

        // Case 3: candidateId is null
        event.setCandidateId(null);
        approvalTrackingService.handleWorkflowEvent(event);
        assertNull(event.getDepartmentId());
    }

    // ==================== handleWorkflowEvent - SUBMITTED ====================

    @Test
    void handleRequestSubmitted_MissingWorkflowId() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        approvalTrackingService.handleWorkflowEvent(event); // Should log warning and return
    }

    @Test
    void handleRequestSubmitted_OfferMissingDepartmentId() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setRequestType("OFFER");
        approvalTrackingService.handleWorkflowEvent(event); // Should log warning and return
    }

    @Test
    void handleRequestSubmitted_PendingExists_AllPlaceholders() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setRequestType("REQUEST");

        ApprovalTracking placeholder = new ApprovalTracking();
        placeholder.setStatus(ApprovalStatus.PENDING);
        placeholder.setNotes("Waiting for update after return - needed");

        Workflow w = new Workflow(); w.setType(WorkflowType.REQUEST);
        placeholder.setStep(createStep(1L, 1, 10L, w));

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(placeholder));
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of()); // No returned tracking

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(w.getId(), 1)).thenReturn(Optional.of(placeholder.getStep()));

        approvalTrackingService.handleWorkflowEvent(event);
        assertEquals(ApprovalStatus.CANCELLED, placeholder.getStatus());
        assertEquals("RESUBMIT", placeholder.getActionType());
    }

    @Test
    void handleRequestSubmitted_PendingExists_NotPlaceholders() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);

        ApprovalTracking normalPending = new ApprovalTracking();
        normalPending.setStatus(ApprovalStatus.PENDING);
        normalPending.setNotes("Normal notes");

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(normalPending));
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of());

        Workflow w = new Workflow(); w.setId(1L);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(createStep(1L, 1, 10L, w)));

        approvalTrackingService.handleWorkflowEvent(event);
        assertEquals(ApprovalStatus.CANCELLED, normalPending.getStatus());
        assertEquals("Cancelled due to resubmit", normalPending.getNotes());
    }

    @Test
    void handleRequestSubmitted_ResubmitAfterReturn() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);

        ApprovalTracking returned = new ApprovalTracking();
        returned.setReturnedToStepId(5L);

        WorkflowStep returnedStep = createStep(5L, 2, 10L, new Workflow());

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of(returned));
        when(workflowStepRepository.findById(5L)).thenReturn(Optional.of(returnedStep));

        approvalTrackingService.handleWorkflowEvent(event);
        verify(approvalTrackingRepository, atLeastOnce()).save(any(ApprovalTracking.class));
    }

    @Test
    void handleRequestSubmitted_ResubmitAfterReturn_WithExistingNotes() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);

        ApprovalTracking returned = new ApprovalTracking();
        returned.setReturnedToStepId(5L);

        WorkflowStep returnedStep = createStep(5L, 2, 10L, new Workflow());

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of(returned));
        when(workflowStepRepository.findById(5L)).thenReturn(Optional.of(returnedStep));

        when(approvalTrackingRepository.save(any(ApprovalTracking.class))).thenAnswer(i -> {
            ApprovalTracking t = i.getArgument(0);
            if (t.getStatus() == ApprovalStatus.PENDING && t.getNotes() == null) {
                t.setNotes("Old Notes");
            }
            return t;
        });

        approvalTrackingService.handleWorkflowEvent(event);
        verify(approvalTrackingRepository, atLeastOnce()).save(argThat(t -> t.getNotes() != null && t.getNotes().contains("Đã chỉnh sửa")));
    }

    @Test
    void handleRequestSubmitted_FirstSubmit_AutoApproveStep1_Match() throws Exception {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setDepartmentId(5L);
        event.setRequesterId(100L);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);
        WorkflowStep step2 = createStep(2L, 2, 20L, w);

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step1));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.of(step2));

        ObjectNode employeeNode = objectMapper.createObjectNode();
        employeeNode.set("position", objectMapper.createObjectNode().put("id", 10L));
        employeeNode.set("department", objectMapper.createObjectNode().put("id", 5L));
        Response<JsonNode> response = new Response<>();
        response.setData(employeeNode);

        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        approvalTrackingService.handleWorkflowEvent(event);

        verify(approvalTrackingRepository, atLeastOnce()).save(argThat(t -> t.getStatus() == ApprovalStatus.APPROVED));
        verify(approvalTrackingRepository, atLeastOnce()).save(argThat(t -> t.getStatus() == ApprovalStatus.PENDING && t.getStep().getId().equals(2L)));
    }

    @Test
    void handleRequestSubmitted_FirstSubmit_AutoApproveStep1_Match_NoStep2() throws Exception {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setDepartmentId(5L);
        event.setRequesterId(100L);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step1));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.empty());

        ObjectNode employeeNode = objectMapper.createObjectNode();
        employeeNode.set("position", objectMapper.createObjectNode().put("id", 10L));

        // FIX LỖI 1: Phải dùng put thay vì set để ghi nhận giá trị Long trực tiếp cho field departmentId
        employeeNode.put("departmentId", 5L);

        Response<JsonNode> response = new Response<>();
        response.setData(employeeNode);

        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        approvalTrackingService.handleWorkflowEvent(event);

        verify(workflowProducer, times(1)).publishEvent(argThat(e -> "WORKFLOW_COMPLETED".equals(e.getEventType())));
    }

    @Test
    void handleRequestSubmitted_FirstSubmit_AutoApproveStep1_NoMatch() throws Exception {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setDepartmentId(5L);
        event.setRequesterId(100L);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of());
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step1));

        ObjectNode employeeNode = objectMapper.createObjectNode();
        employeeNode.set("position", objectMapper.createObjectNode().put("id", 99L));
        Response<JsonNode> response = new Response<>();
        response.setData(employeeNode);

        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        approvalTrackingService.handleWorkflowEvent(event);

        verify(approvalTrackingRepository, atLeastOnce()).save(argThat(t -> t.getStatus() == ApprovalStatus.PENDING && t.getStep().getId().equals(1L)));
    }

    // ==================== handleWorkflowEvent - APPROVED / REJECTED ====================

    @Test
    void handleWorkflowEvent_Approved_NoNextStep_WorkflowCompleted() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_APPROVED");
        event.setRequestId(1L);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);
        ApprovalTracking current = new ApprovalTracking();
        current.setStatus(ApprovalStatus.PENDING);
        current.setStep(step1);

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING))
                .thenReturn(List.of(current))
                .thenReturn(List.of());

        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 2)).thenReturn(Optional.empty());

        approvalTrackingService.handleWorkflowEvent(event);

        assertEquals(ApprovalStatus.APPROVED, current.getStatus());
        verify(workflowProducer).publishEvent(argThat(e -> "WORKFLOW_COMPLETED".equals(e.getEventType())));
    }

    @Test
    void handleWorkflowEvent_Approved_TrackingNotFound() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_APPROVED");
        event.setRequestId(1L);
        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        approvalTrackingService.handleWorkflowEvent(event);
    }

    @Test
    void handleWorkflowEvent_Approved_StepNull() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_APPROVED");
        event.setRequestId(1L);
        ApprovalTracking current = new ApprovalTracking();
        current.setStatus(ApprovalStatus.PENDING);
        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(current));

        assertThrows(CustomException.class, () -> approvalTrackingService.handleWorkflowEvent(event));
    }

    @Test
    void handleWorkflowEvent_Rejected() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_REJECTED");
        event.setRequestId(1L);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);
        WorkflowStep step2 = createStep(2L, 2, 20L, w);

        ApprovalTracking current = new ApprovalTracking();
        current.setStatus(ApprovalStatus.PENDING);
        current.setStep(step1);

        ApprovalTracking future = new ApprovalTracking();
        future.setStatus(ApprovalStatus.PENDING);
        future.setStep(step2);

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(current));
        when(workflowStepRepository.findById(1L)).thenReturn(Optional.of(step1));
        when(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(1L)).thenReturn(List.of(step1, step2));
        when(approvalTrackingRepository.findByRequestId(1L)).thenReturn(List.of(current, future));

        approvalTrackingService.handleWorkflowEvent(event);

        assertEquals(ApprovalStatus.REJECTED, current.getStatus());
        assertEquals(ApprovalStatus.CANCELLED, future.getStatus());
    }

    // ==================== handleWorkflowEvent - RETURNED ====================

    @Test
    void handleWorkflowEvent_Returned_TargetStepNull() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_RETURNED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setReturnedToStepId(null);

        Workflow w = new Workflow(); w.setId(1L);
        WorkflowStep step1 = createStep(1L, 1, 10L, w);
        ApprovalTracking current = new ApprovalTracking();
        current.setStatus(ApprovalStatus.PENDING);
        current.setStep(createStep(2L, 2, 20L, w));

        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(current));
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(step1));
        when(workflowStepRepository.findById(2L)).thenReturn(Optional.of(current.getStep()));
        when(workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(1L)).thenReturn(List.of(step1, current.getStep()));

        approvalTrackingService.handleWorkflowEvent(event);

        assertEquals(ApprovalStatus.RETURNED, current.getStatus());
        assertEquals(1L, current.getReturnedToStepId());
    }

    @Test
    void handleWorkflowEvent_Returned_TrackingNotFound() {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_RETURNED");
        event.setRequestId(1L);
        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of());
        approvalTrackingService.handleWorkflowEvent(event);
    }

    // ==================== handleWorkflowEvent - CANCELLED / WITHDRAWN ====================

    @Test
    void handleWorkflowEvent_CancelledAndWithdrawn() {
        ApprovalTracking t1 = new ApprovalTracking(); t1.setStatus(ApprovalStatus.PENDING);
        ApprovalTracking t2 = new ApprovalTracking(); t2.setStatus(ApprovalStatus.PENDING);
        when(approvalTrackingRepository.findByRequestIdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(List.of(t1, t2));

        RecruitmentWorkflowEvent cancelEvent = new RecruitmentWorkflowEvent();
        cancelEvent.setEventType("REQUEST_CANCELLED");
        cancelEvent.setRequestId(1L);
        approvalTrackingService.handleWorkflowEvent(cancelEvent);
        assertEquals(ApprovalStatus.CANCELLED, t1.getStatus());
        assertEquals(ApprovalStatus.CANCELLED, t2.getStatus());

        t1.setStatus(ApprovalStatus.PENDING);
        RecruitmentWorkflowEvent withdrawEvent = new RecruitmentWorkflowEvent();
        withdrawEvent.setEventType("REQUEST_WITHDRAWN");
        withdrawEvent.setRequestId(1L);
        approvalTrackingService.handleWorkflowEvent(withdrawEvent);
        assertEquals(ApprovalStatus.CANCELLED, t1.getStatus());
    }

    // ==================== EDGE CASES & EXCEPTIONS ====================

    @Test
    void getRequesterDepartmentId_ExceptionHandling() {
        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("API Down"));

        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setRequesterId(100L);

        Workflow w = new Workflow(); w.setId(1L);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(createStep(1L, 1, 10L, w)));

        assertDoesNotThrow(() -> approvalTrackingService.handleWorkflowEvent(event));
    }

    @Test
    void getRequesterPositionId_NullBody() {
        when(restTemplate.exchange(contains("employees/100"), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setEventType("REQUEST_SUBMITTED");
        event.setRequestId(1L);
        event.setWorkflowId(1L);
        event.setRequesterId(100L);

        Workflow w = new Workflow(); w.setId(1L);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(w));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(1L, 1)).thenReturn(Optional.of(createStep(1L, 1, 10L, w)));

        assertDoesNotThrow(() -> approvalTrackingService.handleWorkflowEvent(event));
    }

    // ==================== DEAD CODE REFLECTION TEST ====================

    @Test
    void createTrackingForReturnedStep_ReflectionCall() throws Exception {
        RecruitmentWorkflowEvent event = new RecruitmentWorkflowEvent();
        event.setRequestId(1L);
        event.setDepartmentId(5L);

        WorkflowStep step = createStep(5L, 2, 10L, new Workflow());
        when(workflowStepRepository.findById(5L)).thenReturn(Optional.of(step));

        Method method = ApprovalTrackingService.class.getDeclaredMethod("createTrackingForReturnedStep",
                RecruitmentWorkflowEvent.class, Long.class, boolean.class);
        method.setAccessible(true);

        method.invoke(approvalTrackingService, event, 5L, false);
        verify(approvalTrackingRepository, atLeastOnce()).save(any(ApprovalTracking.class));

        // FIX LỖI 2: Mockito anyString() thay bằng any() để khớp với authToken = null
        verify(notificationProducer, times(1)).sendNotificationToDepartment(eq(5L), eq(10L), anyString(), anyString(), any());

        method.invoke(approvalTrackingService, event, 5L, true);
        verify(notificationProducer, times(1)).sendNotificationToDepartment(any(), any(), any(), any(), any());
    }
}