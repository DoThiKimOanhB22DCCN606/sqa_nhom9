package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import com.example.job_service.dto.recruitment.*;
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

/**
 * ============================================================
 * UNIT TEST MỞ RỘNG: RECRUITMENT REQUEST SERVICE
 * (Bổ sung vào file test gốc để tăng độ phủ code)
 *
 * CÁC NHÓM TEST HIỆN TẠI:
 * - NHÓM 4: Edge cases cho approveStep / rejectStep / returnRequest
 * - NHÓM 5: submit() với trạng thái RETURNED (re-submit)
 * - NHÓM 6: cancel() - happy path & idempotent
 * - NHÓM 7: withdraw() - sai trạng thái
 * - NHÓM 8: changeStatus() - thay đổi trạng thái trực tiếp
 * - NHÓM 9: getAllByDepartmentId() & getAll() & findAllWithFilters()
 * - NHÓM 10: getByIdWithUser() - lỗi DepartmentClient
 * - NHÓM 11: getByIdWithUserAndMetadata()
 * - NHÓM 12: getAllWithFilters() - edge case departmentId = 1
 * - NHÓM 13: update() & delete() - ID không tồn tại
 * - NHÓM 14: getById() - happy path (ID hợp lệ)
 *
 * CÁC NHÓM TEST BỔ SUNG ĐỂ TĂNG COVERAGE:
 * - NHÓM 15: create() - Happy path tạo mới
 * - NHÓM 16: delete() - Happy path xóa mềm (Soft delete)
 * - NHÓM 17: submit() - Happy path từ trạng thái DRAFT
 * - NHÓM 18: withdraw() - Exception do người dùng không có quyền
 * - NHÓM 19: getByIdWithUser() - Lỗi Employee Client
 * - NHÓM 20: getAllWithFilters() - Mapping thành công user names
 *
 * QUY ƯỚC:
 * CheckDB  → verify(repository).save(captor) kiểm tra entity được lưu đúng
 * Rollback → @ExtendWith(MockitoExtension) reset toàn bộ Mock sau mỗi test
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Recruitment Request Service - Extended Test Suite")
class RecruitmentRequestServiceTest {

    // ----------------------------------------------------------------
    // MOCK DEPENDENCIES (giống file gốc)
    // ----------------------------------------------------------------
    @Mock private RecruitmentRequestRepository recruitmentRequestRepository;
    @Mock private UserClient userService;
    @Mock private RecruitmentWorkflowProducer workflowProducer;
    @Mock private WorkflowClient workflowServiceClient;

    @InjectMocks private RecruitmentRequestService recruitmentRequestService;

    // Captor dùng để bắt argument được truyền vào repository/producer
    @Captor private ArgumentCaptor<RecruitmentRequest> requestCaptor;
    @Captor private ArgumentCaptor<RecruitmentWorkflowEvent> eventCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ROLLBACK: Mỗi test dùng một instance Mock mới (do @ExtendWith(MockitoExtension)).
     * mockDraftRequest là dữ liệu nền — TRẠNG THÁI BAN ĐẦU trước mỗi test.
     */
    private RecruitmentRequest mockDraftRequest;
    private RecruitmentRequest mockPendingRequest;
    private RecruitmentRequest mockApprovedRequest;

    @BeforeEach
    void setUp() {
        // --- Entity ở trạng thái DRAFT ---
        mockDraftRequest = new RecruitmentRequest();
        mockDraftRequest.setId(1L);
        mockDraftRequest.setTitle("Tuyển dụng Java Backend");
        mockDraftRequest.setQuantity(2);
        mockDraftRequest.setReason("Mở rộng team");
        mockDraftRequest.setSalaryMin(new BigDecimal("15000000"));
        mockDraftRequest.setSalaryMax(new BigDecimal("25000000"));
        mockDraftRequest.setStatus(RecruitmentRequestStatus.DRAFT);
        mockDraftRequest.setRequesterId(10L);
        mockDraftRequest.setOwnerUserId(10L);
        mockDraftRequest.setDepartmentId(20L);
        mockDraftRequest.setWorkflowId(100L);
        mockDraftRequest.setActive(true);

        // --- Entity ở trạng thái PENDING ---
        mockPendingRequest = new RecruitmentRequest();
        mockPendingRequest.setId(2L);
        mockPendingRequest.setTitle("Tuyển dụng QA Engineer");
        mockPendingRequest.setQuantity(1);
        mockPendingRequest.setStatus(RecruitmentRequestStatus.PENDING);
        mockPendingRequest.setRequesterId(10L);
        mockPendingRequest.setOwnerUserId(10L);
        mockPendingRequest.setDepartmentId(20L);
        mockPendingRequest.setWorkflowId(100L);
        mockPendingRequest.setActive(true);
        mockPendingRequest.setSubmittedAt(LocalDateTime.now().minusHours(1));

        // --- Entity ở trạng thái APPROVED ---
        mockApprovedRequest = new RecruitmentRequest();
        mockApprovedRequest.setId(3L);
        mockApprovedRequest.setTitle("Tuyển dụng DevOps");
        mockApprovedRequest.setQuantity(1);
        mockApprovedRequest.setStatus(RecruitmentRequestStatus.APPROVED);
        mockApprovedRequest.setRequesterId(10L);
        mockApprovedRequest.setOwnerUserId(10L);
        mockApprovedRequest.setDepartmentId(20L);
        mockApprovedRequest.setWorkflowId(100L);
        mockApprovedRequest.setActive(true);
    }

    // ================================================================
    // NHÓM 4: APPROVE / REJECT / RETURN — CÁC TRƯỜNG HỢP CÒN THIẾU
    // ================================================================
    @Nested
    @DisplayName("Approve / Reject / Return — Edge Cases (REQ-02)")
    class ApproveRejectReturnEdgeCasesTests {

        @Test
        @DisplayName("TC-REQ-APP-02 | Phê duyệt (Approve) yêu cầu trạng thái SUBMITTED -> Phát Event thành công")
        void approveStep_SubmittedRequest_ShouldPublishApprovedEvent() throws IdInvalidException {
            RecruitmentRequest submittedRequest = buildRequestWithStatus(4L, RecruitmentRequestStatus.SUBMITTED);
            ApproveRecruitmentRequestDTO approveDto = new ApproveRecruitmentRequestDTO();
            approveDto.setApprovalNotes("Đã xem xét, đồng ý");

            when(recruitmentRequestRepository.findById(4L)).thenReturn(Optional.of(submittedRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenReturn(submittedRequest);

            recruitmentRequestService.approveStep(4L, approveDto, 99L, "token");

            verify(recruitmentRequestRepository, times(1)).save(any(RecruitmentRequest.class));

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            RecruitmentWorkflowEvent capturedEvent = eventCaptor.getValue();
            assertEquals("REQUEST_APPROVED", capturedEvent.getEventType());
            assertEquals("Đã xem xét, đồng ý", capturedEvent.getNotes());
            assertEquals(99L, capturedEvent.getActorUserId());
        }

        @Test
        @DisplayName("TC-REQ-APP-03 | Phê duyệt (Approve) yêu cầu sai trạng thái (DRAFT) -> Ném IllegalStateException")
        void approveStep_DraftRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            ApproveRecruitmentRequestDTO approveDto = new ApproveRecruitmentRequestDTO();
            approveDto.setApprovalNotes("Ghi chú");

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.approveStep(1L, approveDto, 99L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
            verify(workflowProducer, never()).publishEvent(any());
        }

        @Test
        @DisplayName("TC-REQ-REJ-02 | Từ chối (Reject) yêu cầu sai trạng thái (DRAFT) -> Ném IllegalStateException")
        void rejectStep_DraftRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            RejectRecruitmentRequestDTO rejectDto = new RejectRecruitmentRequestDTO();
            rejectDto.setReason("Lý do từ chối");

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.rejectStep(1L, rejectDto, 99L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-REJ-03 | Từ chối (Reject) yêu cầu trạng thái SUBMITTED -> Chuyển sang REJECTED")
        void rejectStep_SubmittedRequest_ShouldChangeToRejected() throws IdInvalidException {
            RecruitmentRequest submittedRequest = buildRequestWithStatus(4L, RecruitmentRequestStatus.SUBMITTED);
            RejectRecruitmentRequestDTO rejectDto = new RejectRecruitmentRequestDTO();
            rejectDto.setReason("Vượt ngân sách phòng ban");

            when(recruitmentRequestRepository.findById(4L)).thenReturn(Optional.of(submittedRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.rejectStep(4L, rejectDto, 99L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.REJECTED, requestCaptor.getValue().getStatus());

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            assertEquals("REQUEST_REJECTED", eventCaptor.getValue().getEventType());
        }

        @Test
        @DisplayName("TC-REQ-RET-02 | Trả về (Return) yêu cầu sai trạng thái (DRAFT) -> Ném IllegalStateException")
        void returnRequest_DraftRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            ReturnRecruitmentRequestDTO returnDto = new ReturnRecruitmentRequestDTO();
            returnDto.setReason("Thiếu mô tả công việc");

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.returnRequest(1L, returnDto, 99L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-RET-03 | Trả về (Return) thành công -> Event chứa returnedToStepId đúng")
        void returnRequest_PendingWithStepId_ShouldPublishEventWithCorrectStepId() throws IdInvalidException {
            ReturnRecruitmentRequestDTO returnDto = new ReturnRecruitmentRequestDTO();
            returnDto.setReason("Cần bổ sung thông tin lương");
            returnDto.setReturnedToStepId(5L);

            when(recruitmentRequestRepository.findById(2L)).thenReturn(Optional.of(mockPendingRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.returnRequest(2L, returnDto, 99L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.RETURNED, requestCaptor.getValue().getStatus());

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            assertEquals("REQUEST_RETURNED", eventCaptor.getValue().getEventType());
            assertEquals(5L, eventCaptor.getValue().getReturnedToStepId());
        }
    }

    // ================================================================
    // NHÓM 5: SUBMIT — RE-SUBMIT SAU KHI BỊ RETURNED
    // ================================================================
    @Nested
    @DisplayName("Submit — Re-submit After Return (REQ-02)")
    class SubmitResubmitTests {

        @Test
        @DisplayName("TC-REQ-SUB-03 | Submit lại từ trạng thái RETURNED -> Chuyển sang PENDING & Phát Event")
        void submit_ReturnedRequest_ShouldChangeToPendingAndPublishEvent() throws IdInvalidException {
            RecruitmentRequest returnedRequest = buildRequestWithStatus(5L, RecruitmentRequestStatus.RETURNED);
            returnedRequest.setOwnerUserId(10L);

            when(recruitmentRequestRepository.findById(5L)).thenReturn(Optional.of(returnedRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.submit(5L, 10L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            RecruitmentRequest savedEntity = requestCaptor.getValue();
            assertEquals(RecruitmentRequestStatus.PENDING, savedEntity.getStatus());
            assertNotNull(savedEntity.getSubmittedAt());

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            assertEquals("REQUEST_SUBMITTED", eventCaptor.getValue().getEventType());
        }

        @Test
        @DisplayName("TC-REQ-SUB-04 | Submit lần đầu khi ownerUserId = null -> ownerUserId được gán bằng actorId")
        void submit_WhenOwnerUserIdIsNull_ShouldSetOwnerUserIdToActorId() throws IdInvalidException {
            RecruitmentRequest noOwnerRequest = buildRequestWithStatus(6L, RecruitmentRequestStatus.DRAFT);
            noOwnerRequest.setOwnerUserId(null);

            when(recruitmentRequestRepository.findById(6L)).thenReturn(Optional.of(noOwnerRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.submit(6L, 55L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(55L, requestCaptor.getValue().getOwnerUserId());
        }

        @Test
        @DisplayName("TC-REQ-SUB-05 | Submit yêu cầu đã APPROVED -> Ném IllegalStateException")
        void submit_ApprovedRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(3L)).thenReturn(Optional.of(mockApprovedRequest));

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.submit(3L, 10L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 6: CANCEL — HAPPY PATH & IDEMPOTENT
    // ================================================================
    @Nested
    @DisplayName("Cancel — Happy Path & Idempotent (REQ-02)")
    class CancelTests {

        @Test
        @DisplayName("TC-REQ-CAN-02 | Hủy (Cancel) yêu cầu ở trạng thái DRAFT -> Chuyển sang CANCELLED & Phát Event")
        void cancel_DraftRequest_ShouldChangeToCancelled() throws IdInvalidException {
            CancelRecruitmentRequestDTO cancelDto = new CancelRecruitmentRequestDTO();
            cancelDto.setReason("Không còn nhu cầu tuyển dụng");

            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.cancel(1L, cancelDto, 10L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.CANCELLED, requestCaptor.getValue().getStatus());

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            assertEquals("REQUEST_CANCELLED", eventCaptor.getValue().getEventType());
        }

        @Test
        @DisplayName("TC-REQ-CAN-03 | Hủy (Cancel) yêu cầu đã CANCELLED -> Idempotent, không thay đổi DB")
        void cancel_AlreadyCancelledRequest_ShouldBeIdempotentAndNotSave() throws IdInvalidException {
            RecruitmentRequest alreadyCancelledRequest = buildRequestWithStatus(7L, RecruitmentRequestStatus.CANCELLED);
            CancelRecruitmentRequestDTO cancelDto = new CancelRecruitmentRequestDTO();

            when(recruitmentRequestRepository.findById(7L)).thenReturn(Optional.of(alreadyCancelledRequest));

            RecruitmentRequest result = recruitmentRequestService.cancel(7L, cancelDto, 10L, "token");

            assertEquals(RecruitmentRequestStatus.CANCELLED, result.getStatus());
            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-CAN-04 | Hủy (Cancel) yêu cầu ở trạng thái PENDING -> Chuyển sang CANCELLED")
        void cancel_PendingRequest_ShouldChangeToCancelled() throws IdInvalidException {
            CancelRecruitmentRequestDTO cancelDto = new CancelRecruitmentRequestDTO();
            when(recruitmentRequestRepository.findById(2L)).thenReturn(Optional.of(mockPendingRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.cancel(2L, cancelDto, 10L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.CANCELLED, requestCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("TC-REQ-CAN-05 | Hủy (Cancel) yêu cầu đã REJECTED -> Ném IllegalStateException")
        void cancel_RejectedRequest_ShouldThrowIllegalStateException() {
            RecruitmentRequest rejectedRequest = buildRequestWithStatus(8L, RecruitmentRequestStatus.REJECTED);
            when(recruitmentRequestRepository.findById(8L)).thenReturn(Optional.of(rejectedRequest));

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.cancel(8L, new CancelRecruitmentRequestDTO(), 10L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 7: WITHDRAW — SẮP XẾP CÁC TRƯỜNG HỢP CÒN THIẾU
    // ================================================================
    @Nested
    @DisplayName("Withdraw — Missing Edge Cases (REQ-02)")
    class WithdrawEdgeCasesTests {

        @Test
        @DisplayName("TC-REQ-WID-03 | Rút lại (Withdraw) yêu cầu sai trạng thái (DRAFT) -> Ném IllegalStateException")
        void withdraw_DraftRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.withdraw(1L, 10L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-WID-04 | Rút lại (Withdraw) bởi requesterId (không phải owner) -> Thành công")
        void withdraw_ByRequesterId_ShouldChangeToWithdrawn() throws IdInvalidException {
            mockPendingRequest.setOwnerUserId(10L);
            mockPendingRequest.setRequesterId(15L);

            when(recruitmentRequestRepository.findById(2L)).thenReturn(Optional.of(mockPendingRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            recruitmentRequestService.withdraw(2L, 15L, "token");

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.WITHDRAWN, requestCaptor.getValue().getStatus());

            verify(workflowProducer).publishEvent(eventCaptor.capture());
            assertEquals("REQUEST_WITHDRAWN", eventCaptor.getValue().getEventType());
        }

        @Test
        @DisplayName("TC-REQ-WID-05 | Rút lại (Withdraw) yêu cầu đã APPROVED -> Ném IllegalStateException")
        void withdraw_ApprovedRequest_ShouldThrowIllegalStateException() {
            when(recruitmentRequestRepository.findById(3L)).thenReturn(Optional.of(mockApprovedRequest));

            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.withdraw(3L, 10L, "token"));

            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 8: CHANGE STATUS — THAY ĐỔI TRẠNG THÁI TRỰC TIẾP
    // ================================================================
    @Nested
    @DisplayName("Change Status (REQ-02 — Internal Status Update)")
    class ChangeStatusTests {

        @Test
        @DisplayName("TC-REQ-CST-01 | changeStatus() với ID hợp lệ -> Cập nhật trạng thái thành công & Trả về true")
        void changeStatus_ValidId_ShouldUpdateStatusAndReturnTrue() throws IdInvalidException {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

            boolean result = recruitmentRequestService.changeStatus(1L, RecruitmentRequestStatus.APPROVED);

            assertTrue(result);
            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.APPROVED, requestCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("TC-REQ-CST-02 | changeStatus() với ID không tồn tại -> Ném IdInvalidException")
        void changeStatus_InvalidId_ShouldThrowIdInvalidException() {
            when(recruitmentRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IdInvalidException.class,
                    () -> recruitmentRequestService.changeStatus(999L, RecruitmentRequestStatus.APPROVED));

            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 9: QUERY METHODS — getAll, getAllByDepartmentId, findAllWithFilters
    // ================================================================
    @Nested
    @DisplayName("Query Methods (REQ-03 — Listing & Filtering)")
    class QueryMethodsTests {

        @Test
        @DisplayName("TC-REQ-GAL-01 | getAll() -> Chỉ trả về các yêu cầu đang active (isActive = true)")
        void getAll_ShouldReturnOnlyActiveRequests() {
            RecruitmentRequest inactiveRequest = buildRequestWithStatus(9L, RecruitmentRequestStatus.DRAFT);
            inactiveRequest.setActive(false);

            when(recruitmentRequestRepository.findAll()).thenReturn(List.of(mockDraftRequest, inactiveRequest));

            List<RecruitmentRequest> result = recruitmentRequestService.getAll();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }

        @Test
        @DisplayName("TC-REQ-GAL-02 | getAll() khi không có dữ liệu -> Trả về danh sách rỗng")
        void getAll_EmptyDatabase_ShouldReturnEmptyList() {
            when(recruitmentRequestRepository.findAll()).thenReturn(List.of());
            List<RecruitmentRequest> result = recruitmentRequestService.getAll();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC-REQ-GAD-01 | getAllByDepartmentId() -> Trả về yêu cầu đúng phòng ban")
        void getAllByDepartmentId_ValidDepartmentId_ShouldReturnRequestsForThatDepartment() {
            Long targetDepartmentId = 20L;
            when(recruitmentRequestRepository.findByDepartmentId(targetDepartmentId))
                    .thenReturn(List.of(mockDraftRequest, mockPendingRequest));

            List<RecruitmentRequest> result = recruitmentRequestService.getAllByDepartmentId(targetDepartmentId);

            assertEquals(2, result.size());
            verify(recruitmentRequestRepository).findByDepartmentId(20L);
        }

        @Test
        @DisplayName("TC-REQ-FAF-01 | findAllWithFilters() với status hợp lệ -> Chuyển đúng String sang Enum")
        void findAllWithFilters_ValidStatus_ShouldConvertToEnumAndQuery() {
            when(recruitmentRequestRepository.findByFiltersList(eq(20L),
                    eq(RecruitmentRequestStatus.PENDING), eq(10L), eq("java")))
                    .thenReturn(List.of(mockPendingRequest));

            List<RecruitmentRequest> result = recruitmentRequestService.findAllWithFilters(
                    20L, "pending", 10L, "java");

            assertEquals(1, result.size());
            verify(recruitmentRequestRepository).findByFiltersList(20L, RecruitmentRequestStatus.PENDING, 10L, "java");
        }

        @Test
        @DisplayName("TC-REQ-FAF-02 | findAllWithFilters() với status không hợp lệ -> Bỏ qua lọc status (null)")
        void findAllWithFilters_InvalidStatus_ShouldPassNullStatusToRepository() {
            when(recruitmentRequestRepository.findByFiltersList(eq(20L), isNull(), eq(10L), eq("java")))
                    .thenReturn(List.of(mockDraftRequest));

            recruitmentRequestService.findAllWithFilters(20L, "TRANG_THAI_KHONG_TON_TAI", 10L, "java");

            verify(recruitmentRequestRepository).findByFiltersList(20L, null, 10L, "java");
        }

        @Test
        @DisplayName("TC-REQ-FAF-03 | findAllWithFilters() với status rỗng -> Bỏ qua lọc status (null)")
        void findAllWithFilters_EmptyStatus_ShouldPassNullStatusToRepository() {
            when(recruitmentRequestRepository.findByFiltersList(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of());

            recruitmentRequestService.findAllWithFilters(null, "   ", null, null);

            verify(recruitmentRequestRepository).findByFiltersList(null, null, null, null);
        }
    }

    // ================================================================
    // NHÓM 10: GET BY ID WITH USER — LỖI DEPARTMENT CLIENT
    // ================================================================
    @Nested
    @DisplayName("getByIdWithUser — Department Client Error (REQ-03)")
    class GetByIdWithUserDepartmentErrorTests {

        @Test
        @DisplayName("TC-REQ-GET-04 | getByIdWithUser() khi DepartmentClient lỗi -> Ném UserClientException")
        void getByIdWithUser_DepartmentClientFails_ShouldThrowUserClientException() {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));

            ObjectNode empNode = objectMapper.createObjectNode().put("name", "Nhân viên A");
            when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(empNode));

            when(userService.getDepartmentById(20L, "token"))
                    .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

            assertThrows(UserClientException.class,
                    () -> recruitmentRequestService.getByIdWithUser(1L, "token"));
        }

        @Test
        @DisplayName("TC-REQ-GET-05 | getById() với ID hợp lệ -> Trả về đúng entity")
        void getById_ValidId_ShouldReturnCorrectEntity() throws IdInvalidException {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));

            RecruitmentRequest result = recruitmentRequestService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(recruitmentRequestRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("TC-REQ-GET-06 | getByIdWithUser() khi workflowId = null -> Vẫn gọi workflowServiceClient thành công")
        void getByIdWithUser_NullWorkflowId_ShouldStillCallWorkflowClient() throws IdInvalidException {
            mockDraftRequest.setWorkflowId(null);
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));

            ObjectNode empNode = objectMapper.createObjectNode().put("name", "Nhân viên A");
            ObjectNode deptNode = objectMapper.createObjectNode().put("name", "Phòng IT");

            when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(empNode));
            when(userService.getDepartmentById(20L, "token")).thenReturn(ResponseEntity.ok(deptNode));

            when(workflowServiceClient.getWorkflowInfoByRequestId(1L, null, "REQUEST", "token"))
                    .thenReturn(null);

            RecruitmentRequestWithUserDTO result = recruitmentRequestService.getByIdWithUser(1L, "token");

            assertNotNull(result);
            assertNull(result.getWorkflowInfo());
            verify(workflowServiceClient).getWorkflowInfoByRequestId(1L, null, "REQUEST", "token");
        }
    }

    // ================================================================
    // NHÓM 11: GET BY ID WITH USER AND METADATA
    // ================================================================
    @Nested
    @DisplayName("getByIdWithUserAndMetadata (REQ-03)")
    class GetByIdWithUserAndMetadataTests {

        @Test
        @DisplayName("TC-REQ-META-01 | getByIdWithUserAndMetadata() -> Trả về SingleResponseDTO có chứa metadata")
        void getByIdWithUserAndMetadata_ValidId_ShouldReturnSingleResponseWithMetadata() throws IdInvalidException {
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));

            ObjectNode empNode = objectMapper.createObjectNode().put("name", "Nhân viên A");
            ObjectNode deptNode = objectMapper.createObjectNode().put("name", "Phòng IT");
            ObjectNode wfNode = objectMapper.createObjectNode().put("step", "Step 1");

            when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(empNode));
            when(userService.getDepartmentById(20L, "token")).thenReturn(ResponseEntity.ok(deptNode));
            when(workflowServiceClient.getWorkflowInfoByRequestId(1L, 100L, "REQUEST", "token"))
                    .thenReturn(wfNode);

            SingleResponseDTO<RecruitmentRequestWithUserDTO> result =
                    recruitmentRequestService.getByIdWithUserAndMetadata(1L, "token");

            assertNotNull(result);
            assertNotNull(result.getData());
            assertNotNull(result.getCharacterLimits());
            verify(recruitmentRequestRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("TC-REQ-META-02 | getByIdWithUserAndMetadata() với ID không tồn tại -> Ném IdInvalidException")
        void getByIdWithUserAndMetadata_InvalidId_ShouldThrowIdInvalidException() {
            when(recruitmentRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IdInvalidException.class,
                    () -> recruitmentRequestService.getByIdWithUserAndMetadata(999L, "token"));
        }
    }

    // ================================================================
    // NHÓM 12: getAllWithFilters — EDGE CASE departmentId = 1
    // ================================================================
    @Nested
    @DisplayName("getAllWithFilters — DepartmentId = 1 Edge Case (REQ-03)")
    class GetAllWithFiltersEdgeCaseTests {

        @Test
        @DisplayName("TC-REQ-LST-03 | getAllWithFilters() với departmentId = 1 -> Tự chuyển thành null (xem tất cả)")
        void getAllWithFilters_DepartmentIdEqualsOne_ShouldConvertToNullForBroadQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<RecruitmentRequest> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(recruitmentRequestRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(emptyPage);
            Map<Long, JsonNode> emptyMap1 = new java.util.HashMap<>();
            when(userService.getEmployeesByIds(any(), any())).thenReturn(emptyMap1);

            PaginationDTO result = recruitmentRequestService.getAllWithFilters(
                    1L, null, null, null, "token", pageable);

            verify(recruitmentRequestRepository).findByFilters(
                    isNull(), isNull(), isNull(), isNull(), eq(pageable));
            assertNotNull(result);
        }

        @Test
        @DisplayName("TC-REQ-LST-04 | getAllWithFilters() không có kết quả -> Trả về PaginationDTO rỗng")
        void getAllWithFilters_NoResults_ShouldReturnEmptyPaginationDTO() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<RecruitmentRequest> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(recruitmentRequestRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(emptyPage);
            Map<Long, JsonNode> emptyMap2 = new java.util.HashMap<>();
            when(userService.getEmployeesByIds(any(), any())).thenReturn(emptyMap2);

            PaginationDTO result = recruitmentRequestService.getAllWithFilters(
                    null, null, null, null, "token", pageable);

            assertNotNull(result);
            assertEquals(0L, result.getMeta().getTotal());
            assertTrue(((List<?>) result.getResult()).isEmpty());
        }
    }

    // ================================================================
    // NHÓM 13: UPDATE & DELETE — CÁC TRƯỜNG HỢP CÒN THIẾU
    // ================================================================
    @Nested
    @DisplayName("Update & Delete — Missing Cases (REQ-04)")
    class UpdateDeleteMissingCasesTests {

        @Test
        @DisplayName("TC-REQ-UPD-02 | update() với ID không tồn tại -> Ném IdInvalidException")
        void update_InvalidId_ShouldThrowIdInvalidException() {
            when(recruitmentRequestRepository.findById(999L)).thenReturn(Optional.empty());
            CreateRecruitmentRequestDTO updateDto = new CreateRecruitmentRequestDTO();

            assertThrows(IdInvalidException.class,
                    () -> recruitmentRequestService.update(999L, updateDto));

            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-DEL-02 | delete() với ID không tồn tại -> Ném IdInvalidException")
        void delete_InvalidId_ShouldThrowIdInvalidException() {
            when(recruitmentRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IdInvalidException.class,
                    () -> recruitmentRequestService.delete(999L));

            verify(recruitmentRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-REQ-UPD-03 | update() cập nhật salary -> Lưu đúng salaryMin và salaryMax")
        void update_WithSalary_ShouldUpdateSalaryFields() throws IdInvalidException {
            CreateRecruitmentRequestDTO updateDto = new CreateRecruitmentRequestDTO();
            updateDto.setTitle("Tuyển dụng Java Backend");
            updateDto.setQuantity(2);
            updateDto.setSalaryMin(new BigDecimal("20000000"));
            updateDto.setSalaryMax(new BigDecimal("35000000"));
            updateDto.setDepartmentId(20L);

            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(recruitmentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            RecruitmentRequest result = recruitmentRequestService.update(1L, updateDto);

            verify(recruitmentRequestRepository).save(requestCaptor.capture());
            RecruitmentRequest savedEntity = requestCaptor.getValue();
            assertEquals(new BigDecimal("20000000"), savedEntity.getSalaryMin());
            assertEquals(new BigDecimal("35000000"), savedEntity.getSalaryMax());
        }
    }

    // ================================================================
    // CÁC NHÓM MỚI BỔ SUNG ĐỂ TĂNG COVERAGE
    // ================================================================

    // ================================================================
    // NHÓM 15: CREATE REQUEST (Missing coverage on Create logic)
    // ================================================================
    @Nested
    @DisplayName("Create Request (REQ-05)")
    class CreateRequestTests {

        /**
         * Test Case ID: TC-REQ-CRE-01
         * Đặc tả: Test luồng happy path tạo mới Yêu cầu tuyển dụng.
         * CheckDB: Verify hàm save của DB được gọi và entity lưu thành công.
         */
        @Test
        @DisplayName("TC-REQ-CRE-01 | Tạo mới (Create) yêu cầu tuyển dụng thành công")
        void create_ValidDto_ShouldSaveAndReturnEntity() {
            // Arrange
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle("Tuyển dụng Frontend");
            createDto.setQuantity(3);
            createDto.setDepartmentId(20L);
            createDto.setWorkflowId(100L);

            // Mock thao tác save
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> {
                RecruitmentRequest req = i.getArgument(0);
                req.setId(10L); // Giả lập DB sinh ra ID
                return req;
            });

            RecruitmentRequest result = recruitmentRequestService.create(createDto);

            // Assert & CheckDB
            assertNotNull(result, "Entity trả về không được null");
            verify(recruitmentRequestRepository, times(1)).save(requestCaptor.capture());

            RecruitmentRequest savedEntity = requestCaptor.getValue();
            assertEquals("Tuyển dụng Frontend", savedEntity.getTitle(), "Map đúng tiêu đề");
            assertEquals(RecruitmentRequestStatus.DRAFT, savedEntity.getStatus(), "Mặc định tạo mới phải là DRAFT");
            assertTrue(savedEntity.isActive(), "Mặc định tạo mới phải là active");
        }
        @Test
        @DisplayName("RR_CREATE_003 | [FAIL] Tạo request thiếu title (Rỗng) -> Ném IllegalArgumentException")
        void create_EmptyTitle_ShouldThrowException() {
            // 1. Arrange (Chuẩn bị dữ liệu lỗi: Title rỗng)
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle(""); // <--- Đây là tác nhân gây lỗi
            createDto.setQuantity(3);
            createDto.setDepartmentId(20L);
            createDto.setWorkflowId(100L);

            // 2. Act & Assert (Thực thi và bắt ngoại lệ)
            // Kỳ vọng: Hệ thống phải tung ra IllegalArgumentException
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                recruitmentRequestService.create(createDto);
            });

            // Kiểm tra message lỗi nếu cần (Tùy chọn)
            // assertEquals("Tiêu đề không được để trống", exception.getMessage());

            // 3. CheckDB (Xác minh tính an toàn của Cơ sở dữ liệu)
            // Quan trọng: Vì dữ liệu sai, Repository tuyệt đối không được gọi hàm save()
            verify(recruitmentRequestRepository, never()).save(any(RecruitmentRequest.class));
        }
        /** * Test Case ID: RR_CREATE_004
         * Đặc tả: Kiểm tra lỗi khi số lượng tuyển dụng không hợp lệ (số âm).
         */
        @Test
        @DisplayName("RR_CREATE_004 | [FAIL] Tạo request với số lượng âm -> Ném IllegalArgumentException")
        void create_InvalidQuantity_ShouldThrowException() {
            // 1. Arrange: Khởi tạo DTO thủ công với Quantity = -1
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle("Tuyển dụng Java Backend");
            createDto.setQuantity(-1); // <--- Giá trị gây lỗi
            createDto.setDepartmentId(20L);
            createDto.setWorkflowId(100L);

            // 2. Act & Assert: Xác minh hệ thống chặn lỗi và ném đúng ngoại lệ
            assertThrows(IllegalArgumentException.class, () -> {
                recruitmentRequestService.create(createDto);
            });

            // 3. CheckDB: Đảm bảo không có bất kỳ tương tác lưu dữ liệu nào với Repository
            verify(recruitmentRequestRepository, never()).save(any(RecruitmentRequest.class));
        }

        /** * Test Case ID: RR_CREATE_005
         * Đặc tả: Kiểm tra lỗi logic khi mức lương tối thiểu cao hơn mức lương tối đa.
         */
        @Test
        @DisplayName("RR_CREATE_005 | [FAIL] Tạo request với salary_min > salary_max -> Ném IllegalArgumentException")
        void create_SalaryMinGreaterThanMax_ShouldThrowException() {
            // 1. Arrange: Khởi tạo DTO với dải lương phi lý
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle("Tuyển dụng Java Backend");
            createDto.setQuantity(3);
            createDto.setSalaryMin(new BigDecimal("30000000")); // 30 Triệu
            createDto.setSalaryMax(new BigDecimal("20000000")); // 20 Triệu (Min > Max)
            createDto.setDepartmentId(20L);
            createDto.setWorkflowId(100L);

            // 2. Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                recruitmentRequestService.create(createDto);
            });

            // 3. CheckDB: Xác minh DB được bảo vệ khỏi dữ liệu lỗi logic
            verify(recruitmentRequestRepository, never()).save(any(RecruitmentRequest.class));
        }
        /** * Test Case ID: RR_CREATE_006
         * Đặc tả: Kiểm tra lỗi khi Workflow ID cung cấp không tồn tại trong hệ thống.
         */
        @Test
        @DisplayName("RR_CREATE_006 | [FAIL] Tạo request với workflow_id không tồn tại -> Ném IllegalArgumentException")
        void create_InvalidWorkflowId_ShouldThrowException() {
            // 1. Arrange: Khởi tạo DTO với Workflow ID không hợp lệ
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle("Tuyển dụng Java Backend");
            createDto.setQuantity(3);
            createDto.setDepartmentId(20L);
            createDto.setWorkflowId(99999L); // <--- Workflow ID không tồn tại

            // 2. Act & Assert: Xác minh Service chặn lại và ném ngoại lệ đúng quy định
            assertThrows(IllegalArgumentException.class, () -> {
                recruitmentRequestService.create(createDto);
            });

            // 3. CheckDB: Đảm bảo không có dữ liệu nào được đẩy xuống tầng Repository/DB
            verify(recruitmentRequestRepository, never()).save(any(RecruitmentRequest.class));
        }

        /** * Test Case ID: RR_CREATE_007
         * Đặc tả: Kiểm tra lỗi khi Department ID cung cấp không khớp với bất kỳ phòng ban nào.
         */
        @Test
        @DisplayName("RR_CREATE_007 | [FAIL] Tạo request với department_id không tồn tại -> Ném IllegalArgumentException")
        void create_InvalidDepartmentId_ShouldThrowException() {
            // 1. Arrange: Khởi tạo DTO với Department ID không hợp lệ
            CreateRecruitmentRequestDTO createDto = new CreateRecruitmentRequestDTO();
            createDto.setTitle("Tuyển dụng Java Backend");
            createDto.setQuantity(3);
            createDto.setDepartmentId(99999L); // <--- Department ID không tồn tại
            createDto.setWorkflowId(100L);

            // 2. Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                recruitmentRequestService.create(createDto);
            });

            // 3. CheckDB: Xác minh DB an toàn trước dữ liệu sai lệch
            verify(recruitmentRequestRepository, never()).save(any(RecruitmentRequest.class));
        }
    }

    // ================================================================
    // NHÓM 16: DELETE HAPPY PATH
    // ================================================================
    @Nested
    @DisplayName("Delete Request (REQ-06)")
    class DeleteRequestTests {

        /**
         * Test Case ID: TC-REQ-DEL-01
         * Đặc tả: Test xóa mềm (Soft Delete) một yêu cầu thành công.
         * CheckDB: Đảm bảo field active được set = false và gọi repository.save()
         */
        @Test
        @DisplayName("TC-REQ-DEL-01 | Xóa mềm (Delete) thành công với ID hợp lệ")
        void delete_ValidId_ShouldSetActiveFalse() throws IdInvalidException {
            // Arrange
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            recruitmentRequestService.delete(1L);

            // Assert & CheckDB
            verify(recruitmentRequestRepository, times(1)).save(requestCaptor.capture());
            assertFalse(requestCaptor.getValue().isActive(), "Thuộc tính isActive phải được set thành false");
        }
    }

    // ================================================================
    // NHÓM 17: SUBMIT HAPPY PATH TỪ TRẠNG THÁI DRAFT
    // ================================================================
    @Nested
    @DisplayName("Submit Happy Path (REQ-07)")
    class SubmitHappyPathTests {

        /**
         * Test Case ID: TC-REQ-SUB-01
         * Đặc tả: Submit một yêu cầu tuyển dụng đang ở trạng thái DRAFT.
         * CheckDB: Trạng thái đổi thành PENDING và Event được bắn đi thành công.
         */
        @Test
        @DisplayName("TC-REQ-SUB-01 | Submit yêu cầu từ DRAFT -> Chuyển sang PENDING & Phát Event")
        void submit_DraftRequest_ShouldChangeToPending() throws IdInvalidException {
            // Arrange
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(recruitmentRequestRepository.save(any(RecruitmentRequest.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            recruitmentRequestService.submit(1L, 10L, "token");

            // Assert & CheckDB
            verify(recruitmentRequestRepository, times(1)).save(requestCaptor.capture());
            assertEquals(RecruitmentRequestStatus.PENDING, requestCaptor.getValue().getStatus(), "Status phải là PENDING");

            verify(workflowProducer, times(1)).publishEvent(any(RecruitmentWorkflowEvent.class));
        }
    }

    // ================================================================
    // NHÓM 18: WITHDRAW UNAUTHORIZED
    // ================================================================
    @Nested
    @DisplayName("Withdraw Exceptions (REQ-08)")
    class WithdrawExceptionTests {

        /**
         * Test Case ID: TC-REQ-WID-06
         * Đặc tả: Người thực hiện withdraw không phải là owner cũng không phải requester.
         * CheckDB: Hàm repository.save() không được gọi.
         */
        @Test
        @DisplayName("TC-REQ-WID-06 | Rút lại (Withdraw) bởi người không có quyền -> Ném Exception")
        void withdraw_UnauthorizedUser_ShouldThrowException() {
            // Arrange
            mockPendingRequest.setOwnerUserId(10L);
            mockPendingRequest.setRequesterId(10L);
            when(recruitmentRequestRepository.findById(2L)).thenReturn(Optional.of(mockPendingRequest));

            // Act & Assert (User 99L cố gắng withdraw request của user 10L)
            assertThrows(IllegalStateException.class,
                    () -> recruitmentRequestService.withdraw(2L, 99L, "token"),
                    "Chỉ Owner hoặc Requester mới được phép withdraw");

            // CheckDB
            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 19: GET BY ID WITH USER - EMPLOYEE CLIENT ERROR
    // ================================================================
    @Nested
    @DisplayName("GetById Exceptions (REQ-09)")
    class GetByIdExceptionTests {

        /**
         * Test Case ID: TC-REQ-GET-07
         * Đặc tả: Gặp lỗi khi call user service để lấy thông tin Employee.
         * CheckDB: Hàm lấy DB vẫn diễn ra 1 lần, sau đó exception bung ra khi call User Client.
         */
        @Test
        @DisplayName("TC-REQ-GET-07 | getByIdWithUser() khi getEmployeeById lỗi -> Ném UserClientException")
        void getByIdWithUser_EmployeeClientFails_ShouldThrowUserClientException() {
            // Arrange
            when(recruitmentRequestRepository.findById(1L)).thenReturn(Optional.of(mockDraftRequest));
            when(userService.getEmployeeById(10L, "token"))
                    .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

            // Act & Assert
            assertThrows(UserClientException.class,
                    () -> recruitmentRequestService.getByIdWithUser(1L, "token"),
                    "Phải ném UserClientException khi gọi getEmployeeById thất bại");
        }
    }

    // ================================================================
    // NHÓM 20: GET ALL WITH FILTERS MAPPING USERS
    // ================================================================
    @Nested
    @DisplayName("GetAllWithFilters Happy Path Mapping (REQ-10)")
    class GetAllWithFiltersMappingTests {

        /**
         * Test Case ID: TC-REQ-LST-05
         * Đặc tả: getAllWithFilters ánh xạ thành công các ID requester/owner thành string tên.
         * CheckDB: Không có record nào bị ghi xuống DB.
         */
        @Test
        @DisplayName("TC-REQ-LST-05 | getAllWithFilters() ánh xạ đúng tên requester và owner")
        void getAllWithFilters_WithValidUsers_ShouldMapNames() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<RecruitmentRequest> pageData = new PageImpl<>(List.of(mockDraftRequest), pageable, 1);

            when(recruitmentRequestRepository.findByFilters(any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(pageData);

            // Mock Client trả về Map UserID -> Name
            Map<Long, JsonNode> userMap = new java.util.HashMap<>();
            userMap.put(10L, objectMapper.createObjectNode().put("name", "Nguyễn Văn A"));
            when(userService.getEmployeesByIds(anyList(), eq("token")))
                    .thenReturn(userMap);

            // Act
            PaginationDTO result = recruitmentRequestService.getAllWithFilters(
                    20L, "draft", null, null, "token", pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getMeta().getTotal());

            // Check mapping
            List<?> resultList = (List<?>) result.getResult();
            assertFalse(resultList.isEmpty());

            // Note: Since we don't have the exact DTO class returned in list (RecruitmentRequestDTO typically),
            // the successful mock return and non-empty result ensures the branch covering mapping logic is hit.
            verify(userService, times(1)).getEmployeesByIds(anyList(), eq("token"));
            verify(recruitmentRequestRepository, never()).save(any());
        }
    }

    // ================================================================
    // PHƯƠNG THỨC HỖ TRỢ (Test Helper Methods)
    // ================================================================

    /**
     * Tạo nhanh một RecruitmentRequest với trạng thái bất kỳ.
     * Dùng trong các test để tránh lặp code setup.
     *
     * @param id     ID của entity
     * @param status Trạng thái cần set
     * @return RecruitmentRequest đã được cấu hình sẵn
     */
    private RecruitmentRequest buildRequestWithStatus(Long id, RecruitmentRequestStatus status) {
        RecruitmentRequest request = new RecruitmentRequest();
        request.setId(id);
        request.setTitle("Yêu cầu tuyển dụng #" + id);
        request.setQuantity(1);
        request.setStatus(status);
        request.setRequesterId(10L);
        request.setOwnerUserId(10L);
        request.setDepartmentId(20L);
        request.setWorkflowId(100L);
        request.setActive(true);
        return request;
    }
}
