package com.example.schedule_service.dto.schedule;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho thống kê - chỉ chứa dữ liệu cần thiết cho statistics service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleStatisticsDTO {
    private Long id;
    private LocalDateTime startTime;
    private String status;
    private String meetingType;
    private String title;
}
