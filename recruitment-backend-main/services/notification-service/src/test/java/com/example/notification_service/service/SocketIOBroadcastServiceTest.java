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
 * Quy Æ°á»›c tÃ i liá»‡u trong file nÃ y:
 * - Má»—i test Ä‘á»u cÃ³ comment xÃ¡c Ä‘á»‹nh rÃµ Test Case ID.
 * - CheckDB:
 *   KhÃ´ng Ã¡p dá»¥ng cho service nÃ y vÃ¬ chá»‰ phÃ¡t websocket event, khÃ´ng truy cáº­p DB.
 * - Rollback:
 *   Test khÃ´ng táº¡o káº¿t ná»‘i socket tháº­t vÃ  khÃ´ng thay Ä‘á»•i persistent data, nÃªn khÃ´ng
 *   cÃ³ tráº¡ng thÃ¡i cáº§n rollback.
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
                .title("ThÃ´ng bÃ¡o má»›i")
                .message("á»¨ng viÃªn Ä‘Ã£ pass")
                .eventType("NOTIFICATION_CREATED")
                .read(false)
                .delivered(true)
                .deliveryStatus("SENT")
                .sentAt("2026-04-18T10:00:00")
                .build();
    }


    @Test
    @DisplayName("TC-SIO-001 - Gá»­i notification Ä‘Ãºng 1 session cá»§a Ä‘Ãºng user")
    void tc_sio_001_pushNotification_whenRecipientSessionMatches_shouldSendNotificationToMatchingUserSession() {
        // Test Case ID: TC-SIO-001
        // Má»¥c tiÃªu: xÃ¡c minh notification Ä‘Æ°á»£c gá»­i Ä‘Ãºng 1 session cá»§a Ä‘Ãºng recipient.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng vÃ¬ service khÃ´ng truy cáº­p DB.
        // Rollback: khÃ´ng cÃ³ socket tháº­t hay dá»¯ liá»‡u tháº­t bá»‹ thay Ä‘á»•i.

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
    @DisplayName("TC-SIO-002 - Gá»­i notification cho nhiá»u session cá»§a cÃ¹ng má»™t user")
    void tc_sio_002_pushNotification_whenUserHasMultipleSessions_shouldSendNotificationToAllSessionsOfSameUser() {
        // Test Case ID: TC-SIO-002
        // Má»¥c tiÃªu: xÃ¡c minh notification Ä‘Æ°á»£c gá»­i tá»›i toÃ n bá»™ session cá»§a cÃ¹ng 1 user.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ tráº¡ng thÃ¡i persistent cáº§n phá»¥c há»“i.
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
    @DisplayName("TC-SIO-003 - KhÃ´ng gá»­i khi recipientId null")
    void tc_sio_003_pushNotification_whenRecipientIdIsNull_shouldSkipSendingNotification() {
        // Test Case ID: TC-SIO-003
        // Má»¥c tiÃªu: xÃ¡c minh service bá» qua khi payload khÃ´ng cÃ³ recipientId.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ thay Ä‘á»•i dá»¯ liá»‡u tháº­t.
        NotificationPayload payload = buildPayload(1L, null);

        socketIOBroadcastService.pushNotification(payload);

        verifyNoInteractions(eventHandler);
        verifyNoInteractions(socketIOServer);
    }

    @Test
    @DisplayName("TC-SIO-004 - KhÃ´ng crash khi user khÃ´ng cÃ³ active session")
    void tc_sio_004_pushNotification_whenUserHasNoActiveSession_shouldNotThrowAnyException() {
        // Test Case ID: TC-SIO-004
        // Má»¥c tiÃªu: xÃ¡c minh service khÃ´ng crash khi user khÃ´ng cÃ³ active session.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ dá»¯ liá»‡u hay socket tháº­t cáº§n rollback.
        NotificationPayload payload = buildPayload(1L, 101L);
        when(eventHandler.getUserSessions()).thenReturn(Map.of());

        assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));

        verify(eventHandler).getUserSessions();
        verifyNoMoreInteractions(socketIOServer);
    }

    @Test
    @DisplayName("TC-SIO-005 - Bá» qua session khÃ´ng tÃ¬m tháº¥y client")
    void tc_sio_005_pushNotification_whenSessionClientIsNotFound_shouldSkipSendingToThatSession() {
        // Test Case ID: TC-SIO-005
        // Má»¥c tiÃªu: xÃ¡c minh service bá» qua session khi khÃ´ng tÃ¬m tháº¥y client tÆ°Æ¡ng á»©ng.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ thay Ä‘á»•i dá»¯ liá»‡u tháº­t.
        String sessionId = UUID.randomUUID().toString();
        NotificationPayload payload = buildPayload(1L, 101L);

        when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "101"));
        when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(null);

        assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));

        verify(socketIOServer).getClient(UUID.fromString(sessionId));
    }

    @Test
    @DisplayName("TC-SIO-006 - Gá»­i unread count Ä‘Ãºng cho user cÃ³ session")
    void tc_sio_006_publishUnreadCount_whenRecipientHasActiveSession_shouldSendSummaryToMatchingUserSessions() {
        // Test Case ID: TC-SIO-006
        // Má»¥c tiÃªu: xÃ¡c minh unread count Ä‘Æ°á»£c gá»­i Ä‘Ãºng cho session cá»§a recipient.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ thay Ä‘á»•i persistent data.
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
    @DisplayName("TC-SIO-007 - KhÃ´ng gá»­i unread count khi recipientId null")
    void tc_sio_007_publishUnreadCount_whenRecipientIdIsNull_shouldSkipSendingSummary() {
        // Test Case ID: TC-SIO-007
        // Má»¥c tiÃªu: xÃ¡c minh service bá» qua khi recipientId null.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ thay Ä‘á»•i dá»¯ liá»‡u tháº­t.
        socketIOBroadcastService.publishUnreadCount(null, 3L);

        verifyNoInteractions(eventHandler);
        verifyNoInteractions(socketIOServer);
    }

    @Test
    @DisplayName("TC-SIO-008 - KhÃ´ng crash khi sessionId khÃ´ng pháº£i UUID há»£p lá»‡")
    void tc_sio_008_publishUnreadCount_whenSessionIdIsInvalidUuid_shouldHandleGracefully() {
        // Test Case ID: TC-SIO-008
        // Má»¥c tiÃªu: xÃ¡c minh service khÃ´ng crash khi sessionId khÃ´ng pháº£i UUID há»£p lá»‡.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ tráº¡ng thÃ¡i cáº§n phá»¥c há»“i.
        when(eventHandler.getUserSessions()).thenReturn(Map.of("not-a-uuid", "202"));

        assertDoesNotThrow(() -> socketIOBroadcastService.publishUnreadCount(202L, 3L));
    }

    @Test
    @DisplayName("TC-SIO-009 - Broadcast presence cho táº¥t cáº£ client")
    void tc_sio_009_broadcastPresence_whenPresenceEventIsTriggered_shouldSendPresenceEventToAllClients() {
        // Test Case ID: TC-SIO-009
        // Má»¥c tiÃªu: xÃ¡c minh broadcast presence Ä‘Æ°á»£c gá»­i tá»›i toÃ n bá»™ client.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng dÃ¹ng socket tháº­t, khÃ´ng cÃ³ dá»¯ liá»‡u cáº§n rollback.
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
    @DisplayName("TC-SIO-010 - EXPECTED FAIL - Khong crash khi sessionId khong phai UUID hop le trong pushNotification")
    void tc_sio_010_pushNotification_whenSessionIdIsInvalidUuid_shouldHandleGracefully() {
        // Test Case ID: TC-SIO-010 - EXPECTED FAIL/Bug exposed: service currently throws for invalid UUID sessionId.
        // Má»¥c tiÃªu: xÃ¡c minh service khÃ´ng crash khi sessionId khÃ´ng pháº£i UUID há»£p lá»‡.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ thay Ä‘á»•i dá»¯ liá»‡u tháº­t.
        NotificationPayload payload = buildPayload(2L, 101L);
        when(eventHandler.getUserSessions()).thenReturn(Map.of("invalid-uuid", "101"));

        assertDoesNotThrow(() -> socketIOBroadcastService.pushNotification(payload));
    }

    @Test
    @DisplayName("TC-SIO-011 - Khong crash khi unread count duoc push toi session co client null")
    void tc_sio_011_publishUnreadCount_whenClientIsNull_shouldNotThrowAnyException() {
        // Test Case ID: TC-SIO-011
        // Má»¥c tiÃªu: xÃ¡c minh service khÃ´ng crash khi session há»£p lá»‡ nhÆ°ng getClient tráº£ vá» null.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ tráº¡ng thÃ¡i cáº§n phá»¥c há»“i.
        String sessionId = UUID.randomUUID().toString();
        when(eventHandler.getUserSessions()).thenReturn(Map.of(sessionId, "202"));
        when(socketIOServer.getClient(UUID.fromString(sessionId))).thenReturn(null);

        assertDoesNotThrow(() -> socketIOBroadcastService.publishUnreadCount(202L, 7L));
    }

    @Test
    @DisplayName("TC-SIO-012 - EXPECTED FAIL - Broadcast presence voi lastSeen null van phai xu ly duoc")
    void tc_sio_012_broadcastPresence_whenLastSeenIsNull_shouldHandleGracefully() {
        // Test Case ID: TC-SIO-012 - EXPECTED FAIL/Bug exposed: service currently throws when lastSeen is null.
        // Má»¥c tiÃªu: xÃ¡c minh service khÃ´ng crash khi lastSeen null.
        // CheckDB: khÃ´ng Ã¡p dá»¥ng.
        // Rollback: khÃ´ng cÃ³ dá»¯ liá»‡u cáº§n rollback.
        BroadcastOperations broadcastOperations = mock(BroadcastOperations.class);
        when(socketIOServer.getBroadcastOperations()).thenReturn(broadcastOperations);

        assertDoesNotThrow(() -> socketIOBroadcastService.broadcastPresence(500L, false, null));
    }
}
