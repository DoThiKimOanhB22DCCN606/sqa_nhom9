package com.example.notification_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.notification_service.dto.PaginationDTO;
import com.example.notification_service.dto.notification.BulkNotificationRequest;
import com.example.notification_service.exception.NotificationNotFoundException;
import com.example.notification_service.messaging.NotificationEvent;
import com.example.notification_service.model.Notification;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.utils.SecurityUtil;

/**
 * Unit test cho {@link NotificationService}.
 *
 * Quy ước tài liệu trong file này:
 * - Mỗi test đều có comment xác định rõ Test Case ID.
 * - CheckDB:
 *   Với các luồng có truy cập/thay đổi DB, việc kiểm tra DB được thực hiện bằng
 *   verify trên các repository mock như {@code findById()}, {@code save()},
 *   {@code countByRecipientIdAndIsReadFalse()} hoặc các hàm query tương ứng.
 * - Rollback:
 *   Đây là unit test dùng Mockito, không ghi xuống DB thật. Vì vậy sau mỗi test,
 *   dữ liệu không bị lưu bền vững và trạng thái được xem như đã quay lại ban đầu.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    @Mock
    private SocketIOBroadcastService socketIOBroadcastService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification buildNotification(Long id, Long recipientId, boolean isRead, String deliveryStatus) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipientId(recipientId);
        notification.setTitle("Thông báo tuyển dụng");
        notification.setMessage("Ứng viên A đã pass phỏng vấn");
        notification.setRead(isRead);
        notification.setDeliveryStatus(deliveryStatus);
        notification.setSentAt(LocalDateTime.of(2026, 4, 18, 10, 0));
        return notification;
    }

    @Nested
    @DisplayName("createNotification()")
    class CreateNotificationTests {

        @Test
        @DisplayName("TC-NS-001 - Tạo notification thành công và push socket")
        void tc_ns_001_createNotification_whenInputIsValid_shouldSaveNotificationAndPushSocketSuccessfully() {
            // Test Case ID: TC-NS-001
            // Mục tiêu: xác minh service tạo, lưu và đẩy socket thành công cho 1 notification.
            // CheckDB:
            // - notificationRepository.save() phải được gọi đúng 1 lần.
            // - dữ liệu notification được lưu phải đúng recipient, title, message, deliveryStatus.
            // Rollback: repository được mock nên không có thay đổi DB thật.

            Long recipientId = 101L;
            String title = "Ứng viên đã pass";
            String message = "Ứng viên Nguyễn Văn A đã pass vòng phỏng vấn.";

            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Notification result = notificationService.createNotification(recipientId, title, message);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(recipientId, result.getRecipientId());
            assertEquals("SENT", result.getDeliveryStatus());
            assertNotNull(result.getSentAt());

            // CheckDB: verify repository.save() receives correct data.
            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(1)).save(notificationCaptor.capture());
            Notification persisted = notificationCaptor.getValue();
            assertEquals(recipientId, persisted.getRecipientId());
            assertEquals(title, persisted.getTitle());
            assertEquals(message, persisted.getMessage());
            assertEquals("SENT", persisted.getDeliveryStatus());

            verify(socketIOBroadcastService, times(1)).pushNotification(any());
        }

        @Test
        @DisplayName("TC-NS-019 - Khong tao notification khi recipientId null")
        void tc_ns_019_createNotification_whenRecipientIdIsNull_shouldThrowExceptionAndNotSaveNotification() {
            // Test Case ID: TC-NS-019
            // Mục tiêu: xác minh service phải chặn tạo notification khi recipientId null.
            // CheckDB:
            // - notificationRepository.save() không được gọi nếu validate đúng.
            // Rollback: repository được mock nên không có thay đổi DB thật.
            assertThrows(RuntimeException.class,
                    () -> notificationService.createNotification(null, "Thong bao", "No recipient"));

            verify(notificationRepository, never()).save(any(Notification.class));
            verify(socketIOBroadcastService, never()).pushNotification(any());
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("TC-NS-002 - Đánh dấu đã đọc thành công khi notification chưa đọc")
        void tc_ns_002_markAsRead_whenNotificationIsUnread_shouldUpdateReadStatusAndPublishUnreadCount() {
            // Test Case ID: TC-NS-002
            // Mục tiêu: xác minh markAsRead cập nhật trạng thái đã đọc và publish unread count.
            // CheckDB:
            // - findById(), save(), countByRecipientIdAndIsReadFalse() phải được gọi đúng.
            // Rollback: không có DB thật bị thay đổi.

            Notification existingNotification = buildNotification(1L, 101L, false, "SENT");

            when(notificationRepository.findById(1L)).thenReturn(Optional.of(existingNotification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationRepository.countByRecipientIdAndIsReadFalse(101L)).thenReturn(2L);

            notificationService.markAsRead(1L);

            assertTrue(existingNotification.isRead());
            assertNotNull(existingNotification.getReadAt());

            // CheckDB
            verify(notificationRepository).findById(1L);
            verify(notificationRepository).save(existingNotification);
            verify(notificationRepository).countByRecipientIdAndIsReadFalse(101L);
            verify(socketIOBroadcastService).publishUnreadCount(101L, 2L);
        }

        @Test
        @DisplayName("TC-NS-003 - Không save lại khi notification đã đọc")
        void tc_ns_003_markAsRead_whenNotificationIsAlreadyRead_shouldNotSaveAgain() {
            // Test Case ID: TC-NS-003
            // Mục tiêu: xác minh notification đã đọc thì không save lại và không publish unread count.
            // CheckDB:
            // - chỉ findById() được gọi, save() không được gọi.
            // Rollback: không dùng DB thật.
            Notification existingNotification = buildNotification(2L, 101L, true, "SENT");
            existingNotification.setReadAt(LocalDateTime.now().minusDays(1));

            when(notificationRepository.findById(2L)).thenReturn(Optional.of(existingNotification));

            notificationService.markAsRead(2L);

            verify(notificationRepository).findById(2L);
            verify(notificationRepository, never()).save(any(Notification.class));
            verify(socketIOBroadcastService, never()).publishUnreadCount(anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-NS-004 - Ném lỗi khi notification không tồn tại")
        void tc_ns_004_markAsRead_whenNotificationIsNotFound_shouldThrowNotificationNotFoundException() {
            // Test Case ID: TC-NS-004
            // Mục tiêu: xác minh service ném lỗi khi notification không tồn tại.
            // CheckDB:
            // - save() không được gọi.
            // Rollback: không có dữ liệu thật cần phục hồi.
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotificationNotFoundException.class, () -> notificationService.markAsRead(999L));

            verify(notificationRepository).findById(999L);
            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("TC-NS-005 - Đánh dấu tất cả đã đọc và push unread count = 0")
        void tc_ns_005_markAllAsRead_whenNotificationsExist_shouldUpdateAllAndPublishZeroUnread() {
            // Test Case ID: TC-NS-005
            // Mục tiêu: xác minh markAllAsRead cập nhật tất cả notification và publish unread = 0.
            // CheckDB:
            // - markAllAsReadByRecipientId() phải được gọi đúng recipientId.
            // Rollback: mock repository nên không có thay đổi DB thật.
            when(notificationRepository.markAllAsReadByRecipientId(eq(101L), any(LocalDateTime.class))).thenReturn(3);

            int updatedCount = notificationService.markAllAsRead(101L);

            assertEquals(3, updatedCount);
            verify(notificationRepository).markAllAsReadByRecipientId(eq(101L), any(LocalDateTime.class));
            verify(socketIOBroadcastService).publishUnreadCount(101L, 0L);
        }

        @Test
        @DisplayName("TC-NS-006 - Không push unread count khi không có bản ghi nào được cập nhật")
        void tc_ns_006_markAllAsRead_whenNoNotificationIsUpdated_shouldNotPublishUnreadCount() {
            // Test Case ID: TC-NS-006
            // Mục tiêu: xác minh không publish unread count khi không có bản ghi nào được cập nhật.
            // CheckDB:
            // - markAllAsReadByRecipientId() vẫn được gọi, publishUnreadCount() không được gọi.
            // Rollback: không có thay đổi DB thật.
            when(notificationRepository.markAllAsReadByRecipientId(eq(101L), any(LocalDateTime.class))).thenReturn(0);

            int updatedCount = notificationService.markAllAsRead(101L);

            assertEquals(0, updatedCount);
            verify(socketIOBroadcastService, never()).publishUnreadCount(anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-NS-020 - Khong mark all as read khi recipientId null")
        void tc_ns_020_markAllAsRead_whenRecipientIdIsNull_shouldThrowExceptionAndNotUpdateDatabase() {
            // Test Case ID: TC-NS-020
            // Mục tiêu: xác minh service phải chặn markAllAsRead khi recipientId null.
            // CheckDB:
            // - notificationRepository.markAllAsReadByRecipientId() không được gọi nếu validate đúng.
            // Rollback: không có thay đổi DB thật.
            assertThrows(RuntimeException.class, () -> notificationService.markAllAsRead(null));

            verify(notificationRepository, never()).markAllAsReadByRecipientId(any(), any(LocalDateTime.class));
            verify(socketIOBroadcastService, never()).publishUnreadCount(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("getAllNotificationsWithFilters() and stats")
    class QueryTests {

        @Test
        @DisplayName("TC-NS-022 - Lay danh sach notification theo recipient")
        void tc_ns_022_getNotificationsByRecipient_whenRecipientIdIsValid_shouldReturnRecipientNotifications() {
            // Test Case ID: TC-NS-022
            // Muc tieu: xac minh service tra ve dung danh sach notification theo recipient.
            // CheckDB:
            // - notificationRepository.findByRecipientId() phai duoc goi dung 1 lan.
            // Rollback: truy van read-only tren mock repository.
            List<Notification> notifications = List.of(
                    buildNotification(1L, 101L, false, "SENT"),
                    buildNotification(2L, 101L, true, "SENT"));

            when(notificationRepository.findByRecipientId(101L)).thenReturn(notifications);

            List<Notification> result = notificationService.getNotificationsByRecipient(101L);

            assertEquals(2, result.size());
            assertEquals(101L, result.get(0).getRecipientId());
            verify(notificationRepository).findByRecipientId(101L);
        }

        @Test
        @DisplayName("TC-NS-007 - Lọc theo recipientId thành công")
        void tc_ns_007_getAllNotificationsWithFilters_whenRecipientIdIsProvided_shouldFilterByRecipientId() {
            // Test Case ID: TC-NS-007
            // Mục tiêu: xác minh query lọc đúng theo recipientId.
            // CheckDB:
            // - notificationRepository.findByRecipientId() phải được gọi đúng tham số.
            // Rollback: truy vấn read-only, không có thay đổi DB.
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(buildNotification(1L, 101L, false, "SENT")), pageable, 1);

            when(notificationRepository.findByRecipientId(101L, pageable)).thenReturn(page);

            PaginationDTO result = notificationService.getAllNotificationsWithFilters(101L, null, pageable);

            assertNotNull(result);
            assertEquals(1L, result.getMeta().getTotal());
            verify(notificationRepository).findByRecipientId(101L, pageable);
        }

        @Test
        @DisplayName("TC-NS-008 - Lọc theo delivery status thành công")
        void tc_ns_008_getAllNotificationsWithFilters_whenStatusIsProvided_shouldFilterByDeliveryStatus() {
            // Test Case ID: TC-NS-008
            // Mục tiêu: xác minh query lọc đúng theo delivery status.
            // CheckDB:
            // - notificationRepository.findByDeliveryStatus() phải được gọi đúng tham số.
            // Rollback: truy vấn read-only, không có thay đổi DB.
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(buildNotification(1L, 101L, false, "SENT")), pageable, 1);

            when(notificationRepository.findByDeliveryStatus("SENT", pageable)).thenReturn(page);

            PaginationDTO result = notificationService.getAllNotificationsWithFilters(null, "SENT", pageable);

            assertNotNull(result);
            assertEquals(1L, result.getMeta().getTotal());
            verify(notificationRepository).findByDeliveryStatus("SENT", pageable);
        }

        @Test
        @DisplayName("TC-NS-009 - Lấy toàn bộ notification khi không có filter")
        void tc_ns_009_getAllNotificationsWithFilters_whenNoFilterIsProvided_shouldReturnAllNotifications() {
            // Test Case ID: TC-NS-009
            // Mục tiêu: xác minh query trả toàn bộ notification khi không có filter.
            // CheckDB:
            // - notificationRepository.findAll() phải được gọi.
            // Rollback: truy vấn read-only, không thay đổi DB.
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(buildNotification(1L, 101L, false, "SENT")), pageable, 1);

            when(notificationRepository.findAll(pageable)).thenReturn(page);

            PaginationDTO result = notificationService.getAllNotificationsWithFilters(null, null, pageable);

            assertNotNull(result);
            verify(notificationRepository).findAll(pageable);
        }

        @Test
        @DisplayName("TC-NS-010 - Lấy thống kê notification theo recipient")
        void tc_ns_010_getNotificationStats_whenRecipientIdIsProvided_shouldReturnNotificationStatistics() {
            // Test Case ID: TC-NS-010
            // Mục tiêu: xác minh service trả đúng thống kê tổng và chưa đọc.
            // CheckDB:
            // - count() và countByRecipientIdAndIsReadFalse() phải được gọi.
            // Rollback: chỉ đọc dữ liệu, không thay đổi DB.
            when(notificationRepository.count()).thenReturn(10L);
            when(notificationRepository.countByRecipientIdAndIsReadFalse(101L)).thenReturn(4L);

            Map<String, Object> stats = notificationService.getNotificationStats(101L);

            assertEquals(10L, stats.get("totalNotifications"));
            assertEquals(4L, stats.get("unreadNotifications"));
        }

        @Test
        @DisplayName("TC-NS-023 - Lay thong ke notification toan he thong khi recipientId null")
        void tc_ns_023_getNotificationStats_whenRecipientIdIsNull_shouldUseGlobalUnreadCounter() {
            // Test Case ID: TC-NS-023
            // Muc tieu: xac minh service dung countByIsReadFalse khi khong truyen recipientId.
            // CheckDB:
            // - count() va countByIsReadFalse() phai duoc goi.
            // Rollback: truy van read-only tren mock repository.
            when(notificationRepository.count()).thenReturn(20L);
            when(notificationRepository.countByIsReadFalse()).thenReturn(6L);

            Map<String, Object> stats = notificationService.getNotificationStats(null);

            assertEquals(20L, stats.get("totalNotifications"));
            assertEquals(6L, stats.get("unreadNotifications"));
            verify(notificationRepository).countByIsReadFalse();
        }
    }

    @Nested
    @DisplayName("processNotificationEvent()")
    class ProcessNotificationEventTests {

        @Test
        @DisplayName("TC-NS-011 - Gửi cho recipientIds + recipientId và tự loại trùng")
        void tc_ns_011_processNotificationEvent_whenRecipientListContainsDuplicates_shouldResolveDistinctRecipients() {
            // Test Case ID: TC-NS-011
            // Mục tiêu: xác minh service gộp recipientIds và recipientId, loại null và loại trùng.
            // CheckDB:
            // - notificationRepository.save() phải được gọi đúng số recipient duy nhất.
            // Rollback: dùng mock repository nên không có thay đổi DB thật.

            NotificationEvent event = new NotificationEvent();
            event.setTitle("Thông báo phỏng vấn");
            event.setMessage("Ứng viên đã pass");
            event.setRecipientIds(List.of(101L, 102L, 101L));
            event.setRecipientId(103L);

            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.processNotificationEvent(event);

            // 3 users: 101, 102, 103
            verify(notificationRepository, times(3)).save(any(Notification.class));
            verify(socketIOBroadcastService, times(3)).pushNotification(any());
        }

        @Test
        @DisplayName("TC-NS-012 - Gửi cho toàn bộ nhân viên khi includeAllEmployees = true")
        void tc_ns_012_processNotificationEvent_whenIncludeAllEmployeesIsTrue_shouldSendNotificationsToAllEmployees() {
            // Test Case ID: TC-NS-012
            // Mục tiêu: xác minh service resolve toàn bộ employee khi includeAllEmployees = true.
            // CheckDB:
            // - getAllEmployeeIds() phải được gọi.
            // - save() phải được gọi đúng số employee nhận notification.
            // Rollback: không có dữ liệu thật bị thay đổi.
            NotificationEvent event = new NotificationEvent();
            event.setTitle("Thông báo công ty");
            event.setMessage("Họp toàn công ty");
            event.setIncludeAllEmployees(true);
            event.setAuthToken("token-123");

            when(userService.getAllEmployeeIds("token-123")).thenReturn(List.of(1L, 2L, 3L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.processNotificationEvent(event);

            verify(userService).getAllEmployeeIds("token-123");
            verify(notificationRepository, times(3)).save(any(Notification.class));
        }

        @Test
        @DisplayName("TC-NS-013 - Gửi theo department/position filter")
        void tc_ns_013_processNotificationEvent_whenDepartmentAndPositionFiltersAreProvided_shouldSendNotificationsByFilters() {
            // Test Case ID: TC-NS-013
            // Mục tiêu: xác minh service resolve employee theo department/position filter.
            // CheckDB:
            // - getEmployeeIdsByFilters() phải được gọi đúng tham số.
            // - save() phải được gọi đúng số employee được resolve.
            // Rollback: không có thay đổi DB thật.
            NotificationEvent event = new NotificationEvent();
            event.setTitle("Thông báo phòng ban");
            event.setMessage("Phòng ban có lịch họp");
            event.setDepartmentId(10L);
            event.setPositionId(20L);
            event.setAuthToken("token-xyz");

            when(userService.getEmployeeIdsByFilters(10L, 20L, "token-xyz")).thenReturn(List.of(11L, 12L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.processNotificationEvent(event);

            verify(userService).getEmployeeIdsByFilters(10L, 20L, "token-xyz");
            verify(notificationRepository, times(2)).save(any(Notification.class));
        }

        @Test
        @DisplayName("TC-NS-014 - Không tạo notification khi không resolve được recipient")
        void tc_ns_014_processNotificationEvent_whenNoRecipientIsResolved_shouldDoNothing() {
            // Test Case ID: TC-NS-014
            // Mục tiêu: xác minh service không tạo notification khi không resolve được recipient.
            // CheckDB:
            // - save() không được gọi.
            // Rollback: không có dữ liệu thật bị thay đổi.
            NotificationEvent event = new NotificationEvent();
            event.setTitle("Thông báo rỗng");
            event.setMessage("Không có ai nhận");

            notificationService.processNotificationEvent(event);

            verify(notificationRepository, never()).save(any());
            verify(socketIOBroadcastService, never()).pushNotification(any());
        }

        @Test
        @DisplayName("TC-NS-024 - Gui theo position filter khi chi co positionId")
        void tc_ns_024_processNotificationEvent_whenOnlyPositionFilterIsProvided_shouldSendNotificationsByPosition() {
            // Test Case ID: TC-NS-024
            // Muc tieu: xac minh service resolve recipient bang positionId khi departmentId null.
            // CheckDB:
            // - getEmployeeIdsByFilters(null, positionId, token) phai duoc goi.
            // - save() phai duoc goi dung so recipient tim thay.
            // Rollback: mock repository nen khong co thay doi DB that.
            NotificationEvent event = new NotificationEvent();
            event.setTitle("Thong bao vi tri");
            event.setMessage("Cap nhat theo vi tri");
            event.setPositionId(30L);
            event.setAuthToken("token-pos");

            when(userService.getEmployeeIdsByFilters(null, 30L, "token-pos")).thenReturn(List.of(201L, 202L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            notificationService.processNotificationEvent(event);

            verify(userService).getEmployeeIdsByFilters(null, 30L, "token-pos");
            verify(notificationRepository, times(2)).save(any(Notification.class));
            verify(socketIOBroadcastService, times(2)).pushNotification(any());
        }

        @Test
        @DisplayName("TC-NS-021 - Khong tao notification khi event khong co title")
        void tc_ns_021_processNotificationEvent_whenTitleIsNull_shouldThrowExceptionAndNotCreateNotifications() {
            // Test Case ID: TC-NS-021
            // Mục tiêu: xác minh event thiếu title phải bị chặn thay vì tạo notification lỗi dữ liệu.
            // CheckDB:
            // - notificationRepository.save() không được gọi nếu validate đúng.
            // Rollback: mock repository nên không có thay đổi DB thật.
            NotificationEvent event = new NotificationEvent();
            event.setTitle(null);
            event.setMessage("Message");
            event.setRecipientId(101L);

            assertThrows(RuntimeException.class, () -> notificationService.processNotificationEvent(event));

            verify(notificationRepository, never()).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("createBulkNotificationsByConditions()")
    class BulkNotificationTests {

        @Test
        @DisplayName("TC-NS-015 - Gửi bulk cho tất cả nhân viên")
        void tc_ns_015_createBulkNotificationsByConditions_whenIncludeAllEmployeesIsTrue_shouldSendNotificationsToAllEmployees() {
            // Test Case ID: TC-NS-015
            // Mục tiêu: xác minh tạo bulk notification cho toàn bộ employee.
            // CheckDB:
            // - getAllEmployeeIds() phải được gọi.
            // - save() phải được gọi đúng số employee.
            // Rollback: mock repository nên không có thay đổi DB thật.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thông báo hệ thống");
            request.setMessage("Bảo trì hệ thống");
            request.setIncludeAllEmployees(true);

            when(userService.getAllEmployeeIds("jwt-token")).thenReturn(List.of(1L, 2L, 3L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(3, count);
                verify(notificationRepository, times(3)).save(any(Notification.class));
            }
        }

        @Test
        @DisplayName("TC-NS-016 - Gửi bulk theo filter employee")
        void tc_ns_016_createBulkNotificationsByConditions_whenEmployeeFiltersAreProvided_shouldSendNotificationsByFilters() {
            // Test Case ID: TC-NS-016
            // Mục tiêu: xác minh tạo bulk notification theo bộ lọc employee.
            // CheckDB:
            // - getEmployeeIdsByFilters() phải được gọi đúng tham số.
            // - save() phải được gọi đúng số employee được resolve.
            // Rollback: không thay đổi DB thật.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thông báo tuyển dụng");
            request.setMessage("Có đợt tuyển mới");
            request.setDepartmentId(10L);
            request.setPositionId(20L);
            request.setStatus("ACTIVE");
            request.setKeyword("HR");

            when(userService.getEmployeeIdsByFilters(10L, 20L, "ACTIVE", "HR", "jwt-token"))
                    .thenReturn(List.of(101L, 102L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(2, count);
                verify(notificationRepository, times(2)).save(any(Notification.class));
            }
        }

        @Test
        @DisplayName("TC-NS-025 - Gui bulk theo position filter")
        void tc_ns_025_createBulkNotificationsByConditions_whenOnlyPositionFilterIsProvided_shouldResolveByPosition() {
            // Test Case ID: TC-NS-025
            // Muc tieu: xac minh nhanh OR branch khi chi co positionId.
            // CheckDB:
            // - getEmployeeIdsByFilters(...) phai duoc goi voi departmentId = null.
            // Rollback: mock repository nen khong co thay doi DB that.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thong bao theo vi tri");
            request.setMessage("Cap nhat theo vi tri");
            request.setPositionId(22L);

            when(userService.getEmployeeIdsByFilters(null, 22L, null, null, "jwt-token"))
                    .thenReturn(List.of(301L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(1, count);
                verify(userService).getEmployeeIdsByFilters(null, 22L, null, null, "jwt-token");
            }
        }

        @Test
        @DisplayName("TC-NS-026 - Gui bulk theo status filter")
        void tc_ns_026_createBulkNotificationsByConditions_whenOnlyStatusFilterIsProvided_shouldResolveByStatus() {
            // Test Case ID: TC-NS-026
            // Muc tieu: xac minh OR branch khi status co gia tri khong rong.
            // CheckDB:
            // - getEmployeeIdsByFilters(...) phai nhan status.
            // Rollback: mock repository nen khong co thay doi DB that.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thong bao theo status");
            request.setMessage("Status update");
            request.setStatus("ACTIVE");

            when(userService.getEmployeeIdsByFilters(null, null, "ACTIVE", null, "jwt-token"))
                    .thenReturn(List.of(401L, 402L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(2, count);
                verify(userService).getEmployeeIdsByFilters(null, null, "ACTIVE", null, "jwt-token");
            }
        }

        @Test
        @DisplayName("TC-NS-027 - Gui bulk theo keyword filter")
        void tc_ns_027_createBulkNotificationsByConditions_whenOnlyKeywordFilterIsProvided_shouldResolveByKeyword() {
            // Test Case ID: TC-NS-027
            // Muc tieu: xac minh OR branch khi keyword co gia tri khong rong.
            // CheckDB:
            // - getEmployeeIdsByFilters(...) phai nhan keyword.
            // Rollback: mock repository nen khong co thay doi DB that.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thong bao theo keyword");
            request.setMessage("Keyword update");
            request.setKeyword("engineer");

            when(userService.getEmployeeIdsByFilters(null, null, null, "engineer", "jwt-token"))
                    .thenReturn(List.of(501L));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(1, count);
                verify(userService).getEmployeeIdsByFilters(null, null, null, "engineer", "jwt-token");
            }
        }

        @Test
        @DisplayName("TC-NS-028 - Khong gui bulk khi status va keyword rong")
        void tc_ns_028_createBulkNotificationsByConditions_whenStatusAndKeywordAreEmpty_shouldNotResolveFilters() {
            // Test Case ID: TC-NS-028
            // Muc tieu: xac minh nhanh nhanh OR branch false o status/keyword khi chuoi rong.
            // CheckDB:
            // - userService.getEmployeeIdsByFilters(...) khong duoc goi.
            // - save() khong duoc goi.
            // Rollback: khong co thay doi DB that.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thong bao rong");
            request.setMessage("No recipients");
            request.setStatus("");
            request.setKeyword("");
            request.setRecipientIds(List.of());

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(0, count);
                verify(userService, never()).getEmployeeIdsByFilters(any(), any(), any(), any(), any());
                verify(notificationRepository, never()).save(any(Notification.class));
            }
        }

        @Test
        @DisplayName("TC-NS-017 - Gửi bulk theo danh sách recipientIds + recipientId và tự loại trùng")
        void tc_ns_017_createBulkNotificationsByConditions_whenRecipientSourcesContainDuplicates_shouldMergeAndDistinctRecipients() {
            // Test Case ID: TC-NS-017
            // Mục tiêu: xác minh service gộp recipientIds và recipientId, sau đó loại trùng.
            // CheckDB:
            // - save() phải được gọi đúng số recipient duy nhất.
            // Rollback: không có DB thật bị thay đổi.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thông báo riêng");
            request.setMessage("Thông báo tới nhóm nhỏ");
            request.setRecipientIds(List.of(10L, 11L, 10L));
            request.setRecipientId(12L);

            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(3, count);
                verify(notificationRepository, times(3)).save(any(Notification.class));
            }
        }

        @Test
        @DisplayName("TC-NS-018 - Không gửi bulk khi không có recipient nào")
        void tc_ns_018_createBulkNotificationsByConditions_whenNoRecipientIsResolved_shouldReturnZero() {
            // Test Case ID: TC-NS-018
            // Mục tiêu: xác minh service trả 0 khi không resolve được recipient nào.
            // CheckDB:
            // - save() không được gọi.
            // Rollback: không có dữ liệu thật bị thay đổi.
            BulkNotificationRequest request = new BulkNotificationRequest();
            request.setTitle("Thông báo không có người nhận");
            request.setMessage("No recipients");

            try (MockedStatic<SecurityUtil> securityMock = mockStatic(SecurityUtil.class)) {
                securityMock.when(SecurityUtil::getCurrentUserJWT).thenReturn(Optional.of("jwt-token"));

                int count = notificationService.createBulkNotificationsByConditions(request);

                assertEquals(0, count);
                verify(notificationRepository, never()).save(any(Notification.class));
            }
        }
    }
}