package com.example.notification_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SocketIOConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String host;

    @Value("${socketio.port:9099}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);
        config.setMaxHttpContentLength(1048576);

        // CORS configuration
        config.setOrigin("*");

        // Authorization handler
        config.setAuthorizationListener(data -> {
            String token = data.getSingleUrlParam("token");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("🔐 SOCKET.IO AUTHORIZATION");
            System.out.println(
                    "   Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            System.out.println("   Remote Address: " + data.getAddress());
            System.out.println("═══════════════════════════════════════════════════════════");

            // Token validation sẽ được xử lý trong event handler
            return token != null && !token.isEmpty();
        });

        SocketIOServer server = new SocketIOServer(config);
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🚀 SOCKET.IO SERVER STARTED");
        System.out.println("   Host: " + host);
        System.out.println("   Port: " + port);
        System.out.println("   Endpoint: http://" + host + ":" + port);
        System.out.println("═══════════════════════════════════════════════════════════");

        return server;
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new SpringAnnotationScanner(socketServer);
    }
}
