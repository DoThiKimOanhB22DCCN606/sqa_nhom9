package com.example.notification_service.service;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.example.notification_service.dto.notification.NotificationPayload;
import com.example.notification_service.websocket.SocketIOEventHandler;

/**
 * Unit test cho {@link SocketIOBroadcastService}.
 *
 * Quy ước tài liệu trong file này:
 * - Mỗi test đều có comment xác định rõ Test Case ID.
 * - CheckDB:
 *   Không áp dụng cho service này vì chỉ phát websocket event, không truy cập DB.
 * - Rollback:
 *   Test không tạo kết nối socket thật và không thay đổi persistent data, nên không
 *   có trạng thái cần rollback.
 */
@ExtendWith(MockitoExtension.class)
class SocketIOBroadcastServiceTest {

    @Mock
    private SocketIOServer socketIOServer;

    @Mock
    private SocketIOEventHandler eventHandler;

    @InjectMocks
    private SocketIOBroadcastService socketIOBroadcastService;

    private NotificationPayload buildPayload(Long id, Long recipientId) {
        return NotificationPayload.builder()
                .id(id)
                .recipientId(recipientId)
                .title("Thông báo mới")
                .message("Ứng viên đã pass")
                .eventType("NOTIFICATION_CREATED")
                .read(false)
                .delivered(true)
                .deliveryStatus("SENT")
                .sentAt("2026-04-18T10:00:00")
                .build();
    }

    @Nested
    @DisplayName("pushNotification()")
    class PushNotificationTests {

        @Test
        @DisplayName("TC-SIO-001 - Gửi notification đúng 1 session của đúng user")
        void tc_sio_001_pushNotification_whenRecipientSessionMatches_shouldSendNotificationToMatchingUserSession() {
            // Test Case ID: TC-SIO-001
            // Mục tiêu: xác minh notification được gửi đúng 1 session của đúng recipient.
            // CheckDB: không áp dụng vì service không truy cập DB.
            // Rollback: không có socket thật hay dữ liệu thật bị thay đổi.

            String sessionId = UUID.randomUUID().toString();
            NotificationPayload payload = buildPayload(1L, 101L);
            SocketIOClient client = mock(SocketIOClient.class);

            when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "101"));
            when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(client);

            socketIOBroadcastService.pushNotification(payload);

