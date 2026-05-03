package com.example.statistics_service.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryStatisticsDTO {
    private Long applications; // Hồ sơ ứng tuyển
    private Long hired; // Tuyển
    private Long interviews; // Phỏng vấn
    private Long rejected; // Từ chối
}
