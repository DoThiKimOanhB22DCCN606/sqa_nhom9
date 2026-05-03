package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.SingleResponseDTO;
import com.example.job_service.dto.offer.ApproveOfferDTO;
import com.example.job_service.dto.offer.CancelOfferDTO;
import com.example.job_service.dto.offer.CreateOfferDTO;
import com.example.job_service.dto.offer.OfferDetailDTO;
import com.example.job_service.dto.offer.OfferWithUserDTO;
import com.example.job_service.dto.offer.RejectOfferDTO;
import com.example.job_service.dto.offer.ReturnOfferDTO;
import com.example.job_service.dto.offer.UpdateOfferDTO;
import com.example.job_service.dto.offer.WithdrawOfferDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.exception.UserClientException;
import com.example.job_service.messaging.OfferWorkflowProducer;
import com.example.job_service.messaging.RecruitmentWorkflowEvent;
import com.example.job_service.model.JobPosition;
import com.example.job_service.model.Offer;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.OfferRepository;
import com.example.job_service.utils.enums.OfferStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private UserClient userService;

    @Mock
    private OfferWorkflowProducer workflowProducer;

    @Mock
    private WorkflowClient workflowServiceClient;

    @Mock
    private CandidateClient candidateClient;

    @Mock
    private JobPositionService jobPositionService;

    @InjectMocks
    private OfferService offerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CreateOfferDTO buildCreateOfferDTO() {
        CreateOfferDTO dto = new CreateOfferDTO();
        dto.setCandidateId(1L);
        dto.setBasicSalary(15000000L);
        dto.setProbationSalaryRate(85);
        dto.setOnboardingDate(LocalDate.now().plusMonths(1));
        dto.setProbationPeriod(2);
        dto.setNotes("Good candidate");
        dto.setWorkflowId(1L);
        return dto;
    }

    private Offer buildOffer(Long id, OfferStatus status) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setCandidateId(1L);
        offer.setBasicSalary(15000000L);
        offer.setProbationSalaryRate(85);
        offer.setOnboardingDate(LocalDate.now().plusMonths(1));
        offer.setProbationPeriod(2);
        offer.setNotes("Initial notes");
        offer.setStatus(status);
        offer.setRequesterId(10L);
        offer.setOwnerUserId(10L);
        offer.setWorkflowId(1L);
        offer.setIsActive(true);
        offer.setCreatedAt(LocalDateTime.now());
        return offer;
    }

    private JobPosition buildJobPosition() {
        JobPosition jp = new JobPosition();
        jp.setId(1L);
        jp.setTitle("Java Developer");
        RecruitmentRequest rr = new RecruitmentRequest();
        rr.setDepartmentId(1L);
        jp.setRecruitmentRequest(rr);
        return jp;
    }

    // ==================== create ====================

    @Test
    void create_Success() {
        CreateOfferDTO dto = buildCreateOfferDTO();
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer offer = inv.getArgument(0);
            offer.setId(1L);
            return offer;
        });

        Offer result = offerService.create(dto);

        assertNotNull(result);
        assertEquals(OfferStatus.DRAFT, result.getStatus());
        assertEquals(1L, result.getCandidateId());
        assertEquals(15000000L, result.getBasicSalary());
        assertEquals(85, result.getProbationSalaryRate());
        assertTrue(result.getIsActive());
    }

    @Test
    void create_WithAllFields() {
        CreateOfferDTO dto = buildCreateOfferDTO();
        dto.setNotes("Excellent candidate");
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.create(dto);

        assertEquals("Excellent candidate", result.getNotes());
    }

    @Test
    void create_WithNullNotes() {
        CreateOfferDTO dto = buildCreateOfferDTO();
        dto.setNotes(null);
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.create(dto);

        assertNull(result.getNotes());
    }

    // ==================== update ====================

    @Test
    void update_AllFields() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        UpdateOfferDTO dto = new UpdateOfferDTO();
        dto.setCandidateId(2L);
        dto.setBasicSalary(20000000L);
        dto.setProbationSalaryRate(90);
        dto.setOnboardingDate(LocalDate.now().plusMonths(2));
        dto.setProbationPeriod(3);
        dto.setNotes("Updated notes");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.update(1L, dto);

        assertEquals(2L, result.getCandidateId());
        assertEquals(20000000L, result.getBasicSalary());
        assertEquals(90, result.getProbationSalaryRate());
        assertEquals(3, result.getProbationPeriod());
        assertEquals("Updated notes", result.getNotes());
    }

    @Test
    void update_NoFields() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        UpdateOfferDTO dto = new UpdateOfferDTO();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.update(1L, dto);

        assertEquals("Initial notes", result.getNotes());
        assertEquals(15000000L, result.getBasicSalary());
    }

    @Test
    void update_NotDraftStatus_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.update(1L, new UpdateOfferDTO()));
    }

    @Test
    void update_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.update(9999L, new UpdateOfferDTO()));
    }

    // ==================== submit ====================

    @Test
    void submit_Success() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        existing.setWorkflowId(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.submit(1L, 10L, "token");

        assertEquals(OfferStatus.PENDING, result.getStatus());
        assertNotNull(result.getSubmittedAt());
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void submit_SetRequesterId() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        existing.setRequesterId(null);
        existing.setWorkflowId(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.submit(1L, 10L, "token");

        assertEquals(10L, result.getRequesterId());
    }

    @Test
    void submit_SetOwnerUserId() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        existing.setOwnerUserId(null);
        existing.setWorkflowId(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.submit(1L, 10L, "token");

        assertEquals(10L, result.getOwnerUserId());
    }

    @Test
    void submit_NotDraftStatus_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.submit(1L, 10L, "token"));
    }

    @Test
    void submit_NoWorkflowId_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        existing.setWorkflowId(null);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.submit(1L, 10L, "token"));
    }

    @Test
    void submit_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.submit(9999L, 10L, "token"));
    }

    // ==================== approveStep ====================

    @Test
    void approveStep_Success() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        ApproveOfferDTO dto = new ApproveOfferDTO();
        dto.setApprovalNotes("Approved");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(existing)).thenReturn(existing);

        Offer result = offerService.approveStep(1L, dto, 10L, "token");

        assertNotNull(result);
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void approveStep_NotPendingStatus_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.approveStep(1L, new ApproveOfferDTO(), 10L, "token"));
    }

    @Test
    void approveStep_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.approveStep(9999L, new ApproveOfferDTO(), 10L, "token"));
    }

    // ==================== rejectStep ====================

    @Test
    void rejectStep_Success() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        RejectOfferDTO dto = new RejectOfferDTO();
        dto.setReason("Salary too high");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.rejectStep(1L, dto, 10L, "token");

        assertEquals(OfferStatus.REJECTED, result.getStatus());
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void rejectStep_NotPendingStatus_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.rejectStep(1L, new RejectOfferDTO(), 10L, "token"));
    }

    @Test
    void rejectStep_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.rejectStep(9999L, new RejectOfferDTO(), 10L, "token"));
    }

    // ==================== returnOffer ====================

    @Test
    void returnOffer_AlwaysThrowException() {
        assertThrows(IllegalStateException.class, () -> offerService.returnOffer(1L, new ReturnOfferDTO(), 10L, "token"));
    }

    // ==================== cancel ====================

    @Test
    void cancel_FromDraft() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        CancelOfferDTO dto = new CancelOfferDTO();
        dto.setReason("No longer needed");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.cancel(1L, dto, 10L, "token");

        assertEquals(OfferStatus.CANCELLED, result.getStatus());
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void cancel_FromPending() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        CancelOfferDTO dto = new CancelOfferDTO();
        dto.setReason("Changed requirements");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.cancel(1L, dto, 10L, "token");

        assertEquals(OfferStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancel_AlreadyCancelled_ReturnSame() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.CANCELLED);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        Offer result = offerService.cancel(1L, new CancelOfferDTO(), 10L, "token");

        assertEquals(OfferStatus.CANCELLED, result.getStatus());
        verify(offerRepository, never()).save(any());
    }

    @Test
    void cancel_FromApproved_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.APPROVED);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.cancel(1L, new CancelOfferDTO(), 10L, "token"));
    }

    @Test
    void cancel_FromRejected_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.REJECTED);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.cancel(1L, new CancelOfferDTO(), 10L, "token"));
    }

    @Test
    void cancel_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.cancel(9999L, new CancelOfferDTO(), 10L, "token"));
    }

    // ==================== withdraw ====================

    @Test
    void withdraw_ByOwner() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        existing.setOwnerUserId(10L);
        existing.setRequesterId(20L);
        WithdrawOfferDTO dto = new WithdrawOfferDTO();
        dto.setReason("Mistake");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.withdraw(1L, dto, 10L, "token");

        assertEquals(OfferStatus.WITHDRAWN, result.getStatus());
        verify(workflowProducer).publishEvent(any(RecruitmentWorkflowEvent.class));
    }

    @Test
    void withdraw_ByRequester() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        existing.setRequesterId(10L);
        existing.setOwnerUserId(20L);
        WithdrawOfferDTO dto = new WithdrawOfferDTO();
        dto.setReason("Need revision");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        Offer result = offerService.withdraw(1L, dto, 10L, "token");

        assertEquals(OfferStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    void withdraw_NotPendingStatus_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.withdraw(1L, new WithdrawOfferDTO(), 10L, "token"));
    }

    @Test
    void withdraw_NotOwnerOrRequester_ThrowException() {
        Offer existing = buildOffer(1L, OfferStatus.PENDING);
        existing.setOwnerUserId(10L);
        existing.setRequesterId(20L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> offerService.withdraw(1L, new WithdrawOfferDTO(), 99L, "token"));
    }

    @Test
    void withdraw_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.withdraw(9999L, new WithdrawOfferDTO(), 10L, "token"));
    }

    // ==================== findById ====================

    @Test
    void findById_Success() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

        Offer result = offerService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.findById(9999L));
    }

    // ==================== getByIdWithUser ====================

    @Test
    void getByIdWithUser_Success() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getByIdWithUser_WithJobPositionAndDepartment() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("jobPositionId", 1L);

        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        ObjectNode deptNode = objectMapper.createObjectNode();
        deptNode.put("name", "IT Department");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(jobPositionService.findById(1L)).thenReturn(buildJobPosition());
        when(userService.getDepartmentById(1L, "token")).thenReturn(ResponseEntity.ok(deptNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertEquals("Java Developer", result.getJobPositionTitle());
        assertEquals("IT Department", result.getDepartmentName());
    }

    @Test
    void getByIdWithUser_WithWorkflowInfo() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");
        JsonNode workflowNode = objectMapper.createObjectNode().put("status", "PENDING");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(workflowNode);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertNotNull(result.getWorkflowInfo());
    }

    @Test
    void getByIdWithUser_WithLevelFromPosition() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        ObjectNode employeeNode = objectMapper.createObjectNode();
        ObjectNode positionNode = objectMapper.createObjectNode();
        positionNode.put("level", "Senior");
        employeeNode.set("position", positionNode);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token"))
                .thenReturn(ResponseEntity.ok(requesterNode))
                .thenReturn(ResponseEntity.ok(employeeNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertEquals("Senior", result.getLevelName());
    }

    @Test
    void getByIdWithUser_WithLevelFromPositionName() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        ObjectNode employeeNode = objectMapper.createObjectNode();
        ObjectNode positionNode = objectMapper.createObjectNode();
        positionNode.put("name", "Manager");
        employeeNode.set("position", positionNode);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token"))
                .thenReturn(ResponseEntity.ok(requesterNode))
                .thenReturn(ResponseEntity.ok(employeeNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertEquals("Manager", result.getLevelName());
    }

    @Test
    void getByIdWithUser_UserClientException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.status(500).build());

        assertThrows(UserClientException.class, () -> offerService.getByIdWithUser(1L, "token"));
    }

    @Test
    void getByIdWithUser_NullRequesterId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setRequesterId(null);
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_NullCandidateId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setCandidateId(null);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_CandidateResponseNotSuccessful() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.status(404).build());
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_CandidateWithNullJobPositionId() throws Exception {
        Offer offer =  buildOffer(1L, OfferStatus.PENDING);
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.putNull("jobPositionId");

        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertNull(result.getJobPositionTitle());
    }

    @Test
    void getByIdWithUser_JobPositionException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("jobPositionId", 1L);

        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(jobPositionService.findById(1L)).thenThrow(new RuntimeException("Error"));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_JobPositionWithNullRecruitmentRequest() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("jobPositionId", 1L);

        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JobPosition jp = new JobPosition();
        jp.setId(1L);
        jp.setTitle("Java Developer");
        jp.setRecruitmentRequest(null);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(jobPositionService.findById(1L)).thenReturn(jp);
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
        assertEquals("Java Developer", result.getJobPositionTitle());
    }

    @Test
    void getByIdWithUser_DepartmentResponseNotSuccessful() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("jobPositionId", 1L);

        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(jobPositionService.findById(1L)).thenReturn(buildJobPosition());
        when(userService.getDepartmentById(1L, "token")).thenReturn(ResponseEntity.status(404).build());
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_EmployeeException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token"))
                .thenReturn(ResponseEntity.ok(requesterNode))
                .thenThrow(new RuntimeException("Error"));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdWithUser_NullEmployeeId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setRequesterId(null);
        offer.setOwnerUserId(null);
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");

        assertNotNull(result);
    }

    // ==================== getByIdWithUserAndMetadata ====================

    @Test
    void getByIdWithUserAndMetadata_Success() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        SingleResponseDTO<OfferWithUserDTO> result = offerService.getByIdWithUserAndMetadata(1L, "token");

        assertNotNull(result);
        assertNotNull(result.getData());
    }

    // ==================== getByIdDetail ====================

    @Test
    void getByIdDetail_Success() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("email", "jane@example.com");
        candidateNode.put("phone", "0123456789");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
        assertEquals("John Doe", result.getRequesterName());
        assertEquals("Jane Smith", result.getCandidateName());
        assertEquals("jane@example.com", result.getCandidateEmail());
        assertEquals("0123456789", result.getCandidatePhone());
    }

    @Test
    void getByIdDetail_WithWorkflowInfo() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");
        JsonNode workflowNode = objectMapper.createObjectNode().put("status", "PENDING");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(workflowNode);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
        assertNotNull(result.getWorkflowInfo());
    }

    @Test
    void getByIdDetail_NullWorkflowId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setWorkflowId(null);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
        assertNull(result.getWorkflowInfo());
    }

    @Test
    void getByIdDetail_NullRequesterId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setRequesterId(null);
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_RequesterException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenThrow(new RuntimeException("Error"));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_NullCandidateId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setCandidateId(null);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_CandidateException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenThrow(new RuntimeException("Error"));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_CandidateWithoutName() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        ObjectNode candidateNode = objectMapper.createObjectNode();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
        assertNull(result.getCandidateName());
    }

    @Test
    void getByIdDetail_CandidateWithJobPositionException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        ObjectNode candidateNode = objectMapper.createObjectNode();
        candidateNode.put("name", "Jane Smith");
        candidateNode.put("jobPositionId", 1L);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(requesterNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(jobPositionService.findById(1L)).thenThrow(new RuntimeException("Error"));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_EmployeeWithoutPosition() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");
        ObjectNode employeeNode = objectMapper.createObjectNode();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token"))
                .thenReturn(ResponseEntity.ok(requesterNode))
                .thenReturn(ResponseEntity.ok(employeeNode));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_NullEmployeeId() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        offer.setRequesterId(null);
        offer.setOwnerUserId(null);
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    @Test
    void getByIdDetail_EmployeeException() throws Exception {
        Offer offer = buildOffer(1L, OfferStatus.PENDING);
        JsonNode requesterNode = objectMapper.createObjectNode().put("name", "John Doe");
        JsonNode candidateNode = objectMapper.createObjectNode().put("name", "Jane Smith");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userService.getEmployeeById(10L, "token"))
                .thenReturn(ResponseEntity.ok(requesterNode))
                .thenThrow(new RuntimeException("Error"));
        when(candidateClient.getCandidateById(1L, "token")).thenReturn(ResponseEntity.ok(candidateNode));
        when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 1L, "OFFER", "token")).thenReturn(null);

        OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

        assertNotNull(result);
    }

    // ==================== getAllWithFilters ====================

    @Test
    void getAllWithFilters_WithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Offer> offers = List.of(buildOffer(1L, OfferStatus.PENDING));
        Page<Offer> page = new PageImpl<>(offers, pageable, 1);

        when(offerRepository.findByFilters(eq(OfferStatus.PENDING), any(), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(userService.getEmployeeById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters("PENDING", null, null, "token", pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test
    void getAllWithFilters_WithInvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(buildOffer(1L, OfferStatus.DRAFT)), pageable, 1);

        when(offerRepository.findByFilters(eq(null), any(), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(userService.getEmployeeById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters("INVALID_STATUS", null, null, "token", pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test
    void getAllWithFilters_WithEmptyStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(buildOffer(1L, OfferStatus.DRAFT)), pageable, 1);

        when(offerRepository.findByFilters(eq(null), any(), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(userService.getEmployeeById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters("", null, null, "token", pageable);

        assertNotNull(result);
    }

    @Test
    void getAllWithFilters_WithKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(buildOffer(1L, OfferStatus.DRAFT)), pageable, 1);

        when(offerRepository.findByFilters(any(), any(), eq("keyword"), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(userService.getEmployeeById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters(null, null, "keyword", "token", pageable);

        assertNotNull(result);
    }

    @Test
    void getAllWithFilters_WithCreatedBy() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(buildOffer(1L, OfferStatus.DRAFT)), pageable, 1);

        when(offerRepository.findByFilters(any(), eq(10L), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(userService.getEmployeeById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters(null, 10L, null, "token", pageable);

        assertNotNull(result);
    }

    @Test
    void getAllWithFilters_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(), pageable, 0);

        when(offerRepository.findByFilters(any(), any(), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());

        PaginationDTO result = offerService.getAllWithFilters(null, null, null, "token", pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
    }

    // ==================== findAllWithFilters ====================

    @Test
    void findAllWithFilters_WithCandidateId() {
        List<Offer> offers = List.of(buildOffer(1L, OfferStatus.DRAFT));
        when(offerRepository.findByFiltersList(any(), eq(1L), any(), any(), any(), any(), any(), any(), any())).thenReturn(offers);

        List<Offer> result = offerService.findAllWithFilters(null, 1L, null, null, null, null, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFilters_WithAllFilters() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusMonths(1);
        List<Offer> offers = List.of(buildOffer(1L, OfferStatus.PENDING));
        when(offerRepository.findByFiltersList(
                eq(OfferStatus.PENDING),
                eq(1L),
                eq(1L),
                eq(10L),
                eq(10000000L),
                eq(20000000L),
                eq(from),
                eq(to),
                eq("keyword")
        )).thenReturn(offers);

        List<Offer> result = offerService.findAllWithFilters("PENDING", 1L, 1L, 10L, 10000000L, 20000000L, from, to, "keyword");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFilters_WithInvalidStatus() {
        List<Offer> offers = List.of(buildOffer(1L, OfferStatus.DRAFT));
        when(offerRepository.findByFiltersList(eq(null), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(offers);

        List<Offer> result = offerService.findAllWithFilters("INVALID", null, null, null, null, null, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFilters_WithEmptyStatus() {
        List<Offer> offers = List.of(buildOffer(1L, OfferStatus.DRAFT));
        when(offerRepository.findByFiltersList(eq(null), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(offers);

        List<Offer> result = offerService.findAllWithFilters("", null, null, null, null, null, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFilters_EmptyResult() {
        when(offerRepository.findByFiltersList(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        List<Offer> result = offerService.findAllWithFilters(null, null, null, null, null, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    // ==================== delete ====================

    @Test
    void delete_Success() throws Exception {
        Offer existing = buildOffer(1L, OfferStatus.DRAFT);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = offerService.delete(1L);

        assertTrue(result);
        assertFalse(existing.getIsActive());
        verify(offerRepository).save(existing);
    }

    @Test
    void delete_NotFound_ThrowException() {
        when(offerRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> offerService.delete(9999L));
    }

    // ==================== convertToWithUserDTOList ====================

    @Test
    void convertToWithUserDTOList_WithNullRequesterId() {
        Pageable pageable = PageRequest.of(0, 10);
        Offer offer = buildOffer(1L, OfferStatus.DRAFT);
        offer.setRequesterId(null);
        Page<Offer> page = new PageImpl<>(List.of(offer), pageable, 1);

        when(offerRepository.findByFilters(any(), any(), any(), eq(pageable))).thenReturn(page);
        when(userService.getEmployeesByIds(any(), any())).thenReturn(Map.of());
        when(candidateClient.getCandidateById(any(), any())).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(workflowServiceClient.getWorkflowInfoByRequestId(any(), any(), any(), any())).thenReturn(null);

        PaginationDTO result = offerService.getAllWithFilters(null, null, null, "token", pageable);

        assertNotNull(result);
    }
}