            verify(socketIOServer).getClient(UUID.fromString(sessionId));
            verify(client, times(1)).sendEvent("notification", payload);
        }

        @Test
        @DisplayName("TC-SIO-002 - Gửi notification cho nhiều session của cùng một user")
        void tc_sio_002_pushNotification_whenUserHasMultipleSessions_shouldSendNotificationToAllSessionsOfSameUser() {
            // Test Case ID: TC-SIO-002
            // Mục tiêu: xác minh notification được gửi tới toàn bộ session của cùng 1 user.
            // CheckDB: không áp dụng.
            // Rollback: không có trạng thái persistent cần phục hồi.
            String sessionId1 = UUID.randomUUID().toString();
            String sessionId2 = UUID.randomUUID().toString();
            NotificationPayload payload = buildPayload(1L, 101L);
            SocketIOClient client1 = mock(SocketIOClient.class);
            SocketIOClient client2 = mock(SocketIOClient.class);

            when(eventHandler.getUserSessions()).thenReturn(Map.of(
                    sessionId1, "101",
                    sessionId2, "101"));
            when(socketIOServer.getClient(UUID.fromString(sessionId1))).thenReturn(client1);
            when(socketIOServer.getClient(UUID.fromString(sessionId2))).thenReturn(client2);

            socketIOBroadcastService.pushNotification(payload);

            verify(client1).sendEvent("notification", payload);
            verify(client2).sendEvent("notification", payload);
        }

        @Test
        @DisplayName("TC-SIO-003 - Không gửi khi recipientId null")
        void tc_sio_003_pushNotification_whenRecipientIdIsNull_shouldSkipSendingNotification() {
            // Test Case ID: TC-SIO-003
            // Mục tiêu: xác minh service bỏ qua khi payload không có recipientId.
            // CheckDB: không áp dụng.
            // Rollback: không có thay đổi dữ liệu thật.
            NotificationPayload payload = buildPayload(1L, null);

            socketIOBroadcastService.pushNotification(payload);

            verifyNoInteractions(eventHandler);
            verifyNoInteractions(socketIOServer);
        }

        @Test
        @DisplayName("TC-SIO-004 - Không crash khi user không có active session")
        void tc_sio_004_pushNotification_whenUserHasNoActiveSession_shouldNotThrowAnyException() {
            // Test Case ID: TC-SIO-004
            // Mục tiêu: xác minh service không crash khi user không có active session.
            // CheckDB: không áp dụng.
            // Rollback: không có dữ liệu hay socket thật cần rollback.
            NotificationPayload payload = buildPayload(1L, 101L);
            when(eventHandler.getUserSessions()).thenReturn(Map.of());

            assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));

            verify(eventHandler).getUserSessions();
            verifyNoMoreInteractions(socketIOServer);
        }

        @Test
        @DisplayName("TC-SIO-005 - Bỏ qua session không tìm thấy client")
        void tc_sio_005_pushNotification_whenSessionClientIsNotFound_shouldSkipSendingToThatSession() {
            // Test Case ID: TC-SIO-005
            // Mục tiêu: xác minh service bỏ qua session khi không tìm thấy client tương ứng.
            // CheckDB: không áp dụng.
            // Rollback: không có thay đổi dữ liệu thật.
            String sessionId = UUID.randomUUID().toString();
            NotificationPayload payload = buildPayload(1L, 101L);

            when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "101"));
            when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(null);

            assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));

            verify(socketIOServer).getClient(UUID.fromString(sessionId));
        }

        @Test
        @DisplayName("TC-SIO-010 - Khong crash khi sessionId khong phai UUID hop le trong pushNotification")
        void tc_sio_010_pushNotification_whenSessionIdIsInvalidUuid_shouldHandleGracefully() {
            // Test Case ID: TC-SIO-010
            // Mục tiêu: xác minh service không crash khi sessionId không phải UUID hợp lệ.
            // CheckDB: không áp dụng.
            // Rollback: không có thay đổi dữ liệu thật.
            NotificationPayload payload = buildPayload(2L, 101L);
            when(eventHandler.getUserSessions()).thenReturn(Map.of("invalid-uuid", "101"));

            assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));
        }
    }

    @Nested
    @DisplayName("publishUnreadCount()")
    class PublishUnreadCountTests {

        @Test
        @DisplayName("TC-SIO-006 - Gửi unread count đúng cho user có session")
        void tc_sio_006_publishUnreadCount_whenRecipientHasActiveSession_shouldSendSummaryToMatchingUserSessions() {
            // Test Case ID: TC-SIO-006
            // Mục tiêu: xác minh unread count được gửi đúng cho session của recipient.
            // CheckDB: không áp dụng.
            // Rollback: không có thay đổi persistent data.
            String sessionId = UUID.randomUUID().toString();
            SocketIOClient client = mock(SocketIOClient.class);

            when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "202"));
            when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(client);

            socketIOBroadcastService.publishUnreadCount(202L, 5L);

            ArgumentCaptor<Map> summaryCaptor = ArgumentCaptor.forClass(Map.class);
            verify(client, times(1)).sendEvent(eq("notification-summary"), summaryCaptor.capture());
            Map<?, ?> summary = summaryCaptor.getValue();
            assertEquals("NOTIFICATION_SUMMARY", summary.get("eventType"));
            assertEquals(5L, summary.get("unread"));
        }

        @Test
        @DisplayName("TC-SIO-007 - Không gửi unread count khi recipientId null")
        void tc_sio_007_publishUnreadCount_whenRecipientIdIsNull_shouldSkipSendingSummary() {
            // Test Case ID: TC-SIO-007
            // Mục tiêu: xác minh service bỏ qua khi recipientId null.
            // CheckDB: không áp dụng.
            // Rollback: không có thay đổi dữ liệu thật.
            socketIOBroadcastService.publishUnreadCount(null, 3L);

            verifyNoInteractions(eventHandler);
            verifyNoInteractions(socketIOServer);
        }

        @Test
        @DisplayName("TC-SIO-008 - Không crash khi sessionId không phải UUID hợp lệ")
        void tc_sio_008_publishUnreadCount_whenSessionIdIsInvalidUuid_shouldHandleGracefully() {
            // Test Case ID: TC-SIO-008
            // Mục tiêu: xác minh service không crash khi sessionId không phải UUID hợp lệ.
            // CheckDB: không áp dụng.
            // Rollback: không có trạng thái cần phục hồi.
            when(eventHandler.getUserSessions()).thenReturn(Map.of("not-a-uuid", "202"));

            assertDoesNotThrow(() -> socketIOBroadcastService.publishUnreadCount(202L, 3L));
        }

        @Test
        @DisplayName("TC-SIO-011 - Khong crash khi unread count duoc push toi session co client null")
        void tc_sio_011_publishUnreadCount_whenClientIsNull_shouldNotThrowAnyException() {
            // Test Case ID: TC-SIO-011
            // Mục tiêu: xác minh service không crash khi session hợp lệ nhưng getClient trả về null.
            // CheckDB: không áp dụng.
            // Rollback: không có trạng thái cần phục hồi.
            String sessionId = UUID.randomUUID().toString();
            when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "202"));
            when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(null);

            assertDoesNotThrow(() -> socketIOBroadcastService.publishUnreadCount(202L, 7L));
        }
    }

    @Nested
    @DisplayName("broadcastPresence()")
    class BroadcastPresenceTests {

        @Test
        @DisplayName("TC-SIO-009 - Broadcast presence cho tất cả client")
        void tc_sio_009_broadcastPresence_whenPresenceEventIsTriggered_shouldSendPresenceEventToAllClients() {
            // Test Case ID: TC-SIO-009
            // Mục tiêu: xác minh broadcast presence được gửi tới toàn bộ client.
            // CheckDB: không áp dụng.
            // Rollback: không dùng socket thật, không có dữ liệu cần rollback.
            BroadcastOperations broadcastOperations = mock(BroadcastOperations.class);
            when(socketIOServer.getBroadcastOperations()).thenReturn(broadcastOperations);

            socketIOBroadcastService.broadcastPresence(500L, true, "2026-04-18T10:00:00");

            ArgumentCaptor<Map> presenceCaptor = ArgumentCaptor.forClass(Map.class);
            verify(broadcastOperations, times(1)).sendEvent(eq("presence"), presenceCaptor.capture());
            Map<?, ?> presence = presenceCaptor.getValue();
            assertEquals(500L, presence.get("userId"));
            assertEquals(true, presence.get("online"));
            assertEquals("2026-04-18T10:00:00", presence.get("lastSeen"));
        }

        @Test
        @DisplayName("TC-SIO-012 - Broadcast presence voi lastSeen null van phai xu ly duoc")
        void tc_sio_012_broadcastPresence_whenLastSeenIsNull_shouldHandleGracefully() {
            // Test Case ID: TC-SIO-012
            // Mục tiêu: xác minh service không crash khi lastSeen null.
            // CheckDB: không áp dụng.
            // Rollback: không có dữ liệu cần rollback.
            BroadcastOperations broadcastOperations = mock(BroadcastOperations.class);
            when(socketIOServer.getBroadcastOperations()).thenReturn(broadcastOperations);

            assertDoesNotThrow(() -> socketIOBroadcastService.broadcastPresence(500L, false, null));
        }
    }
}