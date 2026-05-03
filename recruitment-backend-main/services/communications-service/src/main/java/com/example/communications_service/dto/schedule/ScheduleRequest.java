package com.example.communications_service.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.example.communications_service.utils.enums.MeetingType;

@Data
public class ScheduleRequest {

    @NotBlank(message = "tiêu đề là bắt buộc")
    private String title;

    private String description;

    @NotNull(message = "format là bắt buộc")
    private String format; // "ONLINE", "OFFLINE", "HYBRID"

    @NotNull(message = "loại cuộc họp là bắt buộc")
    private MeetingType meetingType; // "INTERVIEW", "MEETING", "TRAINING", "OTHER"

    private String status; // "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"

    private String location;

    @NotNull(message = "thời gian bắt đầu là bắt buộc")
    private LocalDateTime startTime;

    @NotNull(message = "End t   ime is required")
    private LocalDateTime endTime;

    private String timezone;

    private Integer reminderTime; // minutes before start

    private Long createdById; // Employee Id (người tạo lịch hẹn)

    private Long candidateId;
    private List<Long> employeeIds; // Danh sách Employee Ids tham gia (khi participantType = "USER")
}
