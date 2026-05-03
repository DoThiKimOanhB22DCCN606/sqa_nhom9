package com.example.notification_service.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.example.notification_service.dto.notification.NotificationPayload;
import com.example.notification_service.websocket.SocketIOEventHandler;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocketIOBroadcastService {

    private final SocketIOServer socketIOServer;
    private final SocketIOEventHandler eventHandler;

    public void pushNotification(NotificationPayload payload) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📤 PUSHING NOTIFICATION VIA SOCKET.IO");
        System.out.println("   Notification ID: " + payload.getId());
        System.out.println("   Recipient ID    : " + payload.getRecipientId());
        System.out.println("   Title           : " + payload.getTitle());
        System.out.println("   Event Type      : " + payload.getEventType());

        if (payload.getRecipientId() == null) {
            System.out.println("   ❌ ERROR: Skip notification without recipientId");
            System.out.println("═══════════════════════════════════════════════════════════");
            return;
        }

        String recipientIdStr = String.valueOf(payload.getRecipientId());

        // Find all sessions for this user
        Map<String, String> userSessions = eventHandler.getUserSessions();
        Set<String> userSessionIds = userSessions.entrySet().stream()
                .filter(entry -> recipientIdStr.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        System.out.println("   User ID        : " + recipientIdStr);
        System.out.println("   Active Sessions: " + userSessionIds.size());

        if (userSessionIds.isEmpty()) {
            System.out.println("   ⚠️  WARNING: User '" + recipientIdStr + "' has no active sessions!");
            System.out.println("   ⚠️  Available users: " + userSessions.values().stream().distinct().count());
            userSessions.entrySet().forEach(entry -> {
                System.out.println("      - Session: " + entry.getKey() + " -> User: " + entry.getValue());
            });
        } else {
            // Send to all sessions of this user
            userSessionIds.forEach(sessionId -> {
                SocketIOClient client = socketIOServer.getClient(java.util.UUID.fromString(sessionId));
                if (client != null) {
                    client.sendEvent("notification", payload);
                    System.out.println("   ✅ Sent to session: " + sessionId);
                } else {
                    System.out.println("   ⚠️  Session not found: " + sessionId);
                }
            });
            System.out.println("   ✅ Message sent successfully via Socket.IO");
        }

        System.out.println("═══════════════════════════════════════════════════════════");
    }

    public void publishUnreadCount(Long recipientId, long unreadCount) {
        if (recipientId == null) {
            System.out.println("⚠️  Skip unread count update: recipientId is null");
            return;
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📊 PUSHING UNREAD COUNT UPDATE VIA SOCKET.IO");
        System.out.println("   Recipient ID    : " + recipientId);
        System.out.println("   Unread Count    : " + unreadCount);

        String recipientIdStr = String.valueOf(recipientId);
        Map<String, String> userSessions = eventHandler.getUserSessions();
        Set<String> userSessionIds = userSessions.entrySet().stream()
                .filter(entry -> recipientIdStr.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (userSessionIds.isEmpty()) {
            System.out.println("   ⚠️  WARNING: User has no active sessions");
        } else {
            Map<String, Object> summary = Map.of(
                    "eventType", "NOTIFICATION_SUMMARY",
                    "unread", unreadCount);

            userSessionIds.forEach(sessionId -> {
                try {
                    java.util.UUID sessionUUID = java.util.UUID.fromString(sessionId);
                    SocketIOClient client = socketIOServer.getClient(sessionUUID);
                    if (client != null) {
                        client.sendEvent("notification-summary", summary);
                        System.out.println("   ✅ Unread count sent to session: " + sessionId);
                    }
                } catch (Exception e) {
                    System.out
                            .println("   ❌ Error sending unread count to session " + sessionId + ": " + e.getMessage());
                }
            });
            System.out.println("   ✅ Unread count sent successfully");
        }

        System.out.println("═══════════════════════════════════════════════════════════");
    }

    public void broadcastPresence(Long userId, boolean online, String lastSeen) {
        Map<String, Object> presence = Map.of(
                "userId", userId,
                "online", online,
                "lastSeen", lastSeen);

        // Broadcast to all connected clients
        socketIOServer.getBroadcastOperations().sendEvent("presence", presence);
        System.out.println("📡 Broadcast presence: User " + userId + " is " + (online ? "online" : "offline"));
    }
}
