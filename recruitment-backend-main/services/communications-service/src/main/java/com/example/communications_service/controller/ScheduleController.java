package com.example.communications_service.controller;

import com.example.communications_service.dto.PaginationDTO;
import com.example.communications_service.dto.schedule.ScheduleDetailDTO;
import com.example.communications_service.dto.schedule.ScheduleRequest;
import com.example.communications_service.dto.schedule.AvailableParticipantDTO;
import com.example.communications_service.model.Schedule;
import com.example.communications_service.service.ScheduleService;
import com.example.communications_service.utils.SecurityUtil;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/communications-service/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<Schedule> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        request.setCreatedById(SecurityUtil.extractEmployeeId());
        Schedule schedule = scheduleService.createSchedule(request);
        return ResponseEntity.ok(schedule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        request.setCreatedById(SecurityUtil.extractEmployeeId());
        Schedule schedule = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(schedule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok("Schedule deleted successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDetailDTO> getScheduleById(
            @PathVariable Long id) {
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        var data = scheduleService.getScheduleWithParticipantNames(id, token);
        return ResponseEntity.ok(data);
    }

    @GetMapping
    public ResponseEntity<List<ScheduleDetailDTO>> getAllSchedules(
            @RequestParam(name = "day", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam(name = "week", required = false) Integer week,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "meetingType", required = false) String meetingType,
            @RequestParam(name = "participantId", required = false) Long participantId,
            @RequestParam(name = "participantType", required = false) String participantType,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        var data = scheduleService.getSchedulesDetailed(day, week, month, year, status, meetingType, participantId,
                participantType, token, startDate, endDate);
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Schedule> updateScheduleStatus(
            @PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Schedule schedule = scheduleService.updateScheduleStatus(id, status);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getCalendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "participantId", required = false) Long participantId,
            @RequestParam(name = "participantType", required = false) String participantType) {

        // Use the new getAllSchedules method with filters
        PaginationDTO result = scheduleService.getAllSchedules(1, 1000,
                "startTime", "asc",
                startDate, null, null, null, null, participantId, participantType);

        // Filter by end date if needed
        @SuppressWarnings("unchecked")
        List<Schedule> schedules = (List<Schedule>) result.getResult();
        if (endDate != null) {
            schedules = schedules.stream()
                    .filter(s -> !s.getStartTime().toLocalDate().isAfter(endDate))
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "schedules", schedules,
                "startDate", startDate,
                "endDate", endDate,
                "total", schedules.size()));
    }

    @GetMapping("/available-participants")
    public ResponseEntity<List<AvailableParticipantDTO>> getAvailableParticipants(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "excludeScheduleId", required = false) Long excludeScheduleId) {
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);
        List<AvailableParticipantDTO> availableParticipants = scheduleService.getAvailableParticipants(
                startTime, endTime, excludeScheduleId, token);
        return ResponseEntity.ok(availableParticipants);
    }
}
