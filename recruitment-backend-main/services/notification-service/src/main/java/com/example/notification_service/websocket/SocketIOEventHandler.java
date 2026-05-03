package com.example.notification_service.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class SocketIOEventHandler {

    private final SocketIOServer socketIOServer;
    private final JwtDecoder jwtDecoder;

    // Store user sessions: sessionId -> userId
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    @Autowired
    public SocketIOEventHandler(SocketIOServer socketIOServer,
            JwtDecoder jwtDecoder) {
        this.socketIOServer = socketIOServer;
        this.jwtDecoder = jwtDecoder;
    }

    @PostConstruct
    public void start() {
        socketIOServer.start();
    }

    @PreDestroy
    public void stop() {
        socketIOServer.stop();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        String token = client.getHandshakeData().getSingleUrlParam("token");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🔌 SOCKET.IO CLIENT CONNECTED");
        System.out.println("   Session ID    : " + sessionId);
        System.out.println("   Token         : "
                + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        System.out.println("   Remote Address: " + client.getRemoteAddress());

        if (token != null && !token.isEmpty()) {
            try {
                // Extract user info from JWT
                Long employeeId = extractEmployeeIdFromToken(token);
                Long userId = extractUserIdFromToken(token);

                Long principalId = employeeId != null ? employeeId : userId;

                if (principalId != null) {
                    String userIdStr = principalId.toString();
                    userSessions.put(sessionId, userIdStr);
                    client.set("userId", userIdStr);
                    client.set("employeeId", employeeId);

                    System.out.println("   User ID       : " + userIdStr);
                    System.out.println("   Employee ID   : " + (employeeId != null ? employeeId : "N/A"));
                    System.out.println("   ✅ Client authenticated and registered");
                } else {
                    System.out.println("   ⚠️  WARNING: Could not extract user ID from token");
                }
            } catch (Exception e) {
                System.out.println("   ❌ ERROR parsing token: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("   ⚠️  WARNING: No token provided");
        }

        System.out.println("   📊 Total active sessions: " + userSessions.size());
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        String userId = userSessions.remove(sessionId);

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🔌 SOCKET.IO CLIENT DISCONNECTED");
        System.out.println("   Session ID    : " + sessionId);
        System.out.println("   User ID       : " + (userId != null ? userId : "unknown"));
        System.out.println("   📊 Remaining sessions: " + userSessions.size());
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    @OnEvent("subscribe")
    public void onSubscribe(SocketIOClient client, String data, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        String userId = client.get("userId");

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📥 SOCKET.IO SUBSCRIBE EVENT");
        System.out.println("   Session ID    : " + sessionId);
        System.out.println("   User ID       : " + userId);
        System.out.println("   Data          : " + data);
        System.out.println("═══════════════════════════════════════════════════════════");

        if (ackRequest != null) {
            ackRequest.sendAckData("subscribed");
        }
    }

    private Long extractEmployeeIdFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            @SuppressWarnings("unchecked")
            Map<String, Object> user = jwt.getClaim("user");
            if (user != null && user.containsKey("employeeId")) {
                Object empId = user.get("employeeId");
                if (empId instanceof Number) {
                    return ((Number) empId).longValue();
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting employeeId: " + e.getMessage());
        }
        return null;
    }

    private Long extractUserIdFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            @SuppressWarnings("unchecked")
            Map<String, Object> user = jwt.getClaim("user");
            if (user != null && user.containsKey("userId")) {
                Object uid = user.get("userId");
                if (uid instanceof Number) {
                    return ((Number) uid).longValue();
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting userId: " + e.getMessage());
        }
        return null;
    }

    public Map<String, String> getUserSessions() {
        return userSessions;
    }
}
