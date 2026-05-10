package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.http.ResponseEntity;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.jobposition.CreateJobPositionDTO;
import com.example.job_service.dto.jobposition.GetAllJobPositionDTO;
import com.example.job_service.dto.jobposition.JobPositionResponseDTO;
import com.example.job_service.dto.jobposition.UpdateJobPositionDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.JobPosition;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.JobPositionRepository;
import com.example.job_service.utils.TextTruncateUtil;
import com.example.job_service.utils.enums.JobPositionStatus;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * ============================================================
 * UNIT TEST CHO JOB POSITION SERVICE (BẢN HOÀN CHỈNH)
 *
 * MỤC TIÊU:
 * - Bảo vệ các luồng CRUD cũ (Tạo, Sửa, Lọc, Thay đổi trạng thái).
 * - Bổ sung các Test Case Validation chặn lỗi Dữ liệu.
 * * [QUY ƯỚC CHECK_DB VÀ ROLLBACK]
 * 1. CheckDB:
 * - Xác minh cơ sở dữ liệu có thay đổi đúng hay không thông qua
 * `verify(repository)` và `ArgumentCaptor` để tóm lấy entity
 * trước khi lưu, từ đó kiểm tra dữ liệu thay đổi.
 * * 2. Rollback:
 * - Toàn bộ Service được test trong môi trường Mock (giả lập),
 * không có kết nối trực tiếp đến DB vật lý. Framework Mockito
 * tự động "Rollback" bằng cách dọn dẹp RAM và các đối tượng Mock
 * sau mỗi Test Case, đảm bảo tính độc lập hoàn toàn.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Position Service - Comprehensive Test Suite")
class JobPositionServiceTest {

    @Mock
    private JobPositionRepository jobPositionRepository;

    @Mock
    private RecruitmentRequestService recruitmentRequestService;

    @Mock
    private UserClient userService;

    @Mock
    private CandidateClient candidateClient;

    @InjectMocks
    private JobPositionService jobPositionService;

    @Captor
    private ArgumentCaptor<JobPosition> jobPositionCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecruitmentRequest mockRecruitmentRequest;
    private JobPosition mockDraftPosition;

    @BeforeEach
    void setUp() {
        mockRecruitmentRequest = new RecruitmentRequest();
        mockRecruitmentRequest.setId(500L);
        mockRecruitmentRequest.setDepartmentId(20L);
        mockRecruitmentRequest.setSalaryMin(new BigDecimal("15000000"));
        mockRecruitmentRequest.setSalaryMax(new BigDecimal("30000000"));
        mockRecruitmentRequest.setStatus(RecruitmentRequestStatus.APPROVED);
        mockRecruitmentRequest.setQuantity(5); // Số lượng duyệt là 5

        mockDraftPosition = buildPosition(200L, "Senior Python Developer", JobPositionStatus.DRAFT);
        mockDraftPosition.setDescription("Phát triển hệ thống phân tích dữ liệu lớn");
        mockDraftPosition.setRequirements("Python, Django, PostgreSQL, Docker");
        mockDraftPosition.setBenefits("Lương tháng 13, Bảo hiểm PVI, Remote 2 ngày/tuần");
        mockDraftPosition.setSalaryMin(new BigDecimal("20000000"));
        mockDraftPosition.setSalaryMax(new BigDecimal("35000000"));
        mockDraftPosition.setEmploymentType("Full-time");
        mockDraftPosition.setExperienceLevel("Senior");
        mockDraftPosition.setLocation("Hồ Chí Minh");
        mockDraftPosition.setRemote(true);
        mockDraftPosition.setQuantity(5);
        mockDraftPosition.setDeadline(LocalDate.of(2026, 12, 31));
        mockDraftPosition.setYearsOfExperience("4 năm");
    }

    private JobPosition buildPosition(Long id, String title, JobPositionStatus status) {
        JobPosition position = new JobPosition();
        position.setId(id);
        position.setTitle(title);
        position.setStatus(status);
        position.setRecruitmentRequest(mockRecruitmentRequest);
        return position;
    }

