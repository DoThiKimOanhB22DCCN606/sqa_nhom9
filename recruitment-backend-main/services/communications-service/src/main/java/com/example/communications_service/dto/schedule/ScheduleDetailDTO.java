package com.example.communications_service.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.example.communications_service.utils.enums.MeetingType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailDTO {
    private Long id;
    private String title;
    private String description;
    private String format;
    private MeetingType meetingType;
    private String status;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String timezone;
    private Integer reminderTime;
    private Long roomId;
    private Long createdById; // Employee Id (người tạo lịch hẹn)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ScheduleParticipantDTO> participants;
}
