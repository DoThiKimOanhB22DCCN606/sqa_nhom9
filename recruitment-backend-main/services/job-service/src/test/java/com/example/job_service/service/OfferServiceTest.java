package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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
import com.example.job_service.dto.offer.*;
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

/**
 * ============================================================
 * UNIT TEST CHO OFFER SERVICE (MAX COVERAGE & SPEC VALIDATION)
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Offer Service - Comprehensive Specification Suite")
class OfferServiceTest {

    @Mock private OfferRepository offerRepository;
    @Mock private UserClient userService;
    @Mock private OfferWorkflowProducer workflowProducer;
    @Mock private WorkflowClient workflowServiceClient;
    @Mock private CandidateClient candidateClient;
    @Mock private JobPositionService jobPositionService;

    @InjectMocks private OfferService offerService;

    @Captor private ArgumentCaptor<Offer> offerCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ================================================================
    // HELPER METHODS: TẠO MOCK DATA
    // ================================================================
    private CreateOfferDTO buildCreateOfferDto() {
        CreateOfferDTO dto = new CreateOfferDTO();
        dto.setCandidateId(101L);
        dto.setBasicSalary(20_000_000L);
        dto.setProbationSalaryRate(85);
        dto.setOnboardingDate(LocalDate.now().plusDays(10));
        dto.setProbationPeriod(2);
        dto.setNotes("Offer cho ứng viên backend");
        dto.setWorkflowId(900L);
        return dto;
    }

    private UpdateOfferDTO buildUpdateOfferDto() {
        UpdateOfferDTO dto = new UpdateOfferDTO();
        dto.setCandidateId(202L);
        dto.setBasicSalary(25_000_000L);
        dto.setProbationSalaryRate(90);
        dto.setOnboardingDate(LocalDate.now().plusDays(15));
        dto.setProbationPeriod(3);
        dto.setNotes("Đã thương lượng lại lương");
        return dto;
    }

    private Offer buildOffer(Long id, OfferStatus status) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setCandidateId(101L);
        offer.setBasicSalary(20_000_000L);
        offer.setProbationSalaryRate(85);
        offer.setOnboardingDate(LocalDate.now().plusDays(10));
        offer.setProbationPeriod(2);
        offer.setNotes("Offer ban đầu");
        offer.setStatus(status);
        offer.setRequesterId(10L);
        offer.setOwnerUserId(20L);
        offer.setWorkflowId(900L);
        offer.setSubmittedAt(LocalDateTime.now());
        offer.setIsActive(true);
        return offer;
    }

    private JobPosition buildJobPosition(Long departmentId) {
        RecruitmentRequest request = new RecruitmentRequest();
        request.setDepartmentId(departmentId);
        JobPosition jobPosition = new JobPosition();
        jobPosition.setId(501L);
        jobPosition.setTitle("Java Backend Developer");
        jobPosition.setRecruitmentRequest(request);
        return jobPosition;
    }

    private ObjectNode employeeNode(String name) {
        return objectMapper.createObjectNode().put("name", name);
    }

    private ObjectNode employeeWithLevel(String name, String level) {
        ObjectNode positionNode = objectMapper.createObjectNode().put("level", level);
        return objectMapper.createObjectNode().put("name", name).set("position", positionNode);
    }

    private ObjectNode employeeWithPositionName(String name, String positionName) {
        ObjectNode positionNode = objectMapper.createObjectNode().put("name", positionName);
        return objectMapper.createObjectNode().put("name", name).set("position", positionNode);
    }

    private ObjectNode candidateNode(String name) {
        return objectMapper.createObjectNode().put("name", name);
    }

    // ================================================================
    // NHÓM 1: TẠO MỚI (CREATE) & VALIDATION ĐẶC TẢ
    // ================================================================
    @Nested
    @DisplayName("1. Create Offer & Validation")
    class CreateOfferTests {

        @Test
        @DisplayName("TC-OFF-001 | Tạo offer thành công với trạng thái DRAFT mặc định")
        void create_shouldMapInputAndSetDefaultStatus() {
            CreateOfferDTO dto = buildCreateOfferDto();
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
                Offer saved = inv.getArgument(0); saved.setId(1L); return saved;
            });

            Offer result = offerService.create(dto);

            assertAll(
                    () -> assertEquals(1L, result.getId()),
                    () -> assertEquals(20_000_000L, result.getBasicSalary()),
                    () -> assertEquals(OfferStatus.DRAFT, result.getStatus()),
                    () -> assertTrue(result.getIsActive())
            );
            verify(offerRepository, times(1)).save(any(Offer.class));
        }

        @Test
        @DisplayName("TC-OFF-VAL-01 | Lương cơ bản ÂM -> Phải ném ngoại lệ (Bắt bug đăc tả)")
        void create_shouldThrowWhenSalaryIsNegative() {
            CreateOfferDTO dto = buildCreateOfferDto();
            dto.setBasicSalary(-5_000_000L); // Lương âm

            assertThrows(IllegalArgumentException.class, () -> offerService.create(dto),
                    "Hệ thống phải chặn tạo Offer có mức lương âm.");
            verify(offerRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-OFF-VAL-02 | Ngày nhận việc ở QUÁ KHỨ -> Phải ném ngoại lệ (Bắt bug đặc tả)")
        void create_shouldThrowWhenOnboardingDateInPast() {
            CreateOfferDTO dto = buildCreateOfferDto();
            dto.setOnboardingDate(LocalDate.now().minusDays(5)); // Quá khứ

            assertThrows(IllegalArgumentException.class, () -> offerService.create(dto),
                    "Hệ thống phải chặn ngày nhận việc trong quá khứ.");
            verify(offerRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 2: CẬP NHẬT (UPDATE)
    // ================================================================
    @Nested
    @DisplayName("2. Update Offer")
    class UpdateOfferTests {

        @Test
        @DisplayName("TC-OFF-002 | Cập nhật offer DRAFT với đầy đủ thông tin")
        void update_shouldPersistProvidedFieldsWhenDraft() throws Exception {
            Offer existing = buildOffer(1L, OfferStatus.DRAFT);
            UpdateOfferDTO dto = buildUpdateOfferDto();

            when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.update(1L, dto);

            assertEquals(25_000_000L, result.getBasicSalary());
            assertEquals("Đã thương lượng lại lương", result.getNotes());
        }

        @Test
        @DisplayName("TC-OFF-003 | Cập nhật bỏ qua các trường null (Bảo toàn dữ liệu cũ)")
        void update_shouldKeepOldValuesWhenUpdateFieldsAreNull() throws Exception {
            Offer existing = buildOffer(1L, OfferStatus.DRAFT);
            UpdateOfferDTO dto = new UpdateOfferDTO(); // Tất cả đều null

            when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.update(1L, dto);

            assertEquals(20_000_000L, result.getBasicSalary()); // Giữ nguyên lương cũ
        }

        @Test
        @DisplayName("TC-OFF-004 | Từ chối cập nhật khi KHÔNG PHẢI DRAFT")
        void update_shouldRejectWhenNotDraft() {
            when(offerRepository.findById(1L)).thenReturn(Optional.of(buildOffer(1L, OfferStatus.PENDING)));
            assertThrows(IllegalStateException.class, () -> offerService.update(1L, buildUpdateOfferDto()));
        }

        @Test
        @DisplayName("TC-OFF-005 | Báo lỗi khi cập nhật offer không tồn tại")
        void update_shouldThrowWhenNotFound() {
            when(offerRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(IdInvalidException.class, () -> offerService.update(999L, buildUpdateOfferDto()));
        }
    }

    // ================================================================
    // NHÓM 3: LUỒNG TRẠNG THÁI & WORKFLOW
    // ================================================================
    @Nested
    @DisplayName("3. Workflow States (Submit, Approve, Reject, Cancel, Withdraw)")
    class WorkflowTests {

        @Test
        @DisplayName("TC-OFF-006 | Submit offer thành công -> PENDING và phát sự kiện")
        void submit_shouldChangeStatusAndPublishEvent() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            offer.setRequesterId(null);

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.submit(1L, 99L, "token");

            assertEquals(OfferStatus.PENDING, result.getStatus());
            assertEquals(99L, result.getRequesterId());
            verify(workflowProducer).publishEvent(argThat(e -> "REQUEST_SUBMITTED".equals(e.getEventType())));
        }

        @Test
        @DisplayName("TC-OFF-007 | Submit từ chối nếu thiếu WorkflowId")
        void submit_shouldRejectWhenWorkflowIdMissing() {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            offer.setWorkflowId(null);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

            assertThrows(IllegalStateException.class, () -> offerService.submit(1L, 99L, "token"));
        }

        @Test
        @DisplayName("TC-OFF-008 | Approve offer PENDING thành công")
        void approveStep_shouldSaveAndPublishEvent() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(offer)).thenReturn(offer);

            offerService.approveStep(1L, new ApproveOfferDTO(), 30L, "token");
            verify(workflowProducer).publishEvent(argThat(e -> "REQUEST_APPROVED".equals(e.getEventType())));
        }

        @Test
        @DisplayName("TC-OFF-009 | Reject offer PENDING thành công -> REJECTED")
        void rejectStep_shouldUpdateStatusAndPublishEvent() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.rejectStep(1L, new RejectOfferDTO(), 30L, "token");

            assertEquals(OfferStatus.REJECTED, result.getStatus());
            verify(workflowProducer).publishEvent(argThat(e -> "REQUEST_REJECTED".equals(e.getEventType())));
        }

        @Test
        @DisplayName("TC-OFF-010 | Cancel offer DRAFT thành công -> CANCELLED")
        void cancel_shouldCancelDraftOffer() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.cancel(1L, new CancelOfferDTO(), 45L, "token");

            assertEquals(OfferStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("TC-OFF-011 | Cancel từ chối nếu offer đã APPROVED")
        void cancel_shouldRejectApprovedOffer() {
            when(offerRepository.findById(1L)).thenReturn(Optional.of(buildOffer(1L, OfferStatus.APPROVED)));
            assertThrows(IllegalStateException.class, () -> offerService.cancel(1L, new CancelOfferDTO(), 45L, "token"));
        }

        @Test
        @DisplayName("TC-OFF-012 | Withdraw offer PENDING bởi Owner -> WITHDRAWN")
        void withdraw_shouldAllowOwnerToWithdraw() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.withdraw(1L, new WithdrawOfferDTO(), 20L, "token"); // 20L là Owner

            assertEquals(OfferStatus.WITHDRAWN, result.getStatus());
        }

        @Test
        @DisplayName("TC-OFF-013 | Return Offer luôn ném lỗi vì không còn hỗ trợ")
        void returnOffer_shouldThrowException() {
            assertThrows(IllegalStateException.class, () -> offerService.returnOffer(1L, new ReturnOfferDTO(), 30L, "token"));
        }
    }

    // ================================================================
    // NHÓM 4: TRUY XUẤT CHI TIẾT (GET BY ID & ENRICHMENT)
    // ================================================================
    @Nested
    @DisplayName("4. Get & Data Enrichment")
    class GetOfferTests {

        @Test
        @DisplayName("TC-OFF-014 | Tìm offer theo ID thành công")
        void findById_shouldReturnOffer() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            assertSame(offer, offerService.findById(1L));
        }

        @Test
        @DisplayName("TC-OFF-015 | Lấy offer kèm thông tin User và Metadata (SingleResponseDTO)")
        void getByIdWithUserAndMetadata_shouldWrapResponse() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

            // Mocks cho Enrichment
            when(userService.getEmployeeById(10L, "token"))
                    .thenReturn(ResponseEntity.ok(employeeNode("User A")), ResponseEntity.ok(employeeWithLevel("User A", "Senior")));
            when(candidateClient.getCandidateById(101L, "token")).thenReturn(ResponseEntity.ok(candidateNode("Candidate B")));

            SingleResponseDTO<OfferWithUserDTO> result = offerService.getByIdWithUserAndMetadata(1L, "token");
            assertEquals(1L, result.getData().getId());
        }

        @Test
        @DisplayName("TC-OFF-016 | getByIdDetail lấy đầy đủ Requester, Candidate, Dept, Workflow")
        void getByIdDetail_shouldPopulateDetailFields() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            ObjectNode candidate = candidateNode("Tran B"); candidate.put("jobPositionId", 501L);
            ObjectNode dept = objectMapper.createObjectNode().put("name", "Engineering");

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(employeeNode("Nguyen A")), ResponseEntity.ok(employeeWithLevel("Nguyen A", "Lead")));
            when(candidateClient.getCandidateById(101L, "token")).thenReturn(ResponseEntity.ok(candidate));
            when(jobPositionService.findById(501L)).thenReturn(buildJobPosition(301L));
            when(userService.getDepartmentById(301L, "token")).thenReturn(ResponseEntity.ok(dept));

            OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

            assertEquals("Nguyen A", result.getRequesterName());
            assertEquals("Tran B", result.getCandidateName());
            assertEquals("Engineering", result.getDepartmentName());
            assertEquals("Lead", result.getLevelName());
        }

        @Test
        @DisplayName("TC-OFF-017 | Bỏ qua lỗi Enrichment phụ (Candidate Client sập) không làm sập API")
        void getByIdDetail_shouldIgnoreLookupErrors() throws Exception {
            when(offerRepository.findById(1L)).thenReturn(Optional.of(buildOffer(1L, OfferStatus.DRAFT)));
            when(candidateClient.getCandidateById(anyLong(), anyString())).thenThrow(new RuntimeException("Service Down"));

            OfferDetailDTO result = offerService.getByIdDetail(1L, "token");
            assertEquals(1L, result.getId());
            assertNull(result.getCandidateName());
        }

        @Test
        @DisplayName("TC-OFF-018 | Ném UserClientException khi lấy Requester lỗi")
        void getByIdWithUser_shouldThrowIfRequesterLookupFails() {
            when(offerRepository.findById(1L)).thenReturn(Optional.of(buildOffer(1L, OfferStatus.DRAFT)));
            when(userService.getEmployeeById(10L, "token"))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(employeeNode("error")));

            assertThrows(UserClientException.class, () -> offerService.getByIdWithUser(1L, "token"));
        }
    }

    // ================================================================
    // NHÓM 5: DANH SÁCH & FILTER
    // ================================================================
    @Nested
    @DisplayName("5. Listing & Filtering")
    class FilterOfferTests {

        @Test
        @DisplayName("TC-OFF-019 | Lấy danh sách phân trang và lọc trạng thái hợp lệ")
        void getAllWithFilters_shouldReturnPaginatedOffers() {
            Pageable pageable = PageRequest.of(0, 10);
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            Page<Offer> page = new PageImpl<>(List.of(offer), pageable, 1);

            ObjectNode requesterNode = employeeNode("User A");
            ObjectNode employeeWithLevelNode = employeeWithLevel("User A", "Senior");

            when(offerRepository.findByFilters(eq(OfferStatus.PENDING), eq(10L), eq("key"), eq(pageable))).thenReturn(page);
            // Mock batch lookup cho meta
            Map<Long, JsonNode> employeeMap = new java.util.HashMap<>();
            employeeMap.put(10L, requesterNode);
            when(userService.getEmployeesByIds(List.of(10L), "token")).thenReturn(employeeMap);
            // Mock single lookup cho convertToWithUserDTO (requester + level)
            when(userService.getEmployeeById(10L, "token"))
                    .thenReturn(ResponseEntity.ok(requesterNode), ResponseEntity.ok(employeeWithLevelNode));
            when(candidateClient.getCandidateById(101L, "token"))
                    .thenReturn(ResponseEntity.ok(candidateNode("Candidate B")));
            when(workflowServiceClient.getWorkflowInfoByRequestId(eq(1L), eq(900L), eq("OFFER"), eq("token")))
                    .thenReturn(null);

            PaginationDTO result = offerService.getAllWithFilters("PENDING", 10L, "key", "token", pageable);

            assertEquals(1L, result.getMeta().getTotal());
            List<OfferWithUserDTO> items = (List<OfferWithUserDTO>) result.getResult();
            // Requester được override bởi batch map
            assertEquals("User A", items.get(0).getRequester().get("name").asText());
        }

        @Test
        @DisplayName("TC-OFF-020 | Bỏ qua Status rác khi gọi getAllWithFilters")
        void getAllWithFilters_shouldIgnoreInvalidStatus() {
            Pageable p = PageRequest.of(0, 5);
            when(offerRepository.findByFilters(eq(null), any(), any(), eq(p))).thenReturn(Page.empty(p));

            offerService.getAllWithFilters("NOT_A_STATUS", null, null, "token", p);
            verify(offerRepository).findByFilters(isNull(), isNull(), isNull(), eq(p));
        }

        @Test
        @DisplayName("TC-OFF-021 | Tìm danh sách (List) với bộ lọc đẩy đủ")
        void findAllWithFilters_shouldForwardAllFilters() {
            offerService.findAllWithFilters("PENDING", 101L, 900L, 20L, 15_000_000L, 30_000_000L, null, null, "backend");
            verify(offerRepository).findByFiltersList(eq(OfferStatus.PENDING), eq(101L), eq(900L), eq(20L), eq(15_000_000L), eq(30_000_000L), isNull(), isNull(), eq("backend"));
        }
    }

    // ================================================================
    // NHÓM 6: XÓA (DELETE)
    // ================================================================
    @Nested
    @DisplayName("6. Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("TC-OFF-022 | Xóa mềm offer (isActive = false)")
        void delete_shouldSoftDeleteOffer() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = offerService.delete(1L);

            assertTrue(result);
            assertFalse(offer.getIsActive());
            verify(offerRepository).save(offer);
        }

        @Test
        @DisplayName("TC-OFF-023 | Báo lỗi khi xóa offer không tồn tại")
        void delete_shouldThrowWhenNotFound() {
            when(offerRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(IdInvalidException.class, () -> offerService.delete(999L));
        }
    }

    // ================================================================
    // NHÓM 7: EXTENDED VALIDATION & EDGE CASES (BỔ SUNG)
    // ================================================================
    @Nested
    @DisplayName("7. Extended Validation & Edge Cases")
    class ExtendedValidationTests {

        @Test
        @DisplayName("TC-OFF-024 | Update từ chối mức lương cơ bản ÂM")
        void update_shouldThrowWhenSalaryIsNegative() {
            Offer existing = buildOffer(1L, OfferStatus.DRAFT);
            UpdateOfferDTO dto = buildUpdateOfferDto();
            dto.setBasicSalary(-10_000L); // Cố tình truyền lương âm

            when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class, () -> offerService.update(1L, dto),
                    "Phải chặn việc cập nhật mức lương nhỏ hơn 0");
            verify(offerRepository, never()).save(any(Offer.class));
        }

        @Test
        @DisplayName("TC-OFF-025 | Update từ chối ngày nhận việc trong quá khứ")
        void update_shouldThrowWhenOnboardingDateInPast() {
            Offer existing = buildOffer(1L, OfferStatus.DRAFT);
            UpdateOfferDTO dto = buildUpdateOfferDto();
            dto.setOnboardingDate(LocalDate.now().minusDays(1)); // Ngày hôm qua

            when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class, () -> offerService.update(1L, dto),
                    "Phải chặn việc cập nhật ngày Onboarding trong quá khứ");
            verify(offerRepository, never()).save(any(Offer.class));
        }

        @Test
        @DisplayName("TC-OFF-026 | Approve từ chối thao tác khi offer KHÔNG PHẢI trạng thái PENDING")
        void approveStep_shouldThrowWhenNotPending() {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT); // Đang DRAFT không thể duyệt
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

            assertThrows(IllegalStateException.class,
                    () -> offerService.approveStep(1L, new ApproveOfferDTO(), 30L, "token"));
        }

        @Test
        @DisplayName("TC-OFF-027 | Reject từ chối thao tác khi offer KHÔNG PHẢI trạng thái PENDING")
        void rejectStep_shouldThrowWhenNotPending() {
            Offer offer = buildOffer(1L, OfferStatus.APPROVED); // Đã duyệt không thể từ chối bước duyệt
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

            assertThrows(IllegalStateException.class,
                    () -> offerService.rejectStep(1L, new RejectOfferDTO(), 30L, "token"));
        }

        @Test
        @DisplayName("TC-OFF-028 | Withdraw từ chối nếu người thao tác KHÔNG PHẢI Owner/Requester")
        void withdraw_shouldThrowWhenUnauthorized() {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            offer.setOwnerUserId(10L);
            offer.setRequesterId(10L);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

            // Actor là 99L (không phải 10L)
            assertThrows(IllegalStateException.class,
                    () -> offerService.withdraw(1L, new WithdrawOfferDTO(), 99L, "token"),
                    "Chỉ Owner hoặc Requester mới có quyền rút Offer");
        }

        @Test
        @DisplayName("TC-OFF-029 | Submit tự động gán Owner ID bằng Actor ID nếu chưa có (null)")
        void submit_shouldSetOwnerIdIfNull() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            offer.setOwnerUserId(null); // Giả lập việc Offer chưa được phân công Owner

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class))).thenAnswer(i -> i.getArgument(0));

            Offer result = offerService.submit(1L, 55L, "token"); // Actor thực hiện là 55L

            assertEquals(55L, result.getOwnerUserId(),
                    "Hệ thống phải tự động nhận diện Actor làm Owner nếu đang bị null");
        }
    }

    // ================================================================
    // NHÓM 8: EXTENDED ENRICHMENT & WORKFLOW FAILURES (BỔ SUNG)
    // ================================================================
    @Nested
    @DisplayName("8. Extended Enrichment & State Manipulations")
    class ExtendedEnrichmentTests {

        @Test
        @DisplayName("TC-OFF-030 | getByIdDetail xử lý an toàn khi Candidate không gắn JobPosition")
        void getByIdDetail_shouldHandleNullJobPositionId() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.PENDING);
            ObjectNode candidate = candidateNode("Tran B");
            // Cố tình không thêm trường "jobPositionId" vào candidate JSON

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(userService.getEmployeeById(10L, "token"))
                    .thenReturn(ResponseEntity.ok(employeeNode("Nguyen A")), ResponseEntity.ok(employeeWithLevel("Nguyen A", "Lead")));
            when(candidateClient.getCandidateById(101L, "token"))
                    .thenReturn(ResponseEntity.ok(candidate));

            OfferDetailDTO result = offerService.getByIdDetail(1L, "token");

            assertEquals("Nguyen A", result.getRequesterName());
            assertEquals("Tran B", result.getCandidateName());
            assertNull(result.getDepartmentName(), "Do thiếu jobPositionId, không gọi được Department name nên phải trả về null");
            verify(jobPositionService, never()).findById(anyLong()); // Tối ưu không call service
        }
    }
    // ================================================================
    // NHÓM 9: BRANCH COVERAGE TUNING (TĂNG ĐỘ PHỦ NHÁNH)
    // ================================================================
    @Nested
    @DisplayName("9. Branch Coverage Optimization")
    class BranchCoverageTuningTests {

        @Test
        @DisplayName("TC-OFF-BR-01 | convertToWithUserDTO - Nhánh Level Name lấy từ position.name (khi thiếu level)")
        void convertToWithUserDTO_shouldPickPositionNameWhenLevelIsMissing() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            // JSON chỉ có position.name, không có position.level
            ObjectNode employee = employeeWithPositionName("Nguyen Văn A", "Technical Leader");

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(userService.getEmployeeById(10L, "token")).thenReturn(ResponseEntity.ok(employee));
            when(candidateClient.getCandidateById(anyLong(), anyString())).thenReturn(ResponseEntity.ok(candidateNode("Candidate")));

            OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");
            assertEquals("Technical Leader", result.getLevelName(), "Phải lấy name của position khi level bị thiếu");
        }

        @Test
        @DisplayName("TC-OFF-BR-02 | convertToWithUserDTO - Nhánh Department thiếu field 'name' trong JSON")
        void convertToWithUserDTO_shouldHandleDepartmentWithoutNameField() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            ObjectNode candidate = candidateNode("Tran B");
            candidate.put("jobPositionId", 501L);
            // JSON Department rỗng, không có key "name"
            ObjectNode deptEmpty = objectMapper.createObjectNode();

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(userService.getEmployeeById(anyLong(), anyString())).thenReturn(ResponseEntity.ok(employeeNode("User")));
            when(candidateClient.getCandidateById(101L, "token")).thenReturn(ResponseEntity.ok(candidate));
            when(jobPositionService.findById(501L)).thenReturn(buildJobPosition(301L));
            when(userService.getDepartmentById(301L, "token")).thenReturn(ResponseEntity.ok(deptEmpty));

            OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");
            assertNull(result.getDepartmentName(), "DepartmentName phải là null nếu JSON không có key 'name'");
        }

        @Test
        @DisplayName("TC-OFF-BR-03 | convertToWithUserDTO - Nhánh JobPosition không có RecruitmentRequest (Null lồng)")
        void convertToWithUserDTO_shouldHandleNullRecruitmentRequestInJobPosition() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            ObjectNode candidate = candidateNode("Tran B");
            candidate.put("jobPositionId", 501L);

            // JobPosition không có recruitmentRequest
            JobPosition jobNoReq = new JobPosition();
            jobNoReq.setId(501L);
            jobNoReq.setTitle("No Request Job");

            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            when(userService.getEmployeeById(anyLong(), anyString())).thenReturn(ResponseEntity.ok(employeeNode("User")));
            when(candidateClient.getCandidateById(101L, "token")).thenReturn(ResponseEntity.ok(candidate));
            when(jobPositionService.findById(501L)).thenReturn(jobNoReq);

            OfferWithUserDTO result = offerService.getByIdWithUser(1L, "token");
            assertNull(result.getDepartmentName());
            verify(userService, never()).getDepartmentById(anyLong(), anyString());
        }

        @Test
        @DisplayName("TC-OFF-BR-04 | convertToWithUserDTOList - Nhánh filter RequesterId is Null")
        void convertToWithUserDTOList_shouldFilterOutNullRequesters() {
            Pageable pageable = PageRequest.of(0, 10);
            Offer o1 = buildOffer(1L, OfferStatus.DRAFT); o1.setRequesterId(10L);
            Offer o2 = buildOffer(2L, OfferStatus.DRAFT); o2.setRequesterId(null); // Nhánh filter
            Page<Offer> page = new PageImpl<>(List.of(o1, o2), pageable, 2);

            when(offerRepository.findByFilters(any(), any(), any(), eq(pageable))).thenReturn(page);
            when(userService.getEmployeesByIds(List.of(10L), "token")).thenReturn(Map.of(10L, employeeNode("User")));

            // Mock cho convertToWithUserDTO (o1 và o2)
            when(userService.getEmployeeById(anyLong(), eq("token"))).thenReturn(ResponseEntity.ok(employeeNode("U")));
            when(candidateClient.getCandidateById(anyLong(), anyString())).thenReturn(ResponseEntity.ok(candidateNode("C")));

            PaginationDTO result = offerService.getAllWithFilters(null, null, null, "token", pageable);

            assertNotNull(result);
            verify(userService).getEmployeesByIds(argThat(list -> list.size() == 1), anyString());
        }

        @Test
        @DisplayName("TC-OFF-BR-05 | update - Phủ kín các nhánh if check null từng trường")
        void update_shouldCoverPartialUpdateBranches() throws Exception {
            Offer existing = buildOffer(1L, OfferStatus.DRAFT);
            UpdateOfferDTO dto = new UpdateOfferDTO();
            dto.setCandidateId(303L); // Chỉ set 1 trường, các trường khác null
            dto.setProbationPeriod(6); // Set thêm 1 trường khác

            when(offerRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

            Offer result = offerService.update(1L, dto);

            assertEquals(303L, result.getCandidateId());
            assertEquals(6, result.getProbationPeriod());
            assertEquals(20_000_000L, result.getBasicSalary(), "Lương cũ phải được giữ nguyên (nhánh if null)");
        }
    }

    // ================================================================
    // NHÓM 10: EDGE CASE VALIDATION (MAX COVERAGE)
    // ================================================================
    @Nested
    @DisplayName("10. Extreme Edge Cases")
    class ExtremeEdgeCaseTests {

        @Test
        @DisplayName("TC-OFF-EXT-01 | convertToDetailDTO - Nhánh Requester/Owner sập khi lấy Level")
        void convertToDetailDTO_shouldHandleEmployeeServiceFailureForLevel() throws Exception {
            Offer offer = buildOffer(1L, OfferStatus.DRAFT);
            when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
            // Employee Client ném lỗi khi lấy level
            when(userService.getEmployeeById(10L, "token")).thenThrow(new RuntimeException("API Down"));

            OfferDetailDTO result = offerService.getByIdDetail(1L, "token");
            assertNull(result.getLevelName(), "LevelName phải null thay vì làm sập API");
        }

        @Test
        @DisplayName("TC-OFF-EXT-02 | getAllWithFilters - Nhánh Exception khi parse Enum")
        void getAllWithFilters_shouldHandleEmptyKeywordAndStatus() {
            // Arrange
            Pageable p = PageRequest.of(0, 5);

            // SỬA TẠI ĐÂY: Thay isNull() thứ 3 bằng eq("") hoặc any()
            // vì "" (chuỗi rỗng) không phải là null
            when(offerRepository.findByFilters(isNull(), isNull(), eq(""), eq(p)))
                    .thenReturn(Page.empty(p));

            // Act: Gọi hàm với keyword là ""
            offerService.getAllWithFilters("   ", null, "", "token", p);

            // Assert & CheckDB: Sửa verify tương ứng để khớp với lời gọi
            verify(offerRepository).findByFilters(isNull(), isNull(), eq(""), eq(p));
        }
    }
}
