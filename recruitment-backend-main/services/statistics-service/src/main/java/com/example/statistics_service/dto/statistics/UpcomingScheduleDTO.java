package com.example.statistics_service.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingScheduleDTO {
    private List<ScheduleItem> schedules;    // Danh sách lịch sắp tới

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleItem {
        private Long scheduleId;
        private String time;                  // "09:00 AM"
        private String jobTitle;              // "Marketing Executive"
        private String candidateName;         // "Trần Quang Huy"
        private String type;                  // "Phỏng vấn"
        private String status;                // Status của schedule
        private String date;                  // Ngày (YYYY-MM-DD)
        private String priority;              // Priority level (nếu có)
    }
}

