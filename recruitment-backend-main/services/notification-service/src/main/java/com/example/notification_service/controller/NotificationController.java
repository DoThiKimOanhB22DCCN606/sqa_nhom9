package com.example.notification_service.controller;

import com.example.notification_service.dto.PaginationDTO;
import com.example.notification_service.dto.notification.BulkNotificationRequest;
import com.example.notification_service.dto.notification.NotificationRequest;
import com.example.notification_service.model.Notification;
import com.example.notification_service.service.NotificationService;
import com.example.notification_service.utils.SecurityUtil;
import com.example.notification_service.utils.annotation.ApiMessage;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notification-service/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody NotificationRequest request) {
        Notification notification = notificationService.createNotification(
                request.getRecipientId(),
                request.getTitle(),
                request.getMessage());
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> createBulkNotifications(@Valid @RequestBody List<NotificationRequest> requests) {
        requests.forEach(req -> notificationService.createNotification(
                req.getRecipientId(),
                req.getTitle(),
                req.getMessage()));
        return ResponseEntity.ok("Notifications created successfully");
    }

    /**
     * Gửi thông báo hàng loạt theo các điều kiện
     * Hỗ trợ:
     * - Gửi cho tất cả nhân viên công ty (includeAllEmployees = true)
     * - Gửi cho tất cả nhân viên phòng ban (departmentId)
     * - Gửi cho nhân viên theo vị trí (positionId)
     * - Gửi cho nhân viên theo status (status: ACTIVE, INACTIVE, etc.)
     * - Gửi cho nhân viên theo keyword (tên, email, phone, etc.)
     * - Gửi cho danh sách nhân viên cụ thể (recipientIds)
     * - Gửi cho một nhân viên cụ thể (recipientId)
     * 
     * Có thể kết hợp nhiều điều kiện cùng lúc
     */
    @PostMapping("/bulk-by-conditions")
    @ApiMessage("Gửi thông báo hàng loạt theo điều kiện")
    public ResponseEntity<Map<String, Object>> createBulkNotificationsByConditions(
            @Valid @RequestBody BulkNotificationRequest request) {
        int count = notificationService.createBulkNotificationsByConditions(request);
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi thông báo thành công",
                "notificationsSent", count));
    }

    // Unified GET endpoint for all notifications with filtering, pagination, and
    // sorting
    @GetMapping
    @ApiMessage("Lấy danh sách thông báo")
    public ResponseEntity<PaginationDTO> getAllNotifications(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1", required = false) int page,
            @RequestParam(name = "limit", defaultValue = "10", required = false) int limit,
            @RequestParam(name = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc", required = false) String sortOrder) {

        // Validate pagination parameters
        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;
        Long recipientId = SecurityUtil.extractEmployeeId();
        // Create sort object
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Create pageable object
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        return ResponseEntity.ok(notificationService.getAllNotificationsWithFilters(
                recipientId, status, pageable));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getNotificationsByRecipient(
            @PathVariable Long recipientId) {
        List<Notification> notifications = notificationService.getNotificationsByRecipient(recipientId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{notificationId}/read")
    @ApiMessage("Đánh dấu một thông báo là đã đọc")
    public ResponseEntity<String> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Thông báo đã đọc");
    }

    @PutMapping("/read-all")
    @ApiMessage("Đánh dấu tất cả thông báo của người dùng hiện tại là đã đọc")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Long recipientId = SecurityUtil.extractEmployeeId();
        int updatedCount = notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(Map.of(
                "message", "Đã đánh dấu tất cả thông báo là đã đọc",
                "updatedCount", updatedCount));
    }

    @GetMapping("/stats/{recipientId}")
    public ResponseEntity<Map<String, Object>> getNotificationStats(
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.getNotificationStats(recipientId));
    }
}