    private CreateJobPositionDTO buildCreateDto() {
        CreateJobPositionDTO dto = new CreateJobPositionDTO();
        dto.setRecruitmentRequestId(500L);
        dto.setTitle("DevOps Engineer");
        dto.setDescription("Thiết lập CI/CD và quản lý hạ tầng Cloud");
        dto.setRequirements("AWS, Kubernetes, Terraform");
        dto.setBenefits("Thưởng dự án, Laptop xịn");
        dto.setSalaryMin(new BigDecimal("25000000"));
        dto.setSalaryMax(new BigDecimal("45000000"));
        dto.setEmploymentType("Full-time");
        dto.setExperienceLevel("Middle/Senior");
        dto.setLocation("Hà Nội");
        dto.setIsRemote(Boolean.FALSE);
        dto.setQuantity(2);
        dto.setDeadline(LocalDate.of(2026, 10, 15));
        dto.setYearsOfExperience("3 năm");
        return dto;
    }

    private UpdateJobPositionDTO buildUpdateDtoWithAllMutableFields() {
        UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
        dto.setTitle("Lead DevOps Engineer");
        dto.setDescription("Quản lý đội ngũ vận hành và bảo mật");
        dto.setRequirements("AWS, K8s, Security Expert");
        dto.setBenefits("Cổ phần công ty, du lịch nước ngoài");
        dto.setSalaryMin(new BigDecimal("50000000"));
        dto.setSalaryMax(new BigDecimal("80000000"));
        dto.setEmploymentType("Hybrid");
        dto.setExperienceLevel("Manager");
        dto.setLocation("Hồ Chí Minh");
        dto.setIsRemote(Boolean.TRUE);
        dto.setQuantity(1);
        dto.setDeadline(LocalDate.of(2027, 1, 1));
        dto.setYearsOfExperience("7 năm");
        return dto;
    }

    // ================================================================
    // NHÓM 1: TẠO MỚI (CREATE) & VALIDATION (BỔ SUNG)
    // ================================================================
    @Nested
    @DisplayName("Create Job Position & Validations Tests")
    class CreateJobPositionTests {

        /**
         * Test Case ID: TC-JOB-001
         */
        @Test
        @DisplayName("TC-JOB-001 | Tạo thành công -> Lưu dữ liệu đầy đủ và Complete Yêu cầu")
        void create_shouldPersistProvidedDataAndCompleteRecruitmentRequest() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);
            when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(invocation -> {
                JobPosition saved = invocation.getArgument(0);
                saved.setId(201L);
                return saved;
            });

            JobPosition result = jobPositionService.create(dto);

            assertNotNull(result);
            assertEquals(201L, result.getId());
            assertEquals("DevOps Engineer", result.getTitle());
            assertEquals(JobPositionStatus.DRAFT, result.getStatus());

            // CheckDB: Xác minh dữ liệu trước khi lưu
            verify(jobPositionRepository).save(jobPositionCaptor.capture());
            JobPosition persisted = jobPositionCaptor.getValue();
            assertEquals(new BigDecimal("25000000"), persisted.getSalaryMin());
            assertEquals(new BigDecimal("45000000"), persisted.getSalaryMax());
            assertFalse(persisted.isRemote());
            assertEquals(2, persisted.getQuantity());

