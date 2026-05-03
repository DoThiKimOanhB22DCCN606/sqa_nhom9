package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.SingleResponseDTO;
import com.example.job_service.dto.recruitment.ApproveRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CancelRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.CreateRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.RecruitmentRequestWithUserDTO;
import com.example.job_service.dto.recruitment.RejectRecruitmentRequestDTO;
import com.example.job_service.dto.recruitment.ReturnRecruitmentRequestDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.exception.UserClientException;
import com.example.job_service.messaging.RecruitmentWorkflowEvent;
import com.example.job_service.messaging.RecruitmentWorkflowProducer;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.RecruitmentRequestRepository;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class RecruitmentRequestServiceTest {

    @Mock
    private RecruitmentRequestRepository recruitmentRequestRepository;

    @Mock
    private UserClient userService;

    @Mock
    private RecruitmentWorkflowProducer workflowProducer;

    @Mock
    private WorkflowClient workflowServiceClient;

    @InjectMocks
    private RecruitmentRequestService recruitmentRequestService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private RecruitmentRequest buildRequest(Long id, RecruitmentRequestStatus status) {
        RecruitmentRequest rr = new RecruitmentRequest();
        rr.setId(id);
        rr.setTitle("Java Developer");
        rr.setStatus(status);
        rr.setRequesterId(10L);
        rr.setOwnerUserId(10L);
        rr.setDepartmentId(1L);
        rr.setWorkflowId(100L);
        rr.setActive(true);
        return rr;
    }

    // ==================== BASIC CRUD & SIMPLE METHODS ====================

    @Test
    void create_Success() {
        CreateRecruitmentRequestDTO dto = new CreateRecruitmentRequestDTO();
        dto.setTitle("Backend Dev");
        dto.setQuantity(2);
        dto.setReason("Mở rộng dự án");
        dto.setSalaryMin(BigDecimal.valueOf(1000L));
        dto.setSalaryMax(BigDecimal.valueOf(2000L));
        dto.setRequesterId(1L);
        dto.setWorkflowId(100L);
        dto.setDepartmentId(2L);

        when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> {
            RecruitmentRequest saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        RecruitmentRequest result = recruitmentRequestService.create(dto);

        assertNotNull(result);
        assertEquals("Backend Dev", result.getTitle());
        assertEquals(RecruitmentRequestStatus.DRAFT, result.getStatus());
        assertTrue(result.isActive());
    }

    @Test
    void update_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        CreateRecruitmentRequestDTO dto = new CreateRecruitmentRequestDTO();
        dto.setTitle("Updated Title");

        // FIX LỖI: Cung cấp giá trị cho quantity để tránh lỗi Unboxing từ null sang int
        dto.setQuantity(5);
        dto.setReason("Updated Reason");
        // Nếu có lỗi tương tự ở các trường khác, bạn set thêm ở đây nhé
        // dto.setSalaryMin(BigDecimal.valueOf(1000L));
        // dto.setSalaryMax(BigDecimal.valueOf(2000L));

        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.update(1L, dto);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(5, result.getQuantity());
    }

    @Test
    void delete_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));

        boolean result = recruitmentRequestService.delete(1L);

        assertTrue(result);
        assertFalse(rr.isActive());
        verify(recruitmentRequestRepository).save(rr);
    }

    @Test
    void findById_NotFound_ThrowsException() {
        when(recruitmentRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> recruitmentRequestService.findById(99L));
    }

    @Test
    void getById_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertNotNull(recruitmentRequestService.getById(1L));
    }

    @Test
    void changeStatus_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenReturn(rr);

        boolean result = recruitmentRequestService.changeStatus(1L, RecruitmentRequestStatus.APPROVED);
        assertTrue(result);
        assertEquals(RecruitmentRequestStatus.APPROVED, rr.getStatus());
    }

    @Test
    void getAllByDepartmentId_Success() {
        when(recruitmentRequestRepository.findByDepartmentId(1L)).thenReturn(List.of(new RecruitmentRequest()));
        assertEquals(1, recruitmentRequestService.getAllByDepartmentId(1L).size());
    }

    @Test
    void getAll_OnlyActive() {
        RecruitmentRequest active = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        RecruitmentRequest inactive = buildRequest(2L, RecruitmentRequestStatus.DRAFT);
        inactive.setActive(false);

        when(recruitmentRequestRepository.findAll()).thenReturn(List.of(active, inactive));

        List<RecruitmentRequest> result = recruitmentRequestService.getAll();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    // ==================== WORKFLOW STATE TRANSITIONS ====================

    @Test
    void submit_FromDraft_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        rr.setOwnerUserId(null); // Test nhánh ownerUserId == null
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.submit(1L, 99L, "token");

        assertEquals(RecruitmentRequestStatus.PENDING, result.getStatus());
        assertEquals(99L, result.getOwnerUserId());
        assertNotNull(result.getSubmittedAt());
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void submit_InvalidStatus_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.APPROVED);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.submit(1L, 99L, "token"));
    }

    @Test
    void approveStep_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.SUBMITTED);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.approveStep(1L, new ApproveRecruitmentRequestDTO(), 99L, "token");
        assertNotNull(result);
        verify(workflowProducer).publishEvent(any());
    }

    @Test
    void approveStep_InvalidStatus_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.approveStep(1L, new ApproveRecruitmentRequestDTO(), 99L, "token"));
    }

    @Test
    void rejectStep_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.PENDING);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.rejectStep(1L, new RejectRecruitmentRequestDTO(), 99L, "token");
        assertEquals(RecruitmentRequestStatus.REJECTED, result.getStatus());
    }

    @Test
    void rejectStep_InvalidStatus_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.rejectStep(1L, new RejectRecruitmentRequestDTO(), 99L, "token"));
    }

    @Test
    void returnRequest_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.SUBMITTED);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.returnRequest(1L, new ReturnRecruitmentRequestDTO(), 99L, "token");
        assertEquals(RecruitmentRequestStatus.RETURNED, result.getStatus());
    }

    @Test
    void returnRequest_InvalidStatus_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.returnRequest(1L, new ReturnRecruitmentRequestDTO(), 99L, "token"));
    }

    @Test
    void cancel_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.cancel(1L, new CancelRecruitmentRequestDTO(), 99L, "token");
        assertEquals(RecruitmentRequestStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancel_AlreadyCancelled_ReturnsSame() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.CANCELLED);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));

        RecruitmentRequest result = recruitmentRequestService.cancel(1L, new CancelRecruitmentRequestDTO(), 99L, "token");
        assertEquals(RecruitmentRequestStatus.CANCELLED, result.getStatus());
        verify(recruitmentRequestRepository, never()).save(any());
    }

    @Test
    void cancel_ApprovedOrRejected_ThrowsException() {
        RecruitmentRequest rrApproved = buildRequest(1L, RecruitmentRequestStatus.APPROVED);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rrApproved));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.cancel(1L, new CancelRecruitmentRequestDTO(), 99L, "token"));

        RecruitmentRequest rrRejected = buildRequest(2L, RecruitmentRequestStatus.REJECTED);
        when(recruitmentRequestRepository.findById(2L)).thenReturn(Optional.of(rrRejected));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.cancel(2L, new CancelRecruitmentRequestDTO(), 99L, "token"));
    }

    @Test
    void withdraw_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.PENDING);
        rr.setOwnerUserId(99L); // Actor is owner
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecruitmentRequest result = recruitmentRequestService.withdraw(1L, 99L, "token");
        assertEquals(RecruitmentRequestStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    void withdraw_InvalidStatus_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.withdraw(1L, 99L, "token"));
    }

    @Test
    void withdraw_NotOwnerOrRequester_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.PENDING);
        rr.setOwnerUserId(10L);
        rr.setRequesterId(20L);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));

        // Actor is 99L (Neither 10L nor 20L)
        assertThrows(IllegalStateException.class, () -> recruitmentRequestService.withdraw(1L, 99L, "token"));
    }

    // ==================== CONVERT TO USER DTO BRANCHES ====================

    @Test
    void getByIdWithUser_Success_FullFields() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        rr.setRequesterId(10L);
        rr.setDepartmentId(20L);
        rr.setWorkflowId(100L);

        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));

        ObjectNode userNode = objectMapper.createObjectNode().put("name", "John");
        ObjectNode deptNode = objectMapper.createObjectNode().put("name", "IT");
        ObjectNode workflowNode = objectMapper.createObjectNode().put("status", "ACTIVE");

        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(userNode));
        when(userService.getDepartmentById(20L, "token")).thenReturn(ResponseEntity.ok(deptNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 100L, "REQUEST", "token")).thenReturn(workflowNode);

        RecruitmentRequestWithUserDTO result = recruitmentRequestService.getByIdWithUser(1L, "token");

        assertNotNull(result.getRequester());
        assertNotNull(result.getDepartment());
        assertNotNull(result.getWorkflowInfo());
    }

    @Test
    void getByIdWithUserAndMetadata_Success() throws IdInvalidException {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        rr.setRequesterId(null);
        rr.setDepartmentId(null);
        rr.setWorkflowId(null);

        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, null, "REQUEST", "token")).thenReturn(null);

        SingleResponseDTO<RecruitmentRequestWithUserDTO> result = recruitmentRequestService.getByIdWithUserAndMetadata(1L, "token");

        assertNotNull(result);
        assertNull(result.getData().getRequester());
        assertNull(result.getData().getDepartment());
    }

    @Test
    void getByIdWithUser_EmployeeApiFails_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        assertThrows(UserClientException.class, () -> recruitmentRequestService.getByIdWithUser(1L, "token"));
    }

    @Test
    void getByIdWithUser_DepartmentApiFails_ThrowsException() {
        RecruitmentRequest rr = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(rr));

        ObjectNode userNode = objectMapper.createObjectNode().put("name", "John");
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(userNode));
        when(userService.getDepartmentById(1L, "token")).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        assertThrows(UserClientException.class, () -> recruitmentRequestService.getByIdWithUser(1L, "token"));
    }

    // ==================== LIST & FILTERS BRANCHES ====================

    @Test
    void findAllWithFilters_ParseStatusLogic() {
        // Valid status
        when(recruitmentRequestRepository.findByFiltersList(1L, RecruitmentRequestStatus.DRAFT, 10L, "key")).thenReturn(List.of());
        recruitmentRequestService.findAllWithFilters(1L, "DRAFT", 10L, "key");

        // Invalid status (caught and becomes null)
        when(recruitmentRequestRepository.findByFiltersList(1L, null, 10L, "key")).thenReturn(List.of());
        recruitmentRequestService.findAllWithFilters(1L, "INVALID_STATUS", 10L, "key");

        // Null status
        recruitmentRequestService.findAllWithFilters(1L, null, 10L, "key");

        // Empty status
        recruitmentRequestService.findAllWithFilters(1L, "   ", 10L, "key");
    }

    @Test
    void getAllWithFilters_DepartmentAndStatusLogic() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecruitmentRequest> page = new PageImpl<>(List.of());

        // departmentId == 1 -> becomes null
        when(recruitmentRequestRepository.findByFilters(null, null, null, null, pageable)).thenReturn(page);

        PaginationDTO result = recruitmentRequestService.getAllWithFilters(1L, "INVALID", null, null, "token", pageable);
        assertEquals(0, result.getMeta().getTotal());
    }

    @Test
    void convertToWithUserDTOList_ComplexEmployeeMap() {
        Pageable pageable = PageRequest.of(0, 10);

        RecruitmentRequest req1 = buildRequest(1L, RecruitmentRequestStatus.DRAFT);
        req1.setRequesterId(10L);

        RecruitmentRequest req2 = buildRequest(2L, RecruitmentRequestStatus.DRAFT);
        req2.setRequesterId(20L); // Employee without department

        RecruitmentRequest req3 = buildRequest(3L, RecruitmentRequestStatus.DRAFT);
        req3.setRequesterId(30L); // Employee with department but no ID

        RecruitmentRequest req4 = buildRequest(4L, RecruitmentRequestStatus.DRAFT);
        req4.setRequesterId(null); // No requester

        Page<RecruitmentRequest> page = new PageImpl<>(List.of(req1, req2, req3, req4));

        when(recruitmentRequestRepository.findByFilters(any(), any(), any(), any(), eq(pageable))).thenReturn(page);

        // Map data
        ObjectNode emp10 = objectMapper.createObjectNode();
        ObjectNode dept10 = objectMapper.createObjectNode().put("id", 99L);
        emp10.set("department", dept10);

        ObjectNode emp20 = objectMapper.createObjectNode(); // No department

        ObjectNode emp30 = objectMapper.createObjectNode();
        ObjectNode dept30 = objectMapper.createObjectNode(); // No ID
        emp30.set("department", dept30);

        Map<Long, JsonNode> map = Map.of(
                10L, emp10,
                20L, emp20,
                30L, emp30
        );

        when(userService.getEmployeesByIds(anyList(), anyString())).thenReturn(map);

        PaginationDTO result = recruitmentRequestService.getAllWithFilters(2L, "DRAFT", null, null, "token", pageable);

        assertNotNull(result);
        assertEquals(4, ((java.util.List<?>) result.getResult()).size());
    }
}