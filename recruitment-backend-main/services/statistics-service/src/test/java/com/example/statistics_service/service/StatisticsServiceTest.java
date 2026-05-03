package com.example.statistics_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.statistics.JobOpeningDTO;
import com.example.statistics_service.dto.statistics.SummaryStatisticsDTO;
import com.example.statistics_service.dto.statistics.UpcomingScheduleDTO;
import com.example.statistics_service.service.client.CandidateServiceClient;
import com.example.statistics_service.service.client.JobServiceClient;
import com.example.statistics_service.service.client.ScheduleServiceClient;
import com.example.statistics_service.utils.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatisticsServiceTest {

    @Mock
    private JobServiceClient jobServiceClient;

    @Mock
    private CandidateServiceClient candidateServiceClient;

    @Mock
    private ScheduleServiceClient communicationServiceClient;

    @InjectMocks
    private StatisticsService statisticsService;

    private MockedStatic<SecurityUtil> mockedSecurityUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockedSecurityUtil = mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::extractEmployeeId).thenReturn(100L);
        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("CEO");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentCode).thenReturn("IT");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtil.close();
    }

    private JsonNode createJsonNode(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    // ==================== getSummaryStatistics ====================

    @Test
    void getSummaryStatistics_Success_WithValidDates() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 7);

        // Các app bên trong vùng hợp lệ
        JsonNode app1 = createJsonNode("{\"appliedDate\": \"2026-05-02\", \"status\": \"HIRED\"}");
        JsonNode app2 = createJsonNode("{\"appliedDate\": \"2026-05-03\", \"status\": \"REJECTED\"}");
        JsonNode appInvalidDate = createJsonNode("{\"appliedDate\": \"invalid-date\", \"status\": \"HIRED\"}");
        JsonNode appNoDate = createJsonNode("{\"status\": \"HIRED\"}");

        // Bẫy Coverage: Các app TRƯỚC và SAU khoảng thời gian -> Hit branch !isBefore và !isAfter = false
        JsonNode appBeforeStart = createJsonNode("{\"appliedDate\": \"2026-04-30\", \"status\": \"HIRED\"}");
        JsonNode appAfterEnd = createJsonNode("{\"appliedDate\": \"2026-05-08\", \"status\": \"HIRED\"}");

        List<JsonNode> applications = List.of(app1, app2, appInvalidDate, appNoDate, appBeforeStart, appAfterEnd);

        // Tương tự bẫy Coverage với Schedules
        JsonNode schedule1 = createJsonNode("{\"startTime\": \"2026-05-04T10:00:00\"}");
        JsonNode scheduleInvalid = createJsonNode("{\"startTime\": \"invalid\"}");
        JsonNode scheduleNoTime = createJsonNode("{\"title\": \"no time\"}");
        JsonNode scheduleBefore = createJsonNode("{\"startTime\": \"2026-04-30T10:00:00\"}");
        JsonNode scheduleAfter = createJsonNode("{\"startTime\": \"2026-05-08T10:00:00\"}");

        List<JsonNode> schedules = List.of(schedule1, scheduleInvalid, scheduleNoTime, scheduleBefore, scheduleAfter);

        when(candidateServiceClient.getApplicationsForStatistics(anyString(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(applications);
        when(communicationServiceClient.getSchedulesForStatistics(anyString(), any(), any(), any(), any()))
                .thenReturn(schedules);

        SummaryStatisticsDTO result = statisticsService.getSummaryStatistics("token", start, end);

        assertNotNull(result);
        assertEquals(2, result.getApplications()); // Chỉ app1, app2 lọt qua
        assertEquals(1, result.getHired());
        assertEquals(1, result.getRejected());
        assertEquals(1, result.getInterviews());
    }

    @Test
    void getSummaryStatistics_NullDates_UsesDefault() {
        when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Test cả 2 đều null
        SummaryStatisticsDTO resultAllNull = statisticsService.getSummaryStatistics("token", null, null);
        assertNotNull(resultAllNull);

        // Bẫy Coverage: Test start != null nhưng end == null (phá toán tử 3 ngôi)
        SummaryStatisticsDTO resultEndNull = statisticsService.getSummaryStatistics("token", LocalDate.now(), null);
        assertNotNull(resultEndNull);
    }

    @Test
    void getSummaryStatistics_AppNoAppliedDate_InStatusFilter() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 7);

        JsonNode appHiredNoDate = createJsonNode("{\"status\": \"HIRED\"}");
        JsonNode appHiredInvalidDate = createJsonNode("{\"appliedDate\": \"bad\", \"status\": \"HIRED\"}");
        JsonNode appNoStatus = createJsonNode("{\"appliedDate\": \"2026-05-02\"}");
        List<JsonNode> applications = List.of(appHiredNoDate, appHiredInvalidDate, appNoStatus);

        when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                .thenReturn(applications);
        when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        SummaryStatisticsDTO result = statisticsService.getSummaryStatistics("token", start, end);

        assertNotNull(result);
        assertEquals(0, result.getHired());
        assertEquals(0, result.getRejected());
    }

    // ==================== getDepartmentIdForStatistics ====================

    @Test
    void getSummaryStatistics_RoleBranches() {
        LocalDate today = LocalDate.now();

        when(candidateServiceClient.getApplicationsForStatistics(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(communicationServiceClient.getSchedulesForStatistics(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn(null);
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("ADMIN");
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("STAFF");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentCode).thenReturn("HR");
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("STAFF");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentCode).thenReturn("IT");
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("MANAGER");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentCode).thenReturn("HR");
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("MANAGER");
        mockedSecurityUtil.when(SecurityUtil::extractDepartmentCode).thenReturn("IT");
        statisticsService.getSummaryStatistics("token", today, today);

        mockedSecurityUtil.when(SecurityUtil::extractUserRole).thenReturn("UNKNOWN");
        statisticsService.getSummaryStatistics("token", today, today);

        verify(candidateServiceClient, times(7)).getApplicationsForStatistics(any(), any(), any(), any(), any(), any());
    }

    // ==================== getJobOpenings ====================

    @Test
    void getJobOpenings_NullOrEmptyResult() {
        when(jobServiceClient.getJobPositions(anyString(), any(), anyInt(), anyInt())).thenReturn(null);
        assertTrue(statisticsService.getJobOpenings("token", 1, 10).isEmpty());

        PaginationDTO emptyPagination = new PaginationDTO();
        emptyPagination.setResult(null);
        when(jobServiceClient.getJobPositions(anyString(), any(), anyInt(), anyInt())).thenReturn(emptyPagination);
        assertTrue(statisticsService.getJobOpenings("token", 1, 10).isEmpty());
    }

    @Test
    void getJobOpenings_Success_WithVariousSalariesAndLocations() {
        Map<String, Object> job1 = Map.of(
                "title", "Dev", "isRemote", true, "applicationCount", 5,
                "salaryMin", 10000000, "salaryMax", 20000000
        );
        Map<String, Object> job2 = Map.of(
                "title", "Tester", "location", "Hybrid Office", "employmentType", "Part-time",
                "salaryMin", 15000000, "salaryMax", 15000000
        );
        Map<String, Object> job3 = Map.of(
                "title", "BA", "location", "Hanoi",
                "salaryMin", 12000000
        );
        Map<String, Object> job4 = Map.of(
                "salaryMax", 5000000
        );
        Map<String, Object> job5 = Map.of(
                "title", "HR"
        );
        Map<String, Object> job6 = Map.of(
                "title", "Intern",
                "salaryMin", 500000, "salaryMax", 800000
        );

        // Bẫy Coverage: Ép tạo NullNode cho thư viện Jackson (Cover !posNode.get("salaryMin").isNull() == false)
        Map<String, Object> job7NullNodeCoverage = new HashMap<>();
        job7NullNodeCoverage.put("title", "Explicit Nulls");
        job7NullNodeCoverage.put("salaryMin", null);
        job7NullNodeCoverage.put("salaryMax", null);
        job7NullNodeCoverage.put("isRemote", null);
        job7NullNodeCoverage.put("location", null);

        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setResult(Arrays.asList(job1, job2, job3, job4, job5, job6, job7NullNodeCoverage, null));

        when(jobServiceClient.getJobPositions(anyString(), any(), anyInt(), anyInt())).thenReturn(paginationDTO);

        List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);

        assertEquals(7, results.size());
        assertEquals("Remote", results.get(0).getWorkLocation());
        assertEquals("Hybrid", results.get(1).getWorkLocation());
        assertEquals("On-site", results.get(2).getWorkLocation());
        assertEquals("5 triệu", results.get(3).getSalaryDisplay());
        assertEquals("", results.get(4).getSalaryDisplay());
        assertEquals("500 - 800 triệu", results.get(5).getSalaryDisplay());

        // Job 7 sẽ xử lý fallback location thành On-site do field location là NullNode (asText ra chuỗi "null")
        assertEquals("On-site", results.get(6).getWorkLocation());
        assertEquals("", results.get(6).getSalaryDisplay());
    }

    @Test
    void getJobOpenings_NoLocationField_DefaultsToOnSite() {
        Map<String, Object> job = Map.of("title", "QA", "isRemote", false);
        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setResult(Arrays.asList(job));
        when(jobServiceClient.getJobPositions(anyString(), any(), anyInt(), anyInt())).thenReturn(paginationDTO);

        List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);

        assertEquals(1, results.size());
        assertEquals("On-site", results.get(0).getWorkLocation());
    }

    @Test
    void getJobOpenings_ConvertToJsonNode_Exception() {
        // Ép văng Exception lúc parser để cover vòng catch cuối cùng
        Object exceptionThrowingObj = new Object() {
            public String getFail() { throw new RuntimeException("Force Jackson Fail"); }
        };

        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setResult(Arrays.asList(exceptionThrowingObj));
        when(jobServiceClient.getJobPositions(anyString(), any(), anyInt(), anyInt())).thenReturn(paginationDTO);

        List<JobOpeningDTO> results = statisticsService.getJobOpenings("token", 1, 10);
        assertTrue(results.isEmpty());
    }

    // ==================== getUpcomingSchedules ====================

    @Test
    void getUpcomingSchedules_Success_VariousData() {
        JsonNode schedule1 = createJsonNode("{" +
                "\"id\": 1," +
                "\"startTime\": \"2026-05-01T14:30:00\"," +
                "\"title\": \"Interview BE\"," +
                "\"meetingType\": \"Online\"," +
                "\"status\": \"SCHEDULED\"," +
                "\"participants\": [{\"participantType\": \"CANDIDATE\", \"name\": \"Nguyen Van A\"}]" +
                "}");

        JsonNode schedule2 = createJsonNode("{" +
                "\"startTime\": \"2026-05-02\"," +
                "\"participants\": [{\"participantType\": \"INTERVIEWER\", \"name\": \"B\"}]" +
                "}");

        JsonNode schedule3 = createJsonNode("{}");

        JsonNode schedule4 = createJsonNode("{" +
                "\"startTime\": \"2026-05-03T09:00:00\"," +
                "\"participants\": [{\"participantType\": \"CANDIDATE\"}]" +
                "}");

        JsonNode schedule5 = createJsonNode("{" +
                "\"startTime\": \"2026-05-04T10:00:00\"," +
                "\"participants\": {\"key\": \"not array\"}" +
                "}");

        JsonNode schedule6 = createJsonNode("{" +
                "\"startTime\": \"2026-05-05T10:00:00\"," +
                "\"participants\": [{\"name\": \"Unknown\"}]" +
                "}");

        List<JsonNode> mockSchedules = Arrays.asList(schedule1, schedule2, schedule3, schedule4, schedule5, schedule6, null);

        when(communicationServiceClient.getUpcomingSchedules(anyString(), anyLong(), anyInt()))
                .thenReturn(mockSchedules);

        UpcomingScheduleDTO result = statisticsService.getUpcomingSchedules("token", 10);

        assertNotNull(result);
        assertEquals(6, result.getSchedules().size());
        assertEquals("", result.getSchedules().get(3).getCandidateName());
        assertEquals("", result.getSchedules().get(4).getCandidateName());
        assertEquals("", result.getSchedules().get(5).getCandidateName());
    }

    @Test
    void getUpcomingSchedules_StartTimeUnparseable_ReturnsEmptyTimeAndDate() {
        JsonNode scheduleUnparseable = createJsonNode("{\"startTime\": \"not-a-date-at-all\"}");
        when(communicationServiceClient.getUpcomingSchedules(anyString(), anyLong(), anyInt()))
                .thenReturn(List.of(scheduleUnparseable));

        UpcomingScheduleDTO result = statisticsService.getUpcomingSchedules("token", 10);

        assertNotNull(result);
        assertEquals(1, result.getSchedules().size());
        assertEquals("", result.getSchedules().get(0).getTime());
        assertEquals("", result.getSchedules().get(0).getDate());
    }

    // ==================== Unreachable Branches / Dead Code ====================

    @Test
    void formatVND_NullAmount_DeadCodeCoverage() throws Exception {
        // Dùng Reflection để triệu hồi đoạn "code rác" formatVND(null) vốn dĩ không bao giờ được gọi
        java.lang.reflect.Method formatVndMethod = StatisticsService.class.getDeclaredMethod("formatVND", BigDecimal.class);
        formatVndMethod.setAccessible(true);
        String result = (String) formatVndMethod.invoke(statisticsService, (BigDecimal) null);

        assertEquals("0", result);
    }
}