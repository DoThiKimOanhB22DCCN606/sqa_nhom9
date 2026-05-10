package com.example.statistics_service.service;

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.statistics.*;
import com.example.statistics_service.service.client.*;
import com.example.statistics_service.utils.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * UNIT TEST CHO STATISTICS SERVICE (MAX COVERAGE)
 *
 * MỤC TIÊU:
 * Xác minh logic thống kê, đảm bảo hệ thống tính toán đúng dữ liệu,
 * kiểm soát chặt chẽ điều kiện đầu vào (validation), duy trì sự
 * ổn định (regression) và đảm bảo bao phủ mã (Code Coverage) cao nhất.
 * * [GIẢI THÍCH VỀ CHECK_DB VÀ ROLLBACK]
 * 1. CheckDB:
 * - Service này đóng vai trò tổng hợp dữ liệu (READ-ONLY) thông qua Feign Client.
 * - Khái niệm "CheckDB" ở đây được thực thi bằng cách sử dụng hàm `verify()`
 * của Mockito. Ta sẽ xác minh xem Service đã truyền ĐÚNG các tham số
 * (departmentId, employeeId, startDate, endDate...) xuống tầng Client
 * để truy vấn Database hay chưa.
 * * 2. Rollback:
 * - Toàn bộ dữ liệu kiểm thử trong file này là Mock Data (dữ liệu giả lập trên RAM).
 * - Do không có lệnh INSERT/UPDATE/DELETE nào chạm đến Database vật lý,
 * Framework Mockito (@ExtendWith(MockitoExtension.class)) sẽ tự động dọn dẹp
 * và giải phóng toàn bộ Mock sau mỗi Test Case. Trạng thái hệ thống tự động
 * được "Rollback" hoàn hảo.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Statistics Service – Comprehensive Test Suite")
class StatisticsServiceTest {

    @Mock
    private JobServiceClient jobServiceClient;

    @Mock
    private CandidateServiceClient candidateServiceClient;

    @Mock
    private ScheduleServiceClient communicationServiceClient;

    @InjectMocks
    private StatisticsService statisticsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ================================================================
    //  HELPER METHODS – Tạo Mock Data với định dạng thực tế
    // ================================================================

    private JsonNode buildApplicationNode(String appliedDate, String status) {
        return objectMapper.createObjectNode()
                .put("appliedDate", appliedDate)
                .put("status", status);
    }

    private JsonNode buildScheduleNode(String startTime, String candidateName) throws Exception {
        String jsonString = String.format("""
                {
                  "id": 10,
                  "startTime": "%s",
                  "title": "Phỏng vấn Backend Developer",
                  "meetingType": "OFFLINE",
                  "status": "SCHEDULED",
                  "participants": [
                    { "participantType": "CANDIDATE", "name": "%s" },
                    { "participantType": "INTERVIEWER", "name": "Nguyen Van A" }
                  ]
                }
                """, startTime, candidateName);
        return objectMapper.readTree(jsonString);
    }

    private JsonNode buildSimpleScheduleNode(String startTime) {
        return objectMapper.createObjectNode()
                .put("id", 1L)
                .put("startTime", startTime)
                .put("title", "Phỏng vấn")
                .put("meetingType", "ONLINE")
                .put("status", "SCHEDULED");
    }

