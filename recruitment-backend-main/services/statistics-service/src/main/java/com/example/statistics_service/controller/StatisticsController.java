package com.example.statistics_service.controller;

import com.example.statistics_service.dto.statistics.*;
import com.example.statistics_service.service.StatisticsService;
import com.example.statistics_service.utils.SecurityUtil;
import com.example.statistics_service.utils.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/statistics-service/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Lấy thống kê tổng quan với so sánh kỳ trước
     * GET /api/v1/statistics-service/statistics/summary
     * 
     * @param startDate Ngày bắt đầu (mặc định: ngày hiện tại)
     * @param endDate   Ngày kết thúc (mặc định: 7 ngày sau startDate)
     */
    @GetMapping("/summary")
    @ApiMessage("Lấy thống kê tổng quan - Hồ sơ ứng tuyển, Tuyển, Phỏng vấn, Từ chối")
    public ResponseEntity<SummaryStatisticsDTO> getSummaryStatistics(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Optional<String> tokenOpt = SecurityUtil.getCurrentUserJWT();
        String token = tokenOpt.orElse(null);

        SummaryStatisticsDTO statistics = statisticsService.getSummaryStatistics(token, startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Lấy danh sách vị trí tuyển dụng
     * GET /api/v1/statistics-service/statistics/job-openings
     */
    @GetMapping("/job-openings")
    @ApiMessage("Lấy danh sách vị trí tuyển dụng với thông tin chi tiết")
    public ResponseEntity<List<JobOpeningDTO>> getJobOpenings(
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit) {
        Optional<String> tokenOpt = SecurityUtil.getCurrentUserJWT();
        String token = tokenOpt.orElse(null);

        List<JobOpeningDTO> jobOpenings = statisticsService.getJobOpenings(token, page, limit);
        return ResponseEntity.ok(jobOpenings);
    }

    /**
     * Lấy lịch phỏng vấn sắp tới
     * GET /api/v1/statistics-service/statistics/upcoming-schedules
     */
    @GetMapping("/upcoming-schedules")
    @ApiMessage("Lấy lịch phỏng vấn sắp tới")
    public ResponseEntity<UpcomingScheduleDTO> getUpcomingSchedules(
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit) {
        Optional<String> tokenOpt = SecurityUtil.getCurrentUserJWT();
        String token = tokenOpt.orElse(null);

        UpcomingScheduleDTO schedules = statisticsService.getUpcomingSchedules(token, limit);
        return ResponseEntity.ok(schedules);
    }
}
