package com.example.workflow_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

/**
 * ============================================================
 * UNIT TEST: WORKFLOW SERVICE
 *
 * ÁNH XẠ ĐẶC TẢ:
 * - WF-01: Cấu hình luồng phê duyệt (create, update, delete).
 * - WF-02: Khởi tạo Approval Tracking (liên quan đến trạng thái Active/Inactive).
 * - WF-STEP: Định nghĩa bước duyệt với hierarchyOrder hợp lệ.
 * - WF-STATUS: Quản lý trạng thái Active/Inactive của Workflow.
 *
 * QUY ƯỚC:
 * - PASS test: Kiểm tra happy path theo đúng đặc tả.
 * - FAIL test: Kiểm tra các trường hợp vi phạm đặc tả -> phải ném ngoại lệ.
 * - CheckDB  : verify(repository).save(captor) kiểm tra entity được lưu đúng.
 * - Rollback : @ExtendWith(MockitoExtension) reset toàn bộ Mock sau mỗi test.
 *              MockedStatic được đóng trong @AfterEach để đảm bảo không rò rỉ.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowService — Unit Test Suite")
class WorkflowServiceTest {

    // ----------------------------------------------------------------
    // MOCK DEPENDENCIES
    // ----------------------------------------------------------------
    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowStepRepository workflowStepRepository;
    @Mock private UserService userService;

    @InjectMocks private WorkflowService workflowService;

    // Captor bắt đối số được truyền vào repository để kiểm tra CheckDB
    @Captor private ArgumentCaptor<Workflow> workflowCaptor;
    @Captor private ArgumentCaptor<List<WorkflowStep>> stepsCaptor;

    /**
     * MockedStatic cho SecurityUtil — cần đóng lại sau mỗi test.
     * Khai báo ở đây để @AfterEach có thể truy cập.
     * ROLLBACK: close() trong @AfterEach đảm bảo không ảnh hưởng sang test khác.
     */
    private MockedStatic<SecurityUtil> mockedSecurityUtil;

    // Dữ liệu nền dùng chung
    private Workflow mockActiveWorkflow;      // Workflow đang Active
    private Workflow mockInactiveWorkflow;    // Workflow đã Inactive
    private WorkflowStep mockStep1;           // Bước 1: Trưởng phòng (hierarchyOrder=3)
    private WorkflowStep mockStep2;           // Bước 2: Giám đốc    (hierarchyOrder=1)

    @BeforeEach
    void setUp() {
        // Mock SecurityUtil (static) — trả về token và employeeId giả
        mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::getCurrentUserJWT)
                .thenReturn(Optional.of("mock-jwt-token"));
        mockedSecurityUtil.when(SecurityUtil::extractEmployeeId)
                .thenReturn(99L); // Admin ID = 99

        // --- Workflow Active (dùng trong hầu hết test) ---
        mockActiveWorkflow = new Workflow();
        mockActiveWorkflow.setId(1L);
        mockActiveWorkflow.setName("Quy trình duyệt tuyển dụng");
        mockActiveWorkflow.setDescription("Duyệt yêu cầu tuyển dụng nhân sự");
        mockActiveWorkflow.setType(WorkflowType.REQUEST);
        mockActiveWorkflow.setDepartmentId(10L);
        mockActiveWorkflow.setIsActive(true);
        mockActiveWorkflow.setCreatedBy(99L);
        mockActiveWorkflow.setCreatedAt(LocalDateTime.now().minusDays(1));
        mockActiveWorkflow.setSteps(new HashSet<>());

        // --- Workflow Inactive ---
        mockInactiveWorkflow = new Workflow();
        mockInactiveWorkflow.setId(2L);
        mockInactiveWorkflow.setName("Quy trình cũ - đã tắt");
        mockInactiveWorkflow.setIsActive(false);
        mockInactiveWorkflow.setSteps(new HashSet<>());

        // --- Bước 1: Trưởng phòng — hierarchyOrder = 3 (level thấp hơn) ---
        mockStep1 = new WorkflowStep();
        mockStep1.setId(101L);
        mockStep1.setStepOrder(1);
        mockStep1.setApproverPositionId(201L); // Position ID của Trưởng phòng
        mockStep1.setIsActive(true);
        mockStep1.setWorkflow(mockActiveWorkflow);

        // --- Bước 2: Giám đốc — hierarchyOrder = 1 (level cao hơn) ---
        mockStep2 = new WorkflowStep();
        mockStep2.setId(102L);
        mockStep2.setStepOrder(2);
        mockStep2.setApproverPositionId(202L); // Position ID của Giám đốc
        mockStep2.setIsActive(true);
        mockStep2.setWorkflow(mockActiveWorkflow);
    }

    /**
     * ROLLBACK: Đóng MockedStatic sau mỗi test để tránh rò rỉ trạng thái mock
     * sang các test khác trong cùng JVM thread.
     */
    @AfterEach
    void tearDown() {
        if (mockedSecurityUtil != null) {
            mockedSecurityUtil.close();
        }
    }

    // ================================================================
    // NHÓM 1: TẠO WORKFLOW — create() (WF-01)
    // ================================================================
    @Nested
    @DisplayName("WF-01 | Tạo Workflow — create()")
    class CreateWorkflowTests {

        /**
         * Test Case ID: TC-WF-CRT-01
         * Đặc tả (WF-01): Admin tạo mới workflow thành công, không có bước duyệt.
         * Trạng thái mặc định phải là Active (isActive = true).
         *
         * CheckDB: Verify workflow được lưu với đúng thông tin và isActive = true.
         * Rollback: Mock reset bởi @ExtendWith + mockedSecurityUtil.close().
         */
        @Test
        @DisplayName("  TC-WF-CRT-01 | Tạo workflow không có steps -> isActive=true, lưu DB đúng")
        void create_ValidWorkflowWithoutSteps_ShouldSaveWithActiveStatusTrue() {
            // Arrange — DTO tạo workflow không có steps
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Quy trình duyệt tuyển dụng",
                    "Mô tả quy trình",
                    WorkflowType.REQUEST,
                    10L,
                    null // Không có steps
            );

            when(workflowRepository.findByName("Quy trình duyệt tuyển dụng"))
                    .thenReturn(Optional.empty()); // Tên chưa tồn tại
            when(workflowRepository.save(any(Workflow.class))).thenReturn(mockActiveWorkflow);

            // Mock userService trả về map rỗng (không có steps -> không gọi)
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            WorkflowResponseDTO result = workflowService.create(createDto);

            // Assert — kết quả trả về đúng
            assertNotNull(result, "Kết quả không được null");
            assertEquals("Quy trình duyệt tuyển dụng", result.getName());
            assertTrue(result.getIsActive(), "Workflow mới tạo phải ở trạng thái Active (isActive=true)");

            // CheckDB: Verify workflow được lưu với isActive=true và createdBy đúng
            verify(workflowRepository).save(workflowCaptor.capture());
            Workflow savedWorkflow = workflowCaptor.getValue();
            assertTrue(savedWorkflow.getIsActive(),
                    "isActive trong DB phải là true khi tạo mới");
            assertEquals(99L, savedWorkflow.getCreatedBy(),
                    "createdBy phải là ID của Admin đang đăng nhập");

            // CheckDB: Không lưu steps vì không có steps
            verify(workflowStepRepository, never()).saveAll(any());
        }

        /**
         * Test Case ID: TC-WF-CRT-02
         * Đặc tả (WF-01, WF-STEP): Admin tạo workflow với các bước duyệt hợp lệ.
         * Các bước phải có hierarchyOrder GIẢM DẦN theo stepOrder
         * (Bước 1: Trưởng phòng hierarchyOrder=3, Bước 2: Giám đốc hierarchyOrder=1).
         *
         * CheckDB: Verify workflowStepRepository.saveAll() được gọi với đúng số steps.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName("  TC-WF-CRT-02 | Tạo workflow có steps hợp lệ (hierarchyOrder giảm dần) -> Lưu cả workflow và steps")
        void create_WorkflowWithValidHierarchyOrderSteps_ShouldSaveWorkflowAndAllSteps() {
            // Arrange — 2 bước: Trưởng phòng (order=3) -> Giám đốc (order=1), hợp lệ
            List<CreateStepDTO> validSteps = List.of(
                    buildCreateStepDTO(1, 201L), // Bước 1: Trưởng phòng, hierarchyOrder=3
                    buildCreateStepDTO(2, 202L)  // Bước 2: Giám đốc, hierarchyOrder=1
            );
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Quy trình duyệt tuyển dụng", "Mô tả", WorkflowType.REQUEST, 10L, validSteps);

            when(workflowRepository.findByName(any())).thenReturn(Optional.empty());
            when(workflowRepository.save(any(Workflow.class))).thenReturn(mockActiveWorkflow);

            // Mock: hierarchyOrder giảm dần (3 -> 1) = hợp lệ
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of(
                            201L, 3, // Trưởng phòng = level 3 (thấp hơn)
                            202L, 1  // Giám đốc    = level 1 (cao hơn)
                    ));
            when(userService.getPositionNamesByIds(anyList(), any()))
                    .thenReturn(Map.of(201L, "Trưởng phòng", 202L, "Giám đốc"));

            // Act
            WorkflowResponseDTO result = workflowService.create(createDto);

            // Assert
            assertNotNull(result);

            // CheckDB: workflowStepRepository phải được gọi để lưu steps
            verify(workflowStepRepository).saveAll(stepsCaptor.capture());
            List<WorkflowStep> savedSteps = stepsCaptor.getValue();
            assertEquals(2, savedSteps.size(),
                    "Phải lưu đúng 2 steps xuống DB");
            assertTrue(savedSteps.stream().allMatch(WorkflowStep::getIsActive),
                    "Tất cả steps mới tạo phải ở trạng thái active");
        }

        /**
         * Test Case ID: TC-WF-CRT-03
         * Đặc tả (WF-01): Không được tạo workflow với tên đã tồn tại trong hệ thống.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi khi tên trùng.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName(" TC-WF-CRT-03 | Tạo workflow với tên đã tồn tại -> Ném CustomException")
        void create_DuplicateWorkflowName_ShouldThrowCustomException() {
            // Arrange — tên đã tồn tại trong DB
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Quy trình duyệt tuyển dụng", null, WorkflowType.REQUEST , 10L, null);

            when(workflowRepository.findByName("Quy trình duyệt tuyển dụng"))
                    .thenReturn(Optional.of(mockActiveWorkflow)); // Tên đã tồn tại!

            // Act & Assert
            CustomException thrownException = assertThrows(CustomException.class,
                    () -> workflowService.create(createDto),
                    "Phải ném CustomException khi tên workflow đã tồn tại");

            assertTrue(thrownException.getMessage().contains("Quy trình duyệt tuyển dụng"),
                    "Message lỗi phải chứa tên workflow bị trùng");

            // CheckDB: Không lưu gì khi tên trùng
            verify(workflowRepository, never()).save(any());
            verify(workflowStepRepository, never()).saveAll(any());
        }

        /**
         * Test Case ID: TC-WF-CRT-04
         * Đặc tả (WF-STEP): Steps phải có hierarchyOrder GIẢM DẦN theo stepOrder.
         * Nếu bước sau có hierarchyOrder CAO HƠN bước trước (Giám đốc -> Trưởng phòng)
         * thì vi phạm nguyên tắc "từ level thấp lên cao" -> phải ném ngoại lệ.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi khi validate thất bại.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName(" TC-WF-CRT-04 | Steps có hierarchyOrder tăng dần (sai chiều) -> Ném CustomException")
        void create_StepsWithInvalidAscendingHierarchyOrder_ShouldThrowCustomException() {
            // Arrange — Bước 1: Giám đốc (order=1), Bước 2: Trưởng phòng (order=3) -> SAI!
            // Phải đi từ level thấp đến cao, không được đi ngược lại
            List<CreateStepDTO> invalidSteps = List.of(
                    buildCreateStepDTO(1, 202L), // Bước 1: Giám đốc, hierarchyOrder=1
                    buildCreateStepDTO(2, 201L)  // Bước 2: Trưởng phòng, hierarchyOrder=3 -> SAI!
            );
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Quy trình sai thứ tự", null, WorkflowType.REQUEST , 10L, invalidSteps);

            when(workflowRepository.findByName(any())).thenReturn(Optional.empty());

            // Mock hierarchyOrder: Giám đốc=1, Trưởng phòng=3
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of(
                            202L, 1, // Giám đốc = level 1 (cao nhất)
                            201L, 3  // Trưởng phòng = level 3 (thấp hơn)
                    ));

            // Act & Assert
            CustomException thrownException = assertThrows(CustomException.class,
                    () -> workflowService.create(createDto),
                    "Phải ném CustomException khi hierarchyOrder tăng dần (sai chiều)");

            assertTrue(thrownException.getMessage().contains("hierarchyOrder"),
                    "Message lỗi phải đề cập đến hierarchyOrder");

            // CheckDB: Không lưu gì khi validate thất bại
            verify(workflowRepository, never()).save(any());
            verify(workflowStepRepository, never()).saveAll(any());
        }

        /**
         * Test Case ID: TC-WF-CRT-05
         * Đặc tả (WF-STEP): positionId trong step phải tồn tại trong hệ thống.
         * Nếu userService không trả về hierarchyOrder cho một positionId
         * thì phải ném CustomException.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi.
         */
        @Test
        @DisplayName(" TC-WF-CRT-05 | Step có positionId không tìm thấy hierarchyOrder -> Ném CustomException")
        void create_StepWithUnknownPositionId_ShouldThrowCustomException() {
            // Arrange — positionId=999 không tồn tại trong hệ thống
            List<CreateStepDTO> stepsWithUnknownPosition = List.of(
                    buildCreateStepDTO(1, 999L) // positionId=999 không tồn tại
            );
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Quy trình có position lạ", null, WorkflowType.REQUEST , 10L, stepsWithUnknownPosition);

            when(workflowRepository.findByName(any())).thenReturn(Optional.empty());

            // Mock: userService không trả về hierarchyOrder cho positionId=999
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of()); // Map rỗng = không tìm thấy position

            // Act & Assert
            CustomException thrownException = assertThrows(CustomException.class,
                    () -> workflowService.create(createDto),
                    "Phải ném CustomException khi không tìm thấy hierarchyOrder cho positionId");

            assertTrue(thrownException.getMessage().contains("999"),
                    "Message lỗi phải chứa positionId bị thiếu");

            // CheckDB: Không lưu gì
            verify(workflowRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-WF-CRT-06
         * Đặc tả (WF-01): Tạo workflow với steps = null -> Bỏ qua validate steps,
         * chỉ lưu workflow (không gọi workflowStepRepository).
         *
         * CheckDB: workflowStepRepository.saveAll() KHÔNG được gọi.
         */
        @Test
        @DisplayName("  TC-WF-CRT-06 | Tạo workflow với steps = null -> Chỉ lưu workflow, không tạo steps")
        void create_WorkflowWithNullSteps_ShouldSaveOnlyWorkflowWithoutSteps() {
            // Arrange — steps = null (Admin chưa cấu hình bước duyệt)
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Workflow chưa cấu hình bước", null, WorkflowType.REQUEST , 10L, null);

            when(workflowRepository.findByName(any())).thenReturn(Optional.empty());
            when(workflowRepository.save(any(Workflow.class))).thenReturn(mockActiveWorkflow);
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            WorkflowResponseDTO result = workflowService.create(createDto);

            // Assert
            assertNotNull(result);

            // CheckDB: Chỉ lưu workflow, KHÔNG lưu steps
            verify(workflowRepository, times(1)).save(any(Workflow.class));
            verify(workflowStepRepository, never()).saveAll(any());
        }

        /**
         * Test Case ID: TC-WF-CRT-07
         * Đặc tả (WF-STEP): Workflow chỉ có 1 step -> Không cần so sánh hierarchyOrder,
         * phải tạo thành công.
         *
         * CheckDB: workflowStepRepository.saveAll() được gọi với 1 step.
         */
        @Test
        @DisplayName("  TC-WF-CRT-07 | Tạo workflow chỉ có 1 step -> Hợp lệ, không cần so sánh hierarchy")
        void create_WorkflowWithSingleStep_ShouldCreateSuccessfully() {
            // Arrange — chỉ 1 bước, không cần kiểm tra thứ tự
            List<CreateStepDTO> singleStep = List.of(
                    buildCreateStepDTO(1, 201L)
            );
            CreateWorkflowDTO createDto = buildCreateWorkflowDTO(
                    "Workflow 1 bước", null, WorkflowType.REQUEST , 10L, singleStep);

            when(workflowRepository.findByName(any())).thenReturn(Optional.empty());
            when(workflowRepository.save(any(Workflow.class))).thenReturn(mockActiveWorkflow);
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of(201L, 3));
            when(userService.getPositionNamesByIds(anyList(), any()))
                    .thenReturn(Map.of(201L, "Trưởng phòng"));

            // Act
            WorkflowResponseDTO result = workflowService.create(createDto);

            // Assert
            assertNotNull(result);

            // CheckDB: 1 step được lưu
            verify(workflowStepRepository).saveAll(stepsCaptor.capture());
            assertEquals(1, stepsCaptor.getValue().size(),
                    "Phải lưu đúng 1 step");
        }
    }

    // ================================================================
    // NHÓM 2: LẤY WORKFLOW THEO ID — getById() (WF-01)
    // ================================================================
    @Nested
    @DisplayName("WF-01 | Lấy Workflow theo ID — getById()")
    class GetByIdTests {

        /**
         * Test Case ID: TC-WF-GET-01
         * Đặc tả (WF-01): Lấy workflow theo ID hợp lệ -> Trả về đúng thông tin.
         *
         * CheckDB: workflowRepository.findById() được gọi đúng 1 lần với đúng ID.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName("  TC-WF-GET-01 | getById() với ID hợp lệ -> Trả về WorkflowResponseDTO đúng")
        void getById_ValidWorkflowId_ShouldReturnCorrectWorkflowResponseDTO() {
            // Arrange
            mockActiveWorkflow.setSteps(Set.of(mockStep1, mockStep2));
            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(userService.getPositionNamesByIds(anyList(), any()))
                    .thenReturn(Map.of(201L, "Trưởng phòng", 202L, "Giám đốc"));

            // Act
            WorkflowResponseDTO result = workflowService.getById(1L);

            // Assert
            assertNotNull(result, "Kết quả không được null");
            assertEquals(1L, result.getId(), "ID phải khớp");
            assertEquals("Quy trình duyệt tuyển dụng", result.getName(), "Tên phải khớp");
            assertTrue(result.getIsActive(), "isActive phải là true");
            assertEquals(WorkflowType.REQUEST, result.getType(), "Type phải là REQUEST");
            assertNotNull(result.getSteps(), "Steps không được null");
            assertEquals(2, result.getSteps().size(), "Phải có 2 steps");

            // CheckDB: repository được gọi đúng 1 lần với ID=1
            verify(workflowRepository, times(1)).findById(1L);
        }

        /**
         * Test Case ID: TC-WF-GET-02
         * Đặc tả (WF-01): ID workflow không tồn tại -> Ném IdInvalidException.
         *
         * CheckDB: workflowRepository.findById() được gọi nhưng không tìm thấy.
         */
        @Test
        @DisplayName(" TC-WF-GET-02 | getById() với ID không tồn tại -> Ném IdInvalidException")
        void getById_NonExistentWorkflowId_ShouldThrowIdInvalidException() {
            // Arrange — ID=999 không tồn tại trong DB
            when(workflowRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IdInvalidException thrownException = assertThrows(IdInvalidException.class,
                    () -> workflowService.getById(999L),
                    "Phải ném IdInvalidException khi ID không tồn tại");

            assertTrue(thrownException.getMessage().contains("999"),
                    "Message lỗi phải chứa ID không tìm thấy");

            // CheckDB: repository được gọi đúng 1 lần
            verify(workflowRepository, times(1)).findById(999L);
        }

        /**
         * Test Case ID: TC-WF-GET-03
         * Đặc tả: getById() với workflow không có steps -> Trả về DTO với steps rỗng.
         * (Không crash khi steps = null hoặc rỗng)
         */
        @Test
        @DisplayName("  TC-WF-GET-03 | getById() với workflow không có steps -> Trả về DTO, steps rỗng")
        void getById_WorkflowWithNoSteps_ShouldReturnDTOWithEmptySteps() {
            // Arrange — workflow không có bước duyệt nào
            mockActiveWorkflow.setSteps(new HashSet<>()); // Rỗng
            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            WorkflowResponseDTO result = workflowService.getById(1L);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getSteps(), "Steps không được null, phải là list rỗng");
            assertTrue(result.getSteps().isEmpty(), "Steps phải rỗng");
        }
    }

    // ================================================================
    // NHÓM 3: LẤY DANH SÁCH WORKFLOW — getAll() (WF-01, WF-STATUS)
    // ================================================================
    @Nested
    @DisplayName("WF-01 | Lấy danh sách Workflow — getAll()")
    class GetAllTests {

        /**
         * Test Case ID: TC-WF-LST-01
         * Đặc tả (WF-01): Lấy danh sách tất cả workflow với phân trang.
         * Position names phải được enrich từ UserService.
         *
         * CheckDB: workflowRepository.findByFilters() được gọi đúng 1 lần.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName("  TC-WF-LST-01 | getAll() không filter -> Trả về danh sách có phân trang và position names")
        void getAll_NoFilters_ShouldReturnPaginatedWorkflowListWithPositionNames() {
            // Arrange — 2 workflows trong DB
            mockActiveWorkflow.setSteps(Set.of(mockStep1, mockStep2));
            Pageable pageable = PageRequest.of(0, 10);
            Page<Workflow> workflowPage = new PageImpl<>(
                    List.of(mockActiveWorkflow, mockInactiveWorkflow), pageable, 2);

            when(workflowRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(workflowPage);
            when(userService.getPositionNamesByIds(anyList(), any()))
                    .thenReturn(Map.of(201L, "Trưởng phòng", 202L, "Giám đốc"));

            // Act
            PaginationDTO result = workflowService.getAll(null, null, null, null, pageable);

            // Assert — Meta phân trang đúng
            assertNotNull(result, "Kết quả không được null");
            assertEquals(2L, result.getMeta().getTotal(), "Tổng số phải là 2");
            assertEquals(1, result.getMeta().getPage(), "Trang hiện tại phải là 1");
            assertEquals(10, result.getMeta().getPageSize(), "Page size phải là 10");

            // Assert — Nội dung đúng
            List<?> workflowList = (List<?>) result.getResult();
            assertEquals(2, workflowList.size(), "Phải trả về 2 workflows");

            // CheckDB: repository được gọi đúng 1 lần
            verify(workflowRepository, times(1))
                    .findByFilters(isNull(), isNull(), isNull(), isNull(), eq(pageable));
        }

        /**
         * Test Case ID: TC-WF-LST-02
         * Đặc tả (WF-STATUS): Lọc workflow theo trạng thái Active.
         * Chức năng "Theo dõi tiến trình" chỉ áp dụng cho workflow Active.
         *
         * CheckDB: repository.findByFilters() được gọi với isActive=true.
         */
        @Test
        @DisplayName("  TC-WF-LST-02 | getAll() filter isActive=true -> Chỉ trả về workflows đang Active")
        void getAll_FilterByActiveStatus_ShouldReturnOnlyActiveWorkflows() {
            // Arrange — chỉ trả về workflow Active
            Pageable pageable = PageRequest.of(0, 10);
            Page<Workflow> activeOnlyPage = new PageImpl<>(
                    List.of(mockActiveWorkflow), pageable, 1);

            when(workflowRepository.findByFilters(isNull(), eq(true), isNull(), isNull(), eq(pageable)))
                    .thenReturn(activeOnlyPage);
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            PaginationDTO result = workflowService.getAll(null, true, null, null, pageable);

            // Assert
            assertEquals(1L, result.getMeta().getTotal(),
                    "Chỉ 1 workflow Active được trả về");

            // CheckDB: isActive=true được truyền vào repository
            verify(workflowRepository).findByFilters(isNull(), eq(true), isNull(), isNull(), eq(pageable));
        }

        /**
         * Test Case ID: TC-WF-LST-03
         * Đặc tả (WF-01): Lọc workflow theo type RECRUITMENT.
         *
         * CheckDB: repository.findByFilters() được gọi với type="RECRUITMENT".
         */
        @Test
        @DisplayName("  TC-WF-LST-03 | getAll() filter theo type REQUEST -> Truyền đúng type string vào repository")
        void getAll_FilterByRequestType_ShouldPassCorrectTypeStringToRepository() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Workflow> requestPage = new PageImpl<>(List.of(mockActiveWorkflow), pageable, 1);

            when(workflowRepository.findByFilters(eq("REQUEST"), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(requestPage);
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            PaginationDTO result = workflowService.getAll(WorkflowType.REQUEST, null, null, null, pageable);

            // Assert
            assertNotNull(result);

            // CheckDB: type="REQUEST" được truyền đúng (Enum -> String)
            verify(workflowRepository).findByFilters(
                    eq("REQUEST"), isNull(), isNull(), isNull(), eq(pageable));
        }

        /**
         * Test Case ID: TC-WF-LST-04
         * Đặc tả: Khi workflows không có steps nào -> positionIds rỗng,
         * userService.getPositionNamesByIds() vẫn được gọi với list rỗng (không crash).
         */
        @Test
        @DisplayName("  TC-WF-LST-04 | getAll() với workflows không có steps -> Không crash, trả về danh sách bình thường")
        void getAll_WorkflowsWithNoSteps_ShouldNotCrashAndReturnResults() {
            // Arrange — cả 2 workflows đều không có steps
            mockActiveWorkflow.setSteps(new HashSet<>());
            mockInactiveWorkflow.setSteps(new HashSet<>());

            Pageable pageable = PageRequest.of(0, 10);
            Page<Workflow> page = new PageImpl<>(
                    List.of(mockActiveWorkflow, mockInactiveWorkflow), pageable, 2);

            when(workflowRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(page);
            when(userService.getPositionNamesByIds(eq(List.of()), any()))
                    .thenReturn(Map.of());

            // Act — không được ném exception
            assertDoesNotThrow(() -> workflowService.getAll(null, null, null, null, pageable),
                    "Không được crash khi workflows không có steps");
        }

        /**
         * Test Case ID: TC-WF-LST-05
         * Đặc tả: Lọc theo departmentId -> Chỉ trả về workflows của phòng ban đó.
         *
         * CheckDB: repository.findByFilters() được gọi với departmentId đúng.
         */
        @Test
        @DisplayName("  TC-WF-LST-05 | getAll() filter theo departmentId -> Truyền đúng departmentId vào repository")
        void getAll_FilterByDepartmentId_ShouldPassCorrectDepartmentIdToRepository() {
            // Arrange
            Long targetDepartmentId = 10L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Workflow> deptPage = new PageImpl<>(List.of(mockActiveWorkflow), pageable, 1);

            when(workflowRepository.findByFilters(isNull(), isNull(), isNull(), eq(targetDepartmentId), eq(pageable)))
                    .thenReturn(deptPage);
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            workflowService.getAll(null, null, null, targetDepartmentId, pageable);

            // CheckDB: departmentId=10 được truyền đúng
            verify(workflowRepository).findByFilters(
                    isNull(), isNull(), isNull(), eq(10L), eq(pageable));
        }
    }

    // ================================================================
    // NHÓM 4: CẬP NHẬT WORKFLOW — update() (WF-01, WF-STATUS)
    // ================================================================
    @Nested
    @DisplayName("WF-01 | Cập nhật Workflow — update()")
    class UpdateWorkflowTests {

        /**
         * Test Case ID: TC-WF-UPD-01
         * Đặc tả (WF-01): Admin cập nhật tên workflow thành công.
         * Tên mới chưa tồn tại nên hợp lệ.
         *
         * CheckDB: Verify workflow được lưu với tên mới.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName("  TC-WF-UPD-01 | Cập nhật tên workflow sang tên mới (chưa tồn tại) -> Lưu tên mới")
        void update_ValidNewName_ShouldUpdateWorkflowNameInDatabase() throws IdInvalidException {
            // Arrange — tên mới chưa tồn tại trong DB
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setName("Quy trình duyệt tuyển dụng V2");

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(workflowRepository.findByName("Quy trình duyệt tuyển dụng V2"))
                    .thenReturn(Optional.empty()); // Tên mới chưa tồn tại
            when(workflowRepository.save(any(Workflow.class))).thenReturn(mockActiveWorkflow);
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            workflowService.update(1L, updateDto);

            // CheckDB: Verify tên mới được set trước khi save
            verify(workflowRepository).save(workflowCaptor.capture());
            assertEquals("Quy trình duyệt tuyển dụng V2", workflowCaptor.getValue().getName(),
                    "Tên mới phải được lưu xuống DB");
            assertEquals(99L, workflowCaptor.getValue().getUpdatedBy(),
                    "updatedBy phải là ID của Admin đang đăng nhập");
        }

        /**
         * Test Case ID: TC-WF-UPD-02
         * Đặc tả (WF-STATUS): Admin có thể chuyển workflow sang trạng thái Inactive
         * để ngừng sử dụng nhưng vẫn lưu lịch sử.
         *
         * CheckDB: Verify isActive = false được lưu.
         */
        @Test
        @DisplayName("  TC-WF-UPD-02 | Cập nhật isActive=false -> Workflow chuyển sang trạng thái Inactive")
        void update_SetIsActiveFalse_ShouldDeactivateWorkflow() throws IdInvalidException {
            // Arrange — Admin tắt workflow
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setIsActive(false); // Chuyển sang Inactive

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            workflowService.update(1L, updateDto);

            // CheckDB: isActive phải là false trong entity được lưu
            verify(workflowRepository).save(workflowCaptor.capture());
            assertFalse(workflowCaptor.getValue().getIsActive(),
                    "isActive phải là false sau khi deactivate");
        }

        /**
         * Test Case ID: TC-WF-UPD-03
         * Đặc tả (WF-STATUS): Admin có thể kích hoạt lại workflow Inactive -> Active.
         *
         * CheckDB: Verify isActive = true được lưu.
         */
        @Test
        @DisplayName("  TC-WF-UPD-03 | Kích hoạt lại workflow Inactive (isActive=true) -> Trạng thái Active")
        void update_ReactivateInactiveWorkflow_ShouldSetIsActiveTrue() throws IdInvalidException {
            // Arrange — workflow đang Inactive, Admin muốn kích hoạt lại
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setIsActive(true); // Kích hoạt lại

            when(workflowRepository.findById(2L)).thenReturn(Optional.of(mockInactiveWorkflow));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            workflowService.update(2L, updateDto);

            // CheckDB: isActive phải là true sau khi kích hoạt lại
            verify(workflowRepository).save(workflowCaptor.capture());
            assertTrue(workflowCaptor.getValue().getIsActive(),
                    "isActive phải là true sau khi kích hoạt lại");
        }

        /**
         * Test Case ID: TC-WF-UPD-04
         * Đặc tả (WF-STEP): Cập nhật steps mới hợp lệ -> Xóa steps cũ, thêm steps mới.
         * (Admin thêm bớt bước phê duyệt trong luồng)
         *
         * CheckDB: Workflow được lưu với steps mới (steps cũ bị xóa qua orphanRemoval).
         */
        @Test
        @DisplayName("  TC-WF-UPD-04 | Cập nhật steps hợp lệ -> Xóa steps cũ, thêm steps mới vào workflow")
        void update_ValidNewSteps_ShouldReplaceOldStepsWithNewSteps() throws IdInvalidException {
            // Arrange — workflow có 2 steps cũ
            mockActiveWorkflow.setSteps(new HashSet<>(Set.of(mockStep1, mockStep2)));

            // Steps mới: chỉ 1 bước (giảm từ 2 xuống 1)
            List<CreateStepDTO> newSteps = List.of(
                    buildCreateStepDTO(1, 202L) // Chỉ còn Giám đốc
            );
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setSteps(newSteps);

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of(202L, 1));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of(202L, "Giám đốc"));

            // Act
            workflowService.update(1L, updateDto);

            // CheckDB: Workflow được lưu với steps mới (1 step)
            verify(workflowRepository).save(workflowCaptor.capture());
            Workflow savedWorkflow = workflowCaptor.getValue();
            assertEquals(1, savedWorkflow.getSteps().size(),
                    "Phải có đúng 1 step mới sau khi cập nhật");
        }

        /**
         * Test Case ID: TC-WF-UPD-05
         * Đặc tả (WF-01): Khi dto.steps = null -> Giữ nguyên steps cũ, không xóa.
         *
         * CheckDB: Workflow được lưu, steps không bị thay đổi.
         */
        @Test
        @DisplayName("  TC-WF-UPD-05 | Cập nhật với steps = null -> Giữ nguyên steps cũ (không xóa)")
        void update_NullSteps_ShouldKeepExistingStepsUnchanged() throws IdInvalidException {
            // Arrange — workflow có 2 steps, nhưng DTO không thay đổi steps
            mockActiveWorkflow.setSteps(new HashSet<>(Set.of(mockStep1, mockStep2)));

            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setDescription("Mô tả mới"); // Chỉ cập nhật description
            updateDto.setSteps(null); // Không thay đổi steps

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act
            workflowService.update(1L, updateDto);

            // CheckDB: Steps vẫn là 2 (không bị xóa)
            verify(workflowRepository).save(workflowCaptor.capture());
            assertEquals(2, workflowCaptor.getValue().getSteps().size(),
                    "Steps cũ phải được giữ nguyên khi dto.steps = null");
        }

        /**
         * Test Case ID: TC-WF-UPD-06
         * Đặc tả (WF-01): ID không tồn tại -> Ném IdInvalidException.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi.
         */
        @Test
        @DisplayName(" TC-WF-UPD-06 | update() với ID không tồn tại -> Ném IdInvalidException")
        void update_NonExistentWorkflowId_ShouldThrowIdInvalidException() {
            // Arrange
            when(workflowRepository.findById(999L)).thenReturn(Optional.empty());
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setName("Tên mới");

            // Act & Assert
            assertThrows(IdInvalidException.class,
                    () -> workflowService.update(999L, updateDto),
                    "Phải ném IdInvalidException khi ID không tồn tại");

            // CheckDB: Không lưu gì
            verify(workflowRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-WF-UPD-07
         * Đặc tả (WF-01): Tên mới trùng với workflow KHÁC trong hệ thống -> Ném CustomException.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi.
         */
        @Test
        @DisplayName(" TC-WF-UPD-07 | Cập nhật tên trùng với workflow khác -> Ném CustomException")
        void update_DuplicateNewName_ShouldThrowCustomException() {
            // Arrange — "Tên đã tồn tại" thuộc về workflow khác (ID=5)
            Workflow anotherWorkflow = new Workflow();
            anotherWorkflow.setId(5L);
            anotherWorkflow.setName("Tên đã tồn tại");

            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setName("Tên đã tồn tại"); // Trùng với workflow ID=5

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(workflowRepository.findByName("Tên đã tồn tại"))
                    .thenReturn(Optional.of(anotherWorkflow)); // Tên đã tồn tại!

            // Act & Assert
            CustomException thrownException = assertThrows(CustomException.class,
                    () -> workflowService.update(1L, updateDto),
                    "Phải ném CustomException khi tên mới trùng với workflow khác");

            assertTrue(thrownException.getMessage().contains("Tên đã tồn tại"),
                    "Message lỗi phải chứa tên bị trùng");

            // CheckDB: Không lưu gì
            verify(workflowRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-WF-UPD-08
         * Đặc tả (WF-STEP): Cập nhật steps mới có hierarchyOrder tăng dần (sai chiều)
         * -> Ném CustomException.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi.
         */
        @Test
        @DisplayName(" TC-WF-UPD-08 | Cập nhật steps có hierarchyOrder tăng dần (sai) -> Ném CustomException")
        void update_StepsWithInvalidHierarchyOrder_ShouldThrowCustomException() {
            // Arrange — Steps sai thứ tự hierarchy (từ cao xuống thấp thay vì thấp lên cao)
            List<CreateStepDTO> invalidSteps = List.of(
                    buildCreateStepDTO(1, 202L), // Bước 1: Giám đốc, order=1
                    buildCreateStepDTO(2, 201L)  // Bước 2: Trưởng phòng, order=3 -> SAI!
            );
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setSteps(invalidSteps);

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(userService.getPositionHierarchyOrdersByIds(anyList(), any()))
                    .thenReturn(Map.of(202L, 1, 201L, 3));

            // Act & Assert
            assertThrows(CustomException.class,
                    () -> workflowService.update(1L, updateDto),
                    "Phải ném CustomException khi steps update có hierarchy sai");

            // CheckDB: Không lưu gì
            verify(workflowRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-WF-UPD-09
         * Đặc tả (WF-01): Cập nhật tên giống tên cũ của chính workflow đó
         * -> Không báo lỗi trùng tên (vì đây là cùng 1 workflow).
         *
         * CheckDB: workflowRepository.save() được gọi bình thường.
         */
        @Test
        @DisplayName("  TC-WF-UPD-09 | Cập nhật tên giống tên cũ (không đổi) -> Bỏ qua kiểm tra trùng tên")
        void update_SameNameAsCurrentWorkflow_ShouldSkipDuplicateCheck() throws IdInvalidException {
            // Arrange — dto.name giống với tên hiện tại của workflow
            UpdateWorkflowDTO updateDto = new UpdateWorkflowDTO();
            updateDto.setName("Quy trình duyệt tuyển dụng"); // Giống tên hiện tại

            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            // findByName không được gọi vì tên không thay đổi
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));
            when(userService.getPositionNamesByIds(anyList(), any())).thenReturn(Map.of());

            // Act — không được ném exception
            assertDoesNotThrow(() -> workflowService.update(1L, updateDto),
                    "Không được lỗi khi tên mới giống tên cũ");

            // CheckDB: findByName không được gọi (tên không thay đổi)
            verify(workflowRepository, never()).findByName(any());
            // save được gọi bình thường
            verify(workflowRepository, times(1)).save(any());
        }
    }

    // ================================================================
    // NHÓM 5: XÓA WORKFLOW — delete() (WF-01, WF-STATUS)
    // ================================================================
    @Nested
    @DisplayName("WF-01 | Xóa Workflow (Soft Delete) — delete()")
    class DeleteWorkflowTests {

        /**
         * Test Case ID: TC-WF-DEL-01
         * Đặc tả (WF-01, WF-STATUS): Xóa workflow hợp lệ -> Soft delete (isActive = false).
         * Workflow bị "tắt" nhưng vẫn giữ lại dữ liệu lịch sử trong DB.
         *
         * CheckDB: Verify workflow được lưu với isActive = false.
         * Rollback: Mock reset bởi @ExtendWith.
         */
        @Test
        @DisplayName("  TC-WF-DEL-01 | delete() với ID hợp lệ -> Soft delete (isActive=false), giữ dữ liệu lịch sử")
        void delete_ValidWorkflowId_ShouldPerformSoftDeleteBySettingIsActiveFalse() {
            // Arrange
            when(workflowRepository.findById(1L)).thenReturn(Optional.of(mockActiveWorkflow));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            workflowService.delete(1L);

            // CheckDB: isActive phải là false (soft delete, không xóa vật lý)
            verify(workflowRepository).save(workflowCaptor.capture());
            Workflow softDeletedWorkflow = workflowCaptor.getValue();
            assertFalse(softDeletedWorkflow.getIsActive(),
                    "isActive phải là false sau khi soft delete");

            // CheckDB: Không gọi deleteById (giữ dữ liệu lịch sử)
            verify(workflowRepository, never()).deleteById(any());
        }

        /**
         * Test Case ID: TC-WF-DEL-02
         * Đặc tả (WF-01): ID không tồn tại -> Ném IdInvalidException.
         *
         * CheckDB: workflowRepository.save() KHÔNG được gọi.
         */
        @Test
        @DisplayName(" TC-WF-DEL-02 | delete() với ID không tồn tại -> Ném IdInvalidException")
        void delete_NonExistentWorkflowId_ShouldThrowIdInvalidException() {
            // Arrange
            when(workflowRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IdInvalidException thrownException = assertThrows(IdInvalidException.class,
                    () -> workflowService.delete(999L),
                    "Phải ném IdInvalidException khi xóa workflow không tồn tại");

            assertTrue(thrownException.getMessage().contains("999"),
                    "Message lỗi phải chứa ID không tìm thấy");

            // CheckDB: Không lưu gì khi lỗi
            verify(workflowRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-WF-DEL-03
         * Đặc tả (WF-STATUS): Xóa (soft delete) workflow đã Inactive
         * -> Vẫn thực hiện được (isActive vẫn set = false).
         *
         * CheckDB: Verify save() được gọi với isActive = false.
         */
        @Test
        @DisplayName("  TC-WF-DEL-03 | delete() workflow đã Inactive -> Thực hiện được, isActive vẫn = false")
        void delete_AlreadyInactiveWorkflow_ShouldStillSaveWithIsActiveFalse() {
            // Arrange — workflow đã ở trạng thái Inactive
            when(workflowRepository.findById(2L)).thenReturn(Optional.of(mockInactiveWorkflow));
            when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

            // Act — không được ném exception
            assertDoesNotThrow(() -> workflowService.delete(2L),
                    "Không được lỗi khi xóa workflow đã Inactive");

            // CheckDB: save() được gọi với isActive = false
            verify(workflowRepository).save(workflowCaptor.capture());
            assertFalse(workflowCaptor.getValue().getIsActive());
        }
    }

    // ================================================================
    // PHƯƠNG THỨC HỖ TRỢ (Test Helper Methods)
    // ================================================================

    /**
     * Tạo nhanh CreateWorkflowDTO với các tham số tùy chỉnh.
     * Dùng để giảm code lặp trong các test.
     */
    private CreateWorkflowDTO buildCreateWorkflowDTO(
            String name,
            String description,
            WorkflowType type,
            Long departmentId,
            List<CreateStepDTO> steps) {
        CreateWorkflowDTO dto = new CreateWorkflowDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setType(type);
        dto.setDepartmentId(departmentId);
        dto.setSteps(steps);
        return dto;
    }

    /**
     * Tạo nhanh CreateStepDTO với stepOrder và approverPositionId.
     * Dùng để build danh sách steps trong các test.
     *
     * @param stepOrder          Thứ tự bước trong luồng (1, 2, 3...)
     * @param approverPositionId ID của vị trí công việc có quyền duyệt bước này
     */
    private CreateStepDTO buildCreateStepDTO(int stepOrder, Long approverPositionId) {
        CreateStepDTO stepDto = new CreateStepDTO();
        stepDto.setStepOrder(stepOrder);
        stepDto.setApproverPositionId(approverPositionId);
        return stepDto;
    }
}