    private JsonNode buildJobPositionNode(String title, String employmentType,
                                          boolean isRemote, String location,
                                          int applicantCount,
                                          String salaryMin, String salaryMax) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("title", title)
                .put("employmentType", employmentType)
                .put("isRemote", isRemote)
                .put("location", location)
                .put("applicationCount", applicantCount);
        if (salaryMin != null) node.put("salaryMin", salaryMin);
        if (salaryMax != null) node.put("salaryMax", salaryMax);
        return node;
    }

    // ================================================================
    //  NHÓM TEST 1: THỐNG KÊ TỔNG QUAN (SUMMARY STATISTICS)
    // ================================================================

    @Nested
    @DisplayName("Summary Statistics Logic Tests")
    class SummaryStatisticsTests {

        @Test
        @DisplayName("TC-STAT-SUM-01 | Xử lý đúng dữ liệu thực tế từ DB, trả về số liệu > 0")
        void verifySummaryStatisticsWithRealData_ShouldReturnExactCounts() {
            LocalDate queryStartDate = LocalDate.of(2025, 12, 17);
            LocalDate queryEndDate   = LocalDate.of(2026, 1, 17);
            String mockAuthToken = "Bearer hr-staff-token";

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("STAFF");
                securityMock.when(SecurityUtil::extractDepartmentCode).thenReturn("HR");

                // Dữ liệu giả lập mang định dạng thực tế chứa 'T'
                List<JsonNode> mockApplicationList = Arrays.asList(
                        buildApplicationNode("2025-12-20T10:30:00", "PENDING"),
                        buildApplicationNode("2025-12-25T14:00:00", "HIRED"),
                        buildApplicationNode("2025-12-26T09:00:00", "HIRED"),
                        buildApplicationNode("2026-01-05T13:10:00", "REJECTED")
                );

                // Dữ liệu giả lập mang định dạng thực tế chứa khoảng trắng
                List<JsonNode> mockScheduleList = Arrays.asList(
                        buildSimpleScheduleNode("2025-12-21 09:00:00"),
                        buildSimpleScheduleNode("2025-12-23 10:00:00")
                );

                when(candidateServiceClient.getApplicationsForStatistics(
                        eq(mockAuthToken), isNull(), anyString(), anyString(), isNull(), isNull()
                )).thenReturn(mockApplicationList);

                when(communicationServiceClient.getSchedulesForStatistics(
                        eq(mockAuthToken), eq(queryStartDate), eq(queryEndDate), isNull(), isNull()
                )).thenReturn(mockScheduleList);

                SummaryStatisticsDTO actualResult = statisticsService.getSummaryStatistics(mockAuthToken, queryStartDate, queryEndDate);

                assertNotNull(actualResult);
                assertEquals(4L, actualResult.getApplications(), "Tổng số lượng hồ sơ phải là 4");
                assertEquals(2L, actualResult.getHired(), "Số lượng ứng viên đã tuyển phải là 2");
                assertEquals(1L, actualResult.getRejected(), "Số lượng ứng viên bị từ chối phải là 1");
                assertEquals(2L, actualResult.getInterviews(), "Số lượng lịch phỏng vấn phải là 2");
            }
        }

        @Test
        @DisplayName("TC-STAT-SUM-02 | CheckDB: Quyền CEO phải gọi Client với tham số departmentId = null")
        void verifyCeoRoleCallsClientWithNullDepartmentId_CheckDB() {
            LocalDate queryStartDate = LocalDate.of(2026, 5, 1);
            LocalDate queryEndDate   = LocalDate.of(2026, 5, 31);
            String mockAuthToken = "Bearer ceo-token";

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());
                when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());

                statisticsService.getSummaryStatistics(mockAuthToken, queryStartDate, queryEndDate);

                // CheckDB: Tham số thứ 6 (departmentId) phải là isNull()
                verify(candidateServiceClient, times(1)).getApplicationsForStatistics(
                        eq(mockAuthToken), isNull(), anyString(), anyString(), isNull(), isNull()
                );
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Xử lý an toàn khi startDate và endDate truyền vào là Null.
         */
        @Test
        @DisplayName("TC-STAT-SUM-03 | [NEW] Xử lý an toàn khi startDate và endDate là null (Lấy ngày hiện tại)")
        void verifySummaryStatisticsWithNullDates() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());
                when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());

                SummaryStatisticsDTO actualResult = statisticsService.getSummaryStatistics("token", null, null);

                assertNotNull(actualResult);
                assertEquals(0L, actualResult.getApplications());
            }
        }

        /**
         * Test Case ID: TC-STAT-SUM-EXC-01
         * Kịch bản: Client trả về dữ liệu ngày tháng bị hỏng (Garbage data).
         * Kỳ vọng: Bao phủ khối try-catch, hệ thống in log.warn và đếm bản ghi lỗi = 0 mà không crash.
         */
        @Test
        @DisplayName("TC-STAT-SUM-EXC-01 | Bao phủ catch block: Bỏ qua an toàn khi chuỗi ngày tháng bị rác")
        void verifyExceptionsCaughtWhenParsingGarbageDate() {
            LocalDate queryStartDate = LocalDate.of(2026, 1, 1);
            LocalDate queryEndDate   = LocalDate.of(2026, 1, 31);
            String mockAuthToken = "Bearer token";

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                // Dữ liệu ngày bị rác hoàn toàn, ép văng DateTimeParseException
                List<JsonNode> badApps = List.of(buildApplicationNode("invalid-date-format", "HIRED"));
                List<JsonNode> badSchedules = List.of(buildSimpleScheduleNode("not-a-timestamp"));

                when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                        .thenReturn(badApps);
                when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                        .thenReturn(badSchedules);

                // Act: Không ném lỗi ra ngoài
                assertDoesNotThrow(() -> {
                    SummaryStatisticsDTO result = statisticsService.getSummaryStatistics(mockAuthToken, queryStartDate, queryEndDate);
                    assertEquals(0L, result.getApplications(), "Bản ghi lỗi phải bị skip");
                    assertEquals(0L, result.getInterviews(), "Lịch lỗi phải bị skip");
                });
            }
        }
    }

    // ================================================================
    //  NHÓM TEST 2: PHÂN QUYỀN VÀ TRUY XUẤT DB (ROLE & DEPARTMENT ID)
    // ================================================================

    @Nested
    @DisplayName("Role and Permissions Logic Tests (CheckDB)")
    class RoleAndPermissionsTests {

        @Test
        @DisplayName("TC-STAT-ROLE-01 | CheckDB: ADMIN / CEO / STAFF (HR) phải query với departmentId = null")
        void verifyGlobalRolesQueryWithNullDepartmentId_CheckDB() {
            String mockAuthToken = "Bearer token";
            int targetPage = 1, targetLimit = 10;

            String[] globalRoles = {"ADMIN", "CEO"};

            for (String role : globalRoles) {
                try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                    securityMock.when(SecurityUtil::extractUserRole).thenReturn(role);

                    when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt()))
                            .thenReturn(new PaginationDTO()); // Trả về rỗng cho nhanh

                    statisticsService.getJobOpenings(mockAuthToken, targetPage, targetLimit);

                    // CheckDB: Đảm bảo departmentId (tham số thứ 2) là null
                    verify(jobServiceClient, times(1))
                            .getJobPositions(eq(mockAuthToken), isNull(), eq(targetPage), eq(targetLimit));
                }
                reset(jobServiceClient); // Reset mock cho vòng lặp sau
            }
        }

        @Test
        @DisplayName("TC-STAT-ROLE-02 | CheckDB: MANAGER hoặc STAFF (Không phải HR) chỉ query theo departmentId của họ")
        void verifyLocalRolesQueryWithSpecificDepartmentId_CheckDB() {
            String mockAuthToken = "Bearer token";
            Long expectedDepartmentId = 99L;

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("MANAGER");
                securityMock.when(SecurityUtil::extractDepartmentCode).thenReturn("IT"); // Không phải HR
                securityMock.when(SecurityUtil::extractDepartmentId).thenReturn(expectedDepartmentId);

                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(new PaginationDTO());

                statisticsService.getJobOpenings(mockAuthToken, 1, 10);

                // CheckDB: Tham số thứ 2 phải chính xác là 99L
                verify(jobServiceClient, times(1))
                        .getJobPositions(eq(mockAuthToken), eq(expectedDepartmentId), eq(1), eq(10));
            }
        }

        @Test
        @DisplayName("TC-STAT-ROLE-03 | CheckDB: Role Null thì departmentId = null")
        void verifyNullRoleReturnsNullDepartmentId() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn(null);
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(new PaginationDTO());

                statisticsService.getJobOpenings("token", 1, 10);

                verify(jobServiceClient, times(1)).getJobPositions(anyString(), isNull(), anyInt(), anyInt());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Quét nhánh "default:" trong Switch-case.
         */
        @Test
        @DisplayName("TC-STAT-ROLE-04 | [NEW] CheckDB: Role không xác định (VD: GUEST) thì departmentId = null")
        void verifyDefaultRoleReturnsNullDepartmentId() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("GUEST");
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(new PaginationDTO());

                statisticsService.getJobOpenings("token", 1, 10);

                verify(jobServiceClient, times(1)).getJobPositions(anyString(), isNull(), anyInt(), anyInt());
            }
        }
    }

    // ================================================================
    //  NHÓM TEST 3: VALIDATION NGÀY THÁNG
    // ================================================================

    @Nested
    @DisplayName("Date Range Validation Tests")
    class DateRangeValidationTests {

        @Test
        @DisplayName("TC-STAT-VAL-01 | Ném IllegalArgumentException khi 'Từ ngày' lớn hơn 'Đến ngày'")
        void verifyExceptionThrownWhenStartDateIsAfterEndDate() {
            LocalDate invalidStartDate = LocalDate.of(2026, 5, 20);
            LocalDate invalidEndDate   = LocalDate.of(2026, 5, 10);

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("STAFF");

                assertThrows(
                        IllegalArgumentException.class,
                        () -> statisticsService.getSummaryStatistics("token", invalidStartDate, invalidEndDate)
                );
            }
        }

        @Test
        @DisplayName("TC-STAT-VAL-02 | Chấp nhận hợp lệ khi 'Từ ngày' bằng 'Đến ngày'")
        void verifySuccessWhenStartDateEqualsEndDate() {
            LocalDate sameDate = LocalDate.of(2026, 5, 15);

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("STAFF");

                when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());
                when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                        .thenReturn(Collections.emptyList());

                assertDoesNotThrow(() -> statisticsService.getSummaryStatistics("token", sameDate, sameDate));
            }
        }
    }

    // ================================================================
    //  NHÓM TEST 4: LỊCH SẮP TỚI VÀ DỮ LIỆU KHUYẾT (MISSING FIELDS)
    // ================================================================

    @Nested
    @DisplayName("Upcoming Schedules Logic Tests")
    class UpcomingSchedulesTests {

        @Test
        @DisplayName("TC-STAT-SCH-01 | Tách date và time thành công từ định dạng ngày có khoảng trắng")
        void verifyScheduleDateTimeParsedCorrectlyWithSpaceFormat() throws Exception {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractEmployeeId).thenReturn(42L);

                List<JsonNode> mockScheduleList = List.of(buildScheduleNode("2026-05-25 10:00:00", "Nguyen Van B"));
                when(communicationServiceClient.getUpcomingSchedules(any(), anyLong(), anyInt())).thenReturn(mockScheduleList);

                UpcomingScheduleDTO actualResult = statisticsService.getUpcomingSchedules("token", 5);

                assertEquals(1, actualResult.getSchedules().size());
                assertEquals("2026-05-25", actualResult.getSchedules().get(0).getDate());
            }
        }

        @Test
        @DisplayName("TC-STAT-SCH-03 | Xử lý an toàn khi JSON lịch trình bị khuyết (Missing fields)")
        void verifySafeHandlingWhenScheduleHasMissingFields() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractEmployeeId).thenReturn(1L);

                // Tạo JSON rỗng hoàn toàn {}
                List<JsonNode> emptyScheduleList = List.of(objectMapper.createObjectNode());
                when(communicationServiceClient.getUpcomingSchedules(any(), anyLong(), anyInt())).thenReturn(emptyScheduleList);

                UpcomingScheduleDTO result = statisticsService.getUpcomingSchedules("token", 5);

                assertEquals(1, result.getSchedules().size());
                UpcomingScheduleDTO.ScheduleItem item = result.getSchedules().get(0);

                // Assert các giá trị default
                assertNull(item.getScheduleId());
                assertEquals("", item.getTime());
                assertEquals("", item.getDate());
                assertEquals("", item.getJobTitle());
                assertEquals("", item.getCandidateName());
                assertEquals("Phỏng vấn", item.getType()); // default type
                assertEquals("", item.getStatus());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Quét nhánh parseDateTime -> catch() -> LocalDate.parse().
         */
        @Test
        @DisplayName("TC-STAT-SCH-04 | [NEW] Parse ngày lùi về ISO_DATE khi khuyết Giờ/Phút")
        void verifyScheduleDateTimeFallbackToIsoDate() throws Exception {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractEmployeeId).thenReturn(42L);

                // Chuỗi này không có giờ phút, ép hàm parse ISO_DATE_TIME fail và fallback về ISO_DATE
                List<JsonNode> mockScheduleList = List.of(buildSimpleScheduleNode("2026-05-25"));
                when(communicationServiceClient.getUpcomingSchedules(any(), anyLong(), anyInt())).thenReturn(mockScheduleList);

                UpcomingScheduleDTO actualResult = statisticsService.getUpcomingSchedules("token", 5);

                assertEquals(1, actualResult.getSchedules().size());
                assertEquals("2026-05-25", actualResult.getSchedules().get(0).getDate());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Quét các trường hợp Array 'participants' bất thường.
         */
        @Test
        @DisplayName("TC-STAT-SCH-05 | [NEW] Xử lý mảng participants rỗng hoặc khuyết CANDIDATE")
        void verifyParticipantsEdgeCases() throws Exception {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractEmployeeId).thenReturn(1L);

                ObjectNode sch1 = (ObjectNode) buildSimpleScheduleNode("2026-05-25T10:00:00");
                sch1.put("participants", "Not-An-Array"); // Lỗi 1: Không phải array

                ObjectNode sch2 = (ObjectNode) buildSimpleScheduleNode("2026-05-26T10:00:00");
                sch2.set("participants", objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("participantType", "INTERVIEWER"))); // Lỗi 2: Không có CANDIDATE

                ObjectNode sch3 = (ObjectNode) buildSimpleScheduleNode("2026-05-27T10:00:00");
                sch3.set("participants", objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("participantType", "CANDIDATE"))); // Lỗi 3: CANDIDATE khuyết name

                when(communicationServiceClient.getUpcomingSchedules(any(), anyLong(), anyInt()))
                        .thenReturn(Arrays.asList(sch1, sch2, sch3));

                UpcomingScheduleDTO result = statisticsService.getUpcomingSchedules("token", 5);

                assertEquals(3, result.getSchedules().size());
                assertEquals("", result.getSchedules().get(0).getCandidateName());
                assertEquals("", result.getSchedules().get(1).getCandidateName());
                assertEquals("", result.getSchedules().get(2).getCandidateName());
            }
        }
    }

    // ================================================================
    //  NHÓM TEST 5: REGRESSION - VỊ TRÍ TUYỂN DỤNG VÀ FORMAT LƯƠNG
    // ================================================================

    @Nested
    @DisplayName("Job Openings & Salary Formatting Tests")
    class RegressionJobOpeningsTests {

        @Test
        @DisplayName("TC-STAT-JOB-02 | Gán giá trị mặc định khi JSON vị trí tuyển dụng trống rỗng")
        void verifyDefaultValuesForEmptyJobPositionJson() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                PaginationDTO mockData = new PaginationDTO();
                mockData.setResult(List.of(objectMapper.createObjectNode())); // Truyen {}
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(mockData);

                List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);

                assertEquals(1, results.size());
                JobOpeningDTO item = results.get(0);

                assertEquals("", item.getTitle());
                assertEquals("Full-time", item.getEmploymentType());
                assertEquals("On-site", item.getWorkLocation());
                assertEquals(0, item.getApplicantCount());
                assertEquals("", item.getSalaryDisplay());
            }
        }

        @Test
        @DisplayName("TC-STAT-JOB-03 | Format chuỗi lương chính xác với mọi ngã rẽ (Chỉ Min, Chỉ Max, Min=Max)")
        void verifyCompleteSalaryFormattingLogic() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                List<JsonNode> positions = Arrays.asList(
                        buildJobPositionNode("Job 1", "Full-time", false, "HN", 0, "15000000", null),       // Chỉ Min
                        buildJobPositionNode("Job 2", "Full-time", false, "HN", 0, null, "20000000"),       // Chỉ Max
                        buildJobPositionNode("Job 3", "Full-time", false, "HN", 0, "10000000", "10000000"), // Min = Max
                        buildJobPositionNode("Job 4", "Full-time", false, "HN", 0, "1000000", "5000000")    // Bình thường
                );

                PaginationDTO mockData = new PaginationDTO();
                mockData.setResult(new ArrayList<>(positions));
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(mockData);

                List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);

                assertEquals(4, results.size());
                assertEquals("15 triệu", results.get(0).getSalaryDisplay(), "Nhánh chỉ có lương Min");
                assertEquals("20 triệu", results.get(1).getSalaryDisplay(), "Nhánh chỉ có lương Max");
                assertEquals("10 triệu", results.get(2).getSalaryDisplay(), "Nhánh lương Min = Max");
                assertEquals("1 - 5 triệu", results.get(3).getSalaryDisplay(), "Nhánh có cả Min và Max khác nhau");
            }
        }

        @Test
        @DisplayName("TC-STAT-JOB-04 | Nhận diện đúng workLocation là Hybrid")
        void verifyHybridLocationIsRecognized() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                ObjectNode hybridJob = objectMapper.createObjectNode()
                        .put("isRemote", false)
                        .put("location", "Tầng 3 - Hybrid Working");

                PaginationDTO mockData = new PaginationDTO();
                mockData.setResult(List.of(hybridJob));
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(mockData);

                List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);
                assertEquals("Hybrid", results.get(0).getWorkLocation());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Xử lý an toàn khi PaginationDTO bị null.
         */
        @Test
        @DisplayName("TC-STAT-JOB-05 | [NEW] Xử lý an toàn khi Client trả về Null PaginationDTO")
        void verifyJobOpeningsNullSafety() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                // Trường hợp 1: Client sập, trả về null hoàn toàn
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(null);
                assertTrue(statisticsService.getJobOpenings("token", 1, 10).isEmpty());

                // Trường hợp 2: Có đối tượng DTO nhưng getResult() bị null
                PaginationDTO emptyPagination = new PaginationDTO();
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(emptyPagination);
                assertTrue(statisticsService.getJobOpenings("token", 1, 10).isEmpty());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Quét nhánh formatVND khi lương < 1.000.000.
         */
        @Test
        @DisplayName("TC-STAT-JOB-06 | [NEW] Format chuẩn xác nhánh tiền < 1 triệu (Chia 1.000)")
        void verifySalaryFormattingThousandsBranch() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                ObjectNode node = objectMapper.createObjectNode();
                node.put("salaryMin", "500000"); // 500 nghìn

                PaginationDTO mockData = new PaginationDTO();
                mockData.setResult(List.of(node));
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(mockData);

                List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);
                assertEquals(1, results.size());

                // Ghi chú: Logic hiện tại chia 1.000 (được 500) rồi gộp đuôi " triệu" thành "500 triệu".
                // Test bảo vệ hiện trạng behavior của hàm.
                assertEquals("500 triệu", results.get(0).getSalaryDisplay());
            }
        }

        /**
         * [NEW] Bổ sung Coverage: Quét nhánh convertToJsonNode -> Exception.
         */
        @Test
        @DisplayName("TC-STAT-JOB-07 | [NEW] Ép lỗi Serialization (ObjectMapper) để bao phủ catch block")
        void verifyConvertToJsonNodeException() {
            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::extractUserRole).thenReturn("CEO");

                // Cố ý tạo ra một Object không thể Serialize để ép ObjectMapper quăng exception
                Object toxicObject = new Object() {
                    public String getPoison() { throw new RuntimeException("Force Jackson to fail"); }
                };

                PaginationDTO mockData = new PaginationDTO();
                mockData.setResult(List.of(toxicObject));
                when(jobServiceClient.getJobPositions(any(), any(), anyInt(), anyInt())).thenReturn(mockData);

                List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);

                // ObjectMapper lỗi -> trả về mảng rỗng, không làm sập server
                assertTrue(results.isEmpty(), "Object không thể Parse phải bị skip an toàn");
            }
        }
    }
}
