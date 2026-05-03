package com.example.notification_service.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BulkNotificationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    // Gửi cho tất cả nhân viên công ty
    private Boolean includeAllEmployees = false;

    // Gửi cho nhân viên trong phòng ban cụ thể
    private Long departmentId;

    // Gửi cho nhân viên có vị trí cụ thể
    private Long positionId;

    // Gửi cho nhân viên có status cụ thể (ACTIVE, INACTIVE, etc.)
    private String status;

    // Gửi cho danh sách nhân viên cụ thể
    private List<Long> recipientIds;

    // Gửi cho một nhân viên cụ thể
    private Long recipientId;

    // Keyword để tìm kiếm nhân viên (tên, email, phone, etc.)
    private String keyword;
}

