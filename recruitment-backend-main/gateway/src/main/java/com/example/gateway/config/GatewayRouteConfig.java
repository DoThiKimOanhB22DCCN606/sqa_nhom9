package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRouteConfig {

        private static final Logger log = LoggerFactory.getLogger(GatewayRouteConfig.class);

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                // User Service Routes
                                .route("user-service", r -> r.path("/api/v1/user-service/**")
                                                .filters(f -> f.preserveHostHeader()) // Giữ nguyên Host header
                                                .uri("http://localhost:8082"))

                                // Job Service Routes
                                .route("job-service", r -> r.path("/api/v1/job-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8083"))

                                // Candidate Service Routes
                                .route("candidate-service", r -> r.path("/api/v1/candidate-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8084"))
                                
                                // Communications Service Routes
                                .route("schedule-service", r -> r.path("/api/v1/schedule-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8085"))
                                
                                // Workflow Service Routes
                                .route("workflow-service", r -> r.path("/api/v1/workflow-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8086"))
                                
                                // Upload Service Routes
                                .route("upload-service", r -> r.path("/api/v1/upload/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8087"))
                                
                                // Notification Service Routes
                                .route("notification-service", r -> r.path("/api/v1/notification-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8088"))
                                
                                // --- Notification Service WebSocket ---
                                .route("notification-socketio", r -> r.path("/socket.io/**")
                                                .uri("http://localhost:9099"))
                                
                                // Statistics Service Routes
                                .route("statistics-service", r -> r.path("/api/v1/statistics-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8089"))
                                // Email Service Routes
                                .route("email-service", r -> r.path("/api/v1/email-service/**")
                                                .filters(f -> f.preserveHostHeader())
                                                .uri("http://localhost:8090"))
                                .build();
        }

        /**
         * Global Filter để xử lý response từ services trước khi trả về client
         * Đảm bảo gateway là điểm trung gian xử lý tất cả responses
         */
        @Bean
        public GlobalFilter responseProcessingFilter() {
                return new GlobalFilter() {
                        @Override
                        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                                return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                                        ServerHttpResponse response = exchange.getResponse();
                                        
                                        // Log response để tracking
                                        log.debug("Gateway processing response for: {} - Status: {}", 
                                                exchange.getRequest().getURI().getPath(),
                                                response.getStatusCode());
                                        
                                        // Có thể thêm các xử lý khác ở đây:
                                        // - Thêm custom headers
                                        // - Modify response body
                                        // - Logging, monitoring
                                        // - Error handling
                                        
                                        // Đảm bảo response headers được set đúng
                                        response.getHeaders().add("X-Gateway-Processed", "true");
                                }));
                        }
                };
        }
}
