package com.example.statistics_service.service;

import com.example.statistics_service.dto.PaginationDTO;
import com.example.statistics_service.dto.statistics.*;
import com.example.statistics_service.service.client.*;
import com.example.statistics_service.utils.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

        private final JobServiceClient jobServiceClient;
        private final CandidateServiceClient candidateServiceClient;
        private final ScheduleServiceClient communicationServiceClient;
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Lấy thống kê tổng quan với so sánh kỳ trước
         * GET /api/v1/statistics-service/statistics/summary
         * 
         * @param token     JWT token
         * @param startDate Ngày bắt đầu (mặc định: ngày hiện tại)
         * @param endDate   Ngày kết thúc (mặc định: 7 ngày sau startDate)
         */
        // @Cacheable(value = "dashboard-stats", key = "'summary-' + #token + '-' +
        // #startDate + '-' + #endDate")
        public SummaryStatisticsDTO getSummaryStatistics(String token, LocalDate startDate, LocalDate endDate) {
                LocalDate today = LocalDate.now();

                // Nếu startDate không được cung cấp, mặc định là ngày hiện tại
                LocalDate periodStart = (startDate != null) ? startDate : today;

                // Nếu endDate không được cung cấp, mặc định là 7 ngày sau startDate
                LocalDate periodEnd = (endDate != null) ? endDate : periodStart.plusDays(7);

                log.info("Fetching summary statistics from {} to {}", periodStart, periodEnd);

                // Lấy departmentId dựa trên role (CEO xem tất cả, MANAGER/STAFF xem phòng ban
                // của họ)
                Long departmentId = getDepartmentIdForStatistics();

                // Gọi API để lấy dữ liệu cho kỳ hiện tại
                String startDateStr = periodStart.format(DateTimeFormatter.ISO_DATE);
                String endDateStr = periodEnd.format(DateTimeFormatter.ISO_DATE);

                List<JsonNode> allApplications = candidateServiceClient.getApplicationsForStatistics(
                                token, null, startDateStr, endDateStr, null, departmentId);
                List<JsonNode> allSchedules = communicationServiceClient.getSchedulesForStatistics(
                                token, periodStart, periodEnd, null, null);

                // Tính toán các chỉ số
                Long applications = (long) filterApplicationsByDateRange(allApplications, periodStart, periodEnd)
                                .size();
                Long hired = (long) filterApplicationsByDateRangeAndStatus(allApplications, periodStart,
                                periodEnd, "HIRED").size();
                Long rejected = (long) filterApplicationsByDateRangeAndStatus(allApplications, periodStart,
                                periodEnd, "REJECTED").size();
                Long interviews = (long) filterSchedulesByDateRange(allSchedules, periodStart, periodEnd).size();

                return SummaryStatisticsDTO.builder()
                                .applications(applications)
                                .hired(hired)
                                .interviews(interviews)
                                .rejected(rejected)
                                .build();
        }

        /**
         * Lấy danh sách vị trí tuyển dụng
         * GET /api/v1/statistics-service/statistics/job-openings
         */
        @Cacheable(value = "dashboard-stats", key = "'job-openings-' + #token + '-' + #page + '-' + #limit")
        public List<JobOpeningDTO> getJobOpenings(String token, int page, int limit) {
                log.info("Fetching job openings");

                // Lấy departmentId dựa trên role để filter
                Long departmentId = getDepartmentIdForStatistics();

                // Gọi job-service với filter theo departmentId
                PaginationDTO jobPositions = jobServiceClient.getJobPositions(token, departmentId, page, limit);
                if (jobPositions == null || jobPositions.getResult() == null) {
                        return new ArrayList<>();
                }

                @SuppressWarnings("unchecked")
                List<Object> positions = (List<Object>) jobPositions.getResult();

                return positions.stream()
                                .map(pos -> {
                                        JsonNode posNode = convertToJsonNode(pos);
                                        if (posNode == null)
                                                return null;

                                        String title = posNode.has("title") ? posNode.get("title").asText() : "";
                                        String employmentType = posNode.has("employmentType")
                                                        ? posNode.get("employmentType").asText()
                                                        : "Full-time";

                                        // Tính workLocation từ location và isRemote
                                        String workLocation = "On-site";
                                        if (posNode.has("isRemote") && posNode.get("isRemote").asBoolean()) {
                                                workLocation = "Remote";
                                        } else if (posNode.has("location")) {
                                                String location = posNode.get("location").asText();
                                                if (location != null && location.contains("Hybrid")) {
                                                        workLocation = "Hybrid";
                                                }
                                        }

                                        Integer applicantCount = posNode.has("applicationCount")
                                                        ? posNode.get("applicationCount").asInt()
                                                        : 0;

                                        BigDecimal salaryMin = posNode.has("salaryMin")
                                                        && !posNode.get("salaryMin").isNull()
                                                                        ? new BigDecimal(posNode.get("salaryMin")
                                                                                        .asText())
                                                                        : null;
                                        BigDecimal salaryMax = posNode.has("salaryMax")
                                                        && !posNode.get("salaryMax").isNull()
                                                                        ? new BigDecimal(posNode.get("salaryMax")
                                                                                        .asText())
                                                                        : null;

                                        String salaryDisplay = formatSalary(salaryMin, salaryMax);

                                        return JobOpeningDTO.builder()
                                                        .title(title)
                                                        .employmentType(employmentType)
                                                        .workLocation(workLocation)
                                                        .applicantCount(applicantCount)
                                                        .salaryMin(salaryMin)
                                                        .salaryMax(salaryMax)
                                                        .salaryDisplay(salaryDisplay)
                                                        .build();
                                })
                                .filter(item -> item != null)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy lịch phỏng vấn sắp tới
         * GET /api/v1/statistics-service/statistics/upcoming-schedules
         */
        @Cacheable(value = "dashboard-stats", key = "'upcoming-schedules-' + #token")
        public UpcomingScheduleDTO getUpcomingSchedules(String token, int limit) {
                log.info("Fetching upcoming schedules");

                // Lấy employeeId của người request để lấy lịch của chính họ
                Long employeeId = SecurityUtil.extractEmployeeId();

                // Lấy lịch của chính người request
                List<JsonNode> schedules = communicationServiceClient.getUpcomingSchedules(token, employeeId, limit);

                List<UpcomingScheduleDTO.ScheduleItem> scheduleItems = schedules.stream()
                                .limit(limit)
                                .map(scheduleObj -> {
                                        JsonNode schedule = convertToJsonNode(scheduleObj);
                                        if (schedule == null)
                                                return null;

                                        Long scheduleId = schedule.has("id") ? schedule.get("id").asLong() : null;

                                        // Parse startTime
                                        String startTimeStr = schedule.has("startTime")
                                                        ? schedule.get("startTime").asText()
                                                        : "";
                                        LocalDateTime startTime = parseDateTime(startTimeStr);
                                        String time = startTime != null
                                                        ? startTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                                        : "";
                                        String date = startTime != null
                                                        ? startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        : "";

                                        // Lấy thông tin job và candidate từ schedule
                                        String jobTitle = schedule.has("title") ? schedule.get("title").asText() : "";
                                        String candidateName = "";
                                        if (schedule.has("participants")) {
                                                JsonNode participants = schedule.get("participants");
                                                if (participants.isArray()) {
                                                        // Tìm participant có participantType = "CANDIDATE"
                                                        for (JsonNode participant : participants) {
                                                                if (participant.has("participantType")
                                                                                && "CANDIDATE".equalsIgnoreCase(
                                                                                                participant.get("participantType")
                                                                                                                .asText())) {
                                                                        if (participant.has("name")) {
                                                                                candidateName = participant.get("name")
                                                                                                .asText();
                                                                        }
                                                                        break;
                                                                }
                                                        }
                                                }
                                        }

                                        String type = schedule.has("meetingType") ? schedule.get("meetingType").asText()
                                                        : "Phỏng vấn";
                                        String status = schedule.has("status") ? schedule.get("status").asText() : "";

                                        return UpcomingScheduleDTO.ScheduleItem.builder()
                                                        .scheduleId(scheduleId)
                                                        .time(time)
                                                        .jobTitle(jobTitle)
                                                        .candidateName(candidateName)
                                                        .type(type)
                                                        .status(status)
                                                        .date(date)
                                                        .build();
                                })
                                .filter(item -> item != null)
                                .collect(Collectors.toList());

                return UpcomingScheduleDTO.builder()
                                .schedules(scheduleItems)
                                .build();
        }

        // ===================================================================
        // HELPER METHODS
        // ===================================================================

        private String formatSalary(BigDecimal min, BigDecimal max) {
                if (min == null && max == null)
                        return "";
                if (min == null)
                        return formatVND(max) + " triệu";
                if (max == null)
                        return formatVND(min) + " triệu";
                if (min.equals(max))
                        return formatVND(min) + " triệu";
                return formatVND(min) + " - " + formatVND(max) + " triệu";
        }

        private String formatVND(BigDecimal amount) {
                if (amount == null)
                        return "0";
                long millions = amount.longValue() / 1_000_000;
                if (millions > 0) {
                        return String.valueOf(millions);
                }
                long thousands = amount.longValue() / 1_000;
                return String.valueOf(thousands);
        }

        /**
         * Lấy departmentId dựa trên role và departmentCode để filter dữ liệu
         * - CEO: null (xem tất cả)
         * - STAFF với departmentCode = "HR": null (xem tất cả - phòng ban nhân sự)
         * - MANAGER/STAFF khác: departmentId của họ (chỉ xem phòng ban của mình)
         */
        private Long getDepartmentIdForStatistics() {
                String role = SecurityUtil.extractUserRole();
                if (role == null) {
                        return null;
                }

                switch (role.toUpperCase()) {
                        case "CEO":
                        case "ADMIN":
                                // CEO và ADMIN xem tất cả phòng ban
                                return null;
                        case "STAFF": {
                                // STAFF với departmentCode = "HR" xem tất cả phòng ban
                                String departmentCode = SecurityUtil.extractDepartmentCode();
                                if ("HR".equalsIgnoreCase(departmentCode)) {
                                        return null; // Phòng ban nhân sự xem tất cả
                                }
                                // STAFF khác chỉ xem phòng ban của mình
                                return SecurityUtil.extractDepartmentId();
                        }
                        case "MANAGER":
                                String departmentCode = SecurityUtil.extractDepartmentCode();
                                if ("HR".equalsIgnoreCase(departmentCode)) {
                                        return null; // xem tất cả
                                }
                                // MANAGER chỉ xem phòng ban của mình
                                return SecurityUtil.extractDepartmentId();
                        default:
                                return null;
                }
        }

        /**
         * Filter applications theo date range
         */
        private List<JsonNode> filterApplicationsByDateRange(List<JsonNode> applications, LocalDate start,
                        LocalDate end) {
                return applications.stream()
                                .filter(app -> {
                                        try {
                                                if (app.has("appliedDate")) {
                                                        String appliedDateStr = app.get("appliedDate").asText();
                                                        LocalDate appliedDate = LocalDate.parse(appliedDateStr);
                                                        return !appliedDate.isBefore(start)
                                                                        && !appliedDate.isAfter(end);
                                                }
                                        } catch (Exception e) {
                                                log.warn("Error parsing application date: {}", e.getMessage());
                                        }
                                        return false;
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Filter applications theo date range và status
         */
        private List<JsonNode> filterApplicationsByDateRangeAndStatus(List<JsonNode> applications,
                        LocalDate start, LocalDate end, String status) {
                return applications.stream()
                                .filter(app -> {
                                        try {
                                                // Check status
                                                if (!app.has("status") || !status.equals(app.get("status").asText())) {
                                                        return false;
                                                }
                                                // Check date
                                                if (app.has("appliedDate")) {
                                                        String appliedDateStr = app.get("appliedDate").asText();
                                                        LocalDate appliedDate = LocalDate.parse(appliedDateStr);
                                                        return !appliedDate.isBefore(start)
                                                                        && !appliedDate.isAfter(end);
                                                }
                                        } catch (Exception e) {
                                                log.warn("Error parsing application: {}", e.getMessage());
                                        }
                                        return false;
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Filter schedules theo date range
         */
        private List<JsonNode> filterSchedulesByDateRange(List<JsonNode> schedules, LocalDate start, LocalDate end) {
                return schedules.stream()
                                .filter(schedule -> {
                                        try {
                                                if (schedule.has("startTime")) {
                                                        String startTimeStr = schedule.get("startTime").asText();
                                                        LocalDateTime startTime = LocalDateTime.parse(startTimeStr,
                                                                        DateTimeFormatter.ISO_DATE_TIME);
                                                        LocalDate scheduleDate = startTime.toLocalDate();
                                                        return !scheduleDate.isBefore(start)
                                                                        && !scheduleDate.isAfter(end);
                                                }
                                        } catch (Exception e) {
                                                log.warn("Error parsing schedule date: {}", e.getMessage());
                                        }
                                        return false;
                                })
                                .collect(Collectors.toList());
        }

        private LocalDateTime parseDateTime(String dateTimeStr) {
                try {
                        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                        try {
                                return LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
                        } catch (Exception ex) {
                                return null;
                        }
                }
        }

        /**
         * Convert Object (LinkedHashMap) to JsonNode
         */
        private JsonNode convertToJsonNode(Object obj) {
                if (obj == null) {
                        return null;
                }
                if (obj instanceof JsonNode) {
                        return (JsonNode) obj;
                }
                try {
                        return objectMapper.valueToTree(obj);
                } catch (Exception e) {
                        log.warn("Error converting object to JsonNode: {}", e.getMessage());
                        return null;
                }
        }

}