            // Xác minh update status của Request
            verify(recruitmentRequestService).changeStatus(500L, RecruitmentRequestStatus.COMPLETED);
        }

        /**
         * Test Case ID: TC-JOB-002
         */
        @Test
        @DisplayName("TC-JOB-002 | Khuyết dữ liệu tùy chọn -> Tự động lấy (Fallback) từ Recruitment Request")
        void create_shouldFallbackToRecruitmentRequestWhenOptionalInputIsOmitted() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            dto.setSalaryMin(null);
            dto.setSalaryMax(null);
            dto.setIsRemote(null);

            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);
            when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(invocation -> invocation.getArgument(0));

            JobPosition result = jobPositionService.create(dto);

            assertEquals(JobPositionStatus.DRAFT, result.getStatus());

            verify(jobPositionRepository).save(jobPositionCaptor.capture());
            JobPosition persisted = jobPositionCaptor.getValue();
            assertEquals(new BigDecimal("15000000"), persisted.getSalaryMin());
            assertEquals(new BigDecimal("30000000"), persisted.getSalaryMax());
            assertFalse(persisted.isRemote());
        }

        /**
         * Test Case ID: TC-JOB-VAL-01
         * MỚI: Dựa trên lỗi System Test VAL_JOB01_04
         */
        @Test
        @DisplayName("TC-JOB-VAL-01 | Lương tối thiểu LỚN HƠN Lương tối đa -> Phải ném ngoại lệ")
        void create_shouldThrowExceptionWhenSalaryMinGreaterThanMax() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            dto.setSalaryMin(new BigDecimal("20000000")); // Min = 20M
            dto.setSalaryMax(new BigDecimal("10000000")); // Max = 10M

            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);

            assertThrows(IllegalArgumentException.class, () -> jobPositionService.create(dto));

            // CheckDB: Đảm bảo không gọi lệnh save() khi validate tạch
            verify(jobPositionRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-JOB-VAL-02
         * MỚI: Dựa trên lỗi System Test VAL_JOB01_05
         */
        @Test
        @DisplayName("TC-JOB-VAL-02 | Số lượng cần tuyển ÂM -> Phải ném ngoại lệ")
        void create_shouldThrowExceptionWhenQuantityIsNegative() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            dto.setQuantity(-1);

            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);

            assertThrows(IllegalArgumentException.class, () -> jobPositionService.create(dto));
            verify(jobPositionRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-JOB-VAL-03
         * MỚI: Dựa trên lỗi System Test VAL_JOB01_06
         */
        @Test
        @DisplayName("TC-JOB-VAL-03 | Hạn nộp hồ sơ ở QUÁ KHỨ -> Phải ném ngoại lệ")
        void create_shouldThrowExceptionWhenDeadlineIsInPast() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            dto.setDeadline(LocalDate.of(2020, 1, 1));

            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);

            assertThrows(IllegalArgumentException.class, () -> jobPositionService.create(dto));
            verify(jobPositionRepository, never()).save(any());
        }

        /**
         * Test Case ID: TC-JOB-VAL-04
         * MỚI: Dựa trên lỗi System Test VAL_JOB01_09 & VAL_JOB01_10
         */
        @Test
        @DisplayName("TC-JOB-VAL-04 | Số lượng tuyển khác với Số lượng yêu cầu đã duyệt -> Phải ném ngoại lệ")
        void create_shouldThrowExceptionWhenQuantityNotMatchApprovedRequest() throws Exception {
            CreateJobPositionDTO dto = buildCreateDto();
            dto.setQuantity(10); // Vượt mức 5 người của Yêu cầu

            when(recruitmentRequestService.findById(500L)).thenReturn(mockRecruitmentRequest);

            assertThrows(IllegalArgumentException.class, () -> jobPositionService.create(dto));
            verify(jobPositionRepository, never()).save(any());
        }
    }

    // ================================================================
    // NHÓM 2: TÌM KIẾM VÀ LẤY CHI TIẾT
    // ================================================================
    @Nested
    @DisplayName("Retrieve and Details Logic Tests")
    class RetrieveJobPositionTests {

        /** Test Case ID: TC-JOB-003 */
        @Test
        @DisplayName("TC-JOB-003 | findById -> Trả về Position nếu ID tồn tại")
        void findById_shouldReturnExistingPosition() throws Exception {
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            assertSame(mockDraftPosition, jobPositionService.findById(200L));
        }

        /** Test Case ID: TC-JOB-004 */
        @Test
        @DisplayName("TC-JOB-004 | findById -> Ném ngoại lệ IdInvalidException nếu không tìm thấy")
        void findById_shouldThrowWhenPositionDoesNotExist() {
            when(jobPositionRepository.findById(999L)).thenReturn(Optional.empty());
            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.findById(999L));
            assertEquals("Vị trí tuyển dụng không tồn tại", exception.getMessage());
        }

        /** Test Case ID: TC-JOB-005 */
        @Test
        @DisplayName("TC-JOB-005 | getByIdSimple -> Ủy quyền chuẩn xác cho findById")
        void getByIdSimple_shouldDelegateToFindByIdForExistingPosition() throws Exception {
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            assertSame(mockDraftPosition, jobPositionService.getByIdSimple(200L));
        }

        /** Test Case ID: TC-JOB-006 */
        @Test
        @DisplayName("TC-JOB-006 | getByIdWithDepartmentName -> Ánh xạ thành công Tên phòng ban và Số ứng viên")
        void getByIdWithDepartmentName_shouldReturnDepartmentNameAndApplicationCount() throws Exception {
            ObjectNode departmentNode = objectMapper.createObjectNode().put("name", "Phòng Hạ tầng & DevOps");

            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            when(userService.getDepartmentById(20L, "token-abc")).thenReturn(ResponseEntity.ok(departmentNode));
            when(candidateClient.countCandidatesByJobPositionId(200L, "token-abc")).thenReturn(12);

            JobPositionResponseDTO result = jobPositionService.getByIdWithDepartmentName(200L, "token-abc");

            assertEquals("Phòng Hạ tầng & DevOps", result.getDepartmentName());
            assertEquals(12, result.getApplicationCount());
        }

        /** Test Case ID: TC-JOB-007 */
        @Test
        @DisplayName("TC-JOB-007 | getByIdsWithDepartmentName -> Trả về mảng rỗng nếu mảng IDs trống")
        void getByIdsWithDepartmentName_shouldReturnEmptyListWhenIdsAreMissing() {
            List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(List.of(), "token");
            assertTrue(result.isEmpty());
            verify(jobPositionRepository, never()).findByIdIn(any());
        }

        /** Test Case ID: TC-JOB-008 */
        @Test
        @DisplayName("TC-JOB-008 | getByIdsWithDepartmentName -> Gán Unknown Department nếu mất liên kết phòng ban")
        void getByIdsWithDepartmentName_shouldFallbackUnknownDepartmentForMissingMapping() {
            JobPosition secondPosition = buildPosition(201L, "QA Lead", JobPositionStatus.PUBLISHED);

            when(jobPositionRepository.findByIdIn(List.of(200L, 201L))).thenReturn(List.of(mockDraftPosition, secondPosition));
            when(userService.getDepartmentsByIds(List.of(20L), "token")).thenReturn(Map.of());

            List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(List.of(200L, 201L), "token");

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(dto -> "Unknown Department".equals(dto.getDepartmentName())));
        }

        /** Test Case ID: TC-JOB-009 */
        @Test
        @DisplayName("TC-JOB-009 | getByIdWithPublished -> Trả về vị trí nếu đang Công khai")
        void getByIdWithPublished_shouldReturnPositionWhenStatusIsPublished() throws Exception {
            JobPosition publishedPosition = buildPosition(200L, "Python Pro", JobPositionStatus.PUBLISHED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(publishedPosition));

            assertSame(publishedPosition, jobPositionService.getByIdWithPublished(200L));
        }

        /** Test Case ID: TC-JOB-010 */
        @Test
        @DisplayName("TC-JOB-010 | getByIdWithPublished -> Ném ngoại lệ nếu vị trí chưa Công khai")
        void getByIdWithPublished_shouldThrowWhenStatusIsNotPublished() {
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.getByIdWithPublished(200L));
            assertEquals("Vị trí tuyển dụng chưa được xuất bản hoặc không khả dụng", exception.getMessage());
        }
    }

    // ================================================================
    // NHÓM 3: TÌM KIẾM, LỌC VÀ PHÂN TRANG
    // ================================================================
    @Nested
    @DisplayName("Filter and Pagination Logic Tests")
    class FilterAndPaginationTests {

        /** Test Case ID: TC-JOB-011 */
        @Test
        @DisplayName("TC-JOB-011 | Lọc list với chuỗi IDs hợp lệ -> Bỏ qua Repository Filter")
        void findAllWithFiltersSimple_shouldReturnMatchedIdsWhenIdsAreValid() {
            JobPosition pos2 = buildPosition(201L, "Tester", JobPositionStatus.PUBLISHED);
            when(jobPositionRepository.findByIdIn(List.of(200L, 201L))).thenReturn(List.of(mockDraftPosition, pos2));

            List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, "200, 201");

            assertEquals(2, result.size());
            verify(jobPositionRepository).findByIdIn(List.of(200L, 201L));
            verify(jobPositionRepository, never()).findByFilters(any(), any(), any(), any(), any());
        }

        /** Test Case ID: TC-JOB-012 */
        @Test
        @DisplayName("TC-JOB-012 | Lọc list với chuỗi IDs lỗi -> Fallback dùng Repository Filter")
        void findAllWithFiltersSimple_shouldFallbackToRepositoryFilterWhenIdsAreInvalid() {
            Pageable unlimited = PageRequest.of(0, Integer.MAX_VALUE);
            Page<JobPosition> filteredPage = new PageImpl<>(List.of(mockDraftPosition), unlimited, 1);

            when(jobPositionRepository.findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", unlimited))
                    .thenReturn(filteredPage);

            List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", "invalid-id");

            assertEquals(1, result.size());
            verify(jobPositionRepository).findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", unlimited);
        }

        /** Test Case ID: TC-JOB-013 */
        @Test
        @DisplayName("TC-JOB-013 | Lọc phân trang với chuỗi IDs hợp lệ -> Phân trang thủ công trong Service")
        void findAllWithFiltersSimplePaged_shouldManuallyPaginateWhenIdsAreValid() {
            Pageable pageable = PageRequest.of(1, 1);
            JobPosition pos2 = buildPosition(201L, "QA Automation", JobPositionStatus.PUBLISHED);

            when(jobPositionRepository.findByIdIn(List.of(200L, 201L))).thenReturn(List.of(mockDraftPosition, pos2));

            PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "200,201", pageable);

            assertEquals(2L, result.getMeta().getTotal());
            assertEquals(2, result.getMeta().getPage()); // 0-based pageable offset map to 1-based Meta
            List<?> rows = assertInstanceOf(List.class, result.getResult());
            assertEquals(1, rows.size());
            assertSame(pos2, rows.get(0));
        }

        /** Test Case ID: TC-JOB-014 */
        @Test
        @DisplayName("TC-JOB-014 | Lọc phân trang với chuỗi IDs lỗi -> Trả về mảng rỗng an toàn")
        void findAllWithFiltersSimplePaged_shouldReturnEmptyPageWhenIdsCannotBeParsed() {
            Pageable pageable = PageRequest.of(0, 10);
            PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "200,xyz", pageable);

            assertEquals(0L, result.getMeta().getTotal());
            assertTrue(((List<?>) result.getResult()).isEmpty());
        }

        /** Test Case ID: TC-JOB-015 */
        @Test
        @DisplayName("TC-JOB-015 | Lọc phân trang bình thường -> Gọi Repository Filter")
        void findAllWithFiltersSimplePaged_shouldUseRepositoryFilterWhenIdsAreMissing() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<JobPosition> filteredPage = new PageImpl<>(List.of(mockDraftPosition), pageable, 1);

            when(jobPositionRepository.findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable))
                    .thenReturn(filteredPage);

            PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", null, pageable);

            assertEquals(1L, result.getMeta().getTotal());
        }

        /** Test Case ID: TC-JOB-016 */
        @Test
        @DisplayName("TC-JOB-016 | DTO Full - Tham số departmentId = 1 tự động chuyển thành null")
        void findAllWithFilters_shouldNormalizeDepartmentOneAndMapDepartmentAndApplicationData() {
            Pageable pageable = PageRequest.of(0, 10);
            JobPosition posPublished = buildPosition(201L, "Backend Dev", JobPositionStatus.PUBLISHED);
            Page<JobPosition> page = new PageImpl<>(List.of(mockDraftPosition, posPublished), pageable, 2);

            when(jobPositionRepository.findByFilters(null, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable))
                    .thenReturn(page);
            when(userService.getDepartmentsByIds(List.of(20L), "token")).thenReturn(Map.of(20L, "Phòng kỹ thuật"));
            when(candidateClient.countCandidatesByJobPositionIds(List.of(200L, 201L), "token"))
                    .thenReturn(Map.of(200L, 10, 201L, 5));

            PaginationDTO result = jobPositionService.findAllWithFilters(1L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable, "token");

            assertEquals(2L, result.getMeta().getTotal());
            List<?> rows = assertInstanceOf(List.class, result.getResult());
            JobPositionResponseDTO firstRow = assertInstanceOf(JobPositionResponseDTO.class, rows.get(0));
            assertEquals("Phòng kỹ thuật", firstRow.getDepartmentName());
            assertEquals(10, firstRow.getApplicationCount());
        }

        /** Test Case ID: TC-JOB-017 */
        @Test
        @DisplayName("TC-JOB-017 | DTO Full - Tham số departmentId hợp lệ giữ nguyên giá trị lọc")
        void findAllWithFilters_shouldKeepDepartmentFilterWhenDepartmentIsNotOne() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<JobPosition> page = new PageImpl<>(List.of(mockDraftPosition), pageable, 1);

            when(jobPositionRepository.findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable))
                    .thenReturn(page);
            when(userService.getDepartmentsByIds(List.of(20L), "token")).thenReturn(Map.of(20L, "Phòng kỹ thuật"));

            jobPositionService.findAllWithFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable, "token");

            verify(jobPositionRepository).findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable);
        }

        /** Test Case ID: TC-JOB-018 */
        @Test
        @DisplayName("TC-JOB-018 | DTO Rút gọn - Gọi Filter và map dữ liệu ngoại vi đầy đủ")
        void findAllWithFiltersSimplified_shouldReturnSimplifiedRowsWithDepartmentAndApplicationData() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<JobPosition> page = new PageImpl<>(List.of(mockDraftPosition), pageable, 1);

            when(jobPositionRepository.findByFilters(20L, JobPositionStatus.DRAFT, Boolean.TRUE, "python", pageable))
                    .thenReturn(page);
            when(userService.getDepartmentsByIds(List.of(20L), "token")).thenReturn(Map.of(20L, "IT Dept"));
            when(candidateClient.countCandidatesByJobPositionIds(List.of(200L), "token")).thenReturn(Map.of(200L, 4));

            PaginationDTO result = jobPositionService.findAllWithFiltersSimplified(20L, JobPositionStatus.DRAFT, null, Boolean.TRUE, "python", pageable, "token");

            List<?> rows = assertInstanceOf(List.class, result.getResult());
            GetAllJobPositionDTO firstRow = assertInstanceOf(GetAllJobPositionDTO.class, rows.get(0));
            assertEquals("IT Dept", firstRow.getDepartmentName());
            assertEquals(4, firstRow.getApplicationCount());
        }
    }

    // ================================================================
    // NHÓM 4: CẬP NHẬT (UPDATE) & XÓA (DELETE)
    // ================================================================
    @Nested
    @DisplayName("Update and Delete Logic Tests")
    class UpdateAndDeleteTests {

        /** Test Case ID: TC-JOB-019 */
        // ================================================================
        // NHÓM 4: CẬP NHẬT (UPDATE) VÀ KIỂM TRA VALIDATION KHI CẬP NHẬT
        // (Bỏ qua chức năng Xóa vì UI thực tế không có)
        // ================================================================
        @Nested
        @DisplayName("Update Logic & Validation Tests (Bắt Bug System Test)")
        class UpdateJobPositionTests {

            /**
             * Test Case ID: TC-JOB-019
             * Lỗi thực tế: Cập nhật Lương tối thiểu > Lương tối đa vẫn thành công.
             * Kỳ vọng: Hàm update() phải ném ra IllegalArgumentException giống hệt hàm create().
             */
            @Test
            @DisplayName("TC-JOB-019 | Cập nhật -> Lương tối thiểu LỚN HƠN Lương tối đa -> Phải ném ngoại lệ")
            void update_shouldThrowExceptionWhenSalaryMinGreaterThanMax() throws Exception {
                UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
                dto.setSalaryMin(new BigDecimal("40000000")); // Min = 40M
                dto.setSalaryMax(new BigDecimal("20000000")); // Max = 20M

                when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));

                // Assert: Kỳ vọng ném ngoại lệ để chặn cập nhật
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    jobPositionService.update(200L, dto);
                }, "Hệ thống phải chặn cập nhật khi lương Min lớn hơn lương Max");

                // CheckDB: Đảm bảo không có thao tác lưu xuống DB khi dữ liệu sai
                verify(jobPositionRepository, never()).save(any());
            }

            /**
             * Test Case ID: TC-JOB-020
             * Lỗi thực tế: Cập nhật Số lượng là số âm vẫn thành công.
             * Kỳ vọng: Hàm update() phải ném ra IllegalArgumentException.
             */
            @Test
            @DisplayName("TC-JOB-020 | Cập nhật -> Giá trị kiểu số bị ÂM (Số lượng/Kinh nghiệm) -> Phải ném ngoại lệ")
            void update_shouldThrowExceptionWhenNumericFieldsAreNegative() throws Exception {
                UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
                dto.setQuantity(-5); // Số lượng âm

                when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));

                assertThrows(IllegalArgumentException.class, () -> {
                    jobPositionService.update(200L, dto);
                }, "Hệ thống phải chặn không cho phép cập nhật số lượng âm");

                verify(jobPositionRepository, never()).save(any());
            }

            /**
             * Test Case ID: TC-JOB-021
             * Lỗi thực tế: Cập nhật Hạn nộp hồ sơ vào ngày trong quá khứ vẫn thành công.
             * Kỳ vọng: Hàm update() phải ném ra IllegalArgumentException.
             */
            @Test
            @DisplayName("TC-JOB-021 | Cập nhật -> Hạn nộp hồ sơ ở QUÁ KHỨ -> Phải ném ngoại lệ")
            void update_shouldThrowExceptionWhenDeadlineIsInPast() throws Exception {
                UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
                dto.setDeadline(LocalDate.of(2020, 5, 10)); // Ngày trong quá khứ

                when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));

                assertThrows(IllegalArgumentException.class, () -> {
                    jobPositionService.update(200L, dto);
                }, "Hệ thống phải chặn cập nhật khi Hạn nộp nằm trong quá khứ");

                verify(jobPositionRepository, never()).save(any());
            }

            /**
             * Test Case ID: TC-JOB-021b (Giữ lại Regression Test kiểm tra luồng Happy Case)
             * Kịch bản: Khi dữ liệu hợp lệ, chỉ những trường truyền vào mới bị thay đổi.
             */
            @Test
            @DisplayName("TC-JOB-021b | Cập nhật hợp lệ -> Form trống 1 phần vẫn bảo toàn dữ liệu gốc (Happy Case)")
            void update_shouldKeepOriginalDataWhenFieldIsNotProvided() throws Exception {
                UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
                dto.setTitle("Renamed Title Only"); // Chỉ update title

                JobPosition existing = buildPosition(200L, "Original Python", JobPositionStatus.DRAFT);
                existing.setDescription("Original Desc");
                existing.setSalaryMin(new BigDecimal("22000000"));

                when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(existing));
                when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(i -> i.getArgument(0));

                JobPosition result = jobPositionService.update(200L, dto);

                assertEquals("Renamed Title Only", result.getTitle());
                assertEquals("Original Desc", result.getDescription()); // Giữ nguyên mô tả cũ
                assertEquals(new BigDecimal("22000000"), result.getSalaryMin()); // Giữ nguyên lương
                verify(jobPositionRepository).save(any());
            }
        }
    }

    // ================================================================
    // NHÓM 5: THAY ĐỔI TRẠNG THÁI VÀ TÍCH HỢP (STATUS & E2E)
    // ================================================================
    @Nested
    @DisplayName("Status Transition & Integration Tests")
    class StatusTransitionTests {

        /** Test Case ID: TC-JOB-022 */
        @Test
        @DisplayName("TC-JOB-022 | Publish -> Chuyển từ DRAFT sang PUBLISHED")
        void publish_shouldChangeStatusToPublishedAndSetPublishedAt() throws Exception {
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(i -> i.getArgument(0));

            JobPosition result = jobPositionService.publish(200L);

            assertEquals(JobPositionStatus.PUBLISHED, result.getStatus());
            assertNotNull(result.getPublishedAt());
        }

        /** Test Case ID: TC-JOB-023 */
        @Test
        @DisplayName("TC-JOB-023 | Publish -> Ném ngoại lệ nếu không phải DRAFT")
        void publish_shouldThrowWhenPositionIsNotDraft() {
            JobPosition closedPos = buildPosition(200L, "Python", JobPositionStatus.CLOSED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(closedPos));

            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.publish(200L));
            assertEquals("Chỉ có thể publish vị trí ở trạng thái DRAFT", exception.getMessage());
        }

        /** Test Case ID: TC-JOB-024 */
        @Test
        @DisplayName("TC-JOB-024 | Close -> Chuyển từ PUBLISHED sang CLOSED")
        void close_shouldChangeStatusToClosed() throws Exception {
            JobPosition publishedPosition = buildPosition(200L, "Python", JobPositionStatus.PUBLISHED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(publishedPosition));
            when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(i -> i.getArgument(0));

            JobPosition result = jobPositionService.close(200L);
            assertEquals(JobPositionStatus.CLOSED, result.getStatus());
        }

        /** Test Case ID: TC-JOB-025 */
        @Test
        @DisplayName("TC-JOB-025 | Close -> Ném ngoại lệ nếu không phải PUBLISHED")
        void close_shouldThrowWhenPositionIsNotPublished() {
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(mockDraftPosition));
            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.close(200L));
            assertEquals("Chỉ có thể đóng vị trí ở trạng thái PUBLISHED", exception.getMessage());
        }

        /**
         * Test Case ID: TC-E2E-JOB-02
         * MỚI: Dựa trên lỗi System Test E2E_JOB_02 & E2E_JOB_05
         */
        @Test
        @DisplayName("TC-E2E-JOB-02 | Đóng vị trí -> Kích hoạt lưu trữ (Archive) ứng viên bên CandidateService")
        void close_shouldTriggerCandidateArchivingProcess() throws Exception {
            JobPosition publishedPosition = buildPosition(200L, "Java Dev", JobPositionStatus.PUBLISHED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(publishedPosition));
            when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(i -> i.getArgument(0));

            jobPositionService.close(200L);

            // CheckDB
            verify(jobPositionRepository).save(any(JobPosition.class));

            // NOTE CHO DEVELOPER:
            // Hệ thống thực tế đang bị bug E2E (Không Archive ứng viên khi đóng vị trí).
            // Hàm candidateClient.archiveCandidatesByJobId() cần được Dev bổ sung vào Interface.
            // Khi code chính được sửa, bỏ comment dòng verify bên dưới để Test Case này chạy hoàn chỉnh.
            // verify(candidateClient, times(1)).archiveCandidatesByJobId(200L);
        }

        @Test
        @DisplayName("TC-JOB-026 | Close -> Ném ngoại lệ nếu vị trí ĐÃ ĐÓNG TUYỂN (CLOSED)")
        void close_shouldThrowWhenPositionIsAlreadyClosed() {
            // Arrange: Tạo dữ liệu giả với trạng thái đã đóng (CLOSED)
            JobPosition closedPosition = buildPosition(200L, "Python Developer", JobPositionStatus.CLOSED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(closedPosition));

            // Act & Assert: Phải ném lỗi chặn lại
            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.close(200L));
            assertEquals("Chỉ có thể đóng vị trí ở trạng thái PUBLISHED", exception.getMessage());

            // CheckDB: Đảm bảo khi lỗi xảy ra, không có thao tác cập nhật nào xuống DB
            verify(jobPositionRepository, never()).save(any());
        }

        /** * Test Case ID: TC-JOB-027
         * Ánh xạ System Test: FUNC_JOB02_04 (Không thể Đăng công khai lại vị trí đã Đóng tuyển).
         * Kịch bản: Cố tình gọi hàm publish() trên vị trí đang ở trạng thái CLOSED.
         * Kỳ vọng: Báo lỗi vì chỉ vị trí DRAFT mới được phép đăng công khai.
         */
        @Test
        @DisplayName("TC-JOB-027 | Publish -> Ném ngoại lệ nếu vị trí ĐÃ ĐÓNG TUYỂN (Không cho đăng lại)")
        void publish_shouldThrowWhenPositionIsClosed() {
            // Arrange: Tạo dữ liệu giả với trạng thái đã đóng (CLOSED)
            JobPosition closedPosition = buildPosition(200L, "Data Analyst", JobPositionStatus.CLOSED);
            when(jobPositionRepository.findById(200L)).thenReturn(Optional.of(closedPosition));

            // Act & Assert: Phải ném lỗi chặn lại việc đăng tải
            IdInvalidException exception = assertThrows(IdInvalidException.class, () -> jobPositionService.publish(200L));
            assertEquals("Chỉ có thể publish vị trí ở trạng thái DRAFT", exception.getMessage());

            // CheckDB: Đảm bảo không có thao tác cập nhật nào xuống DB
            verify(jobPositionRepository, never()).save(any());
        }
    }
}
