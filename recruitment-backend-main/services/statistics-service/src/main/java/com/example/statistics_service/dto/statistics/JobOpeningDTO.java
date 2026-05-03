package com.example.statistics_service.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOpeningDTO {
    private Long id;
    private String title; // Tên vị trí
    // private String iconColor; // Màu icon (purple, green, pink, etc.)
    private String employmentType; // "Full-time", "Part-time"
    private String workLocation; // "On-site", "Hybrid", "Remote"
    private Integer applicantCount; // Số ứng viên
    private BigDecimal salaryMin; // Mức lương tối thiểu
    private BigDecimal salaryMax; // Mức lương tối đa
    private String salaryDisplay; // "25 triệu", "150 - 300 nghìn"
}
