package com.example.user_service.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.example.user_service.service.PermissionService;
import com.example.user_service.utils.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    private PermissionService permissionService;

    @Override
    @Transactional()
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // Skip CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (path == null) {
            path = request.getRequestURI();
        }
        String httpMethod = request.getMethod();
        Long userId = SecurityUtil.extractUserId();
        // Chuẩn hóa name theo {service}:{resource}:{action}
        String permissionName = extractPermissionName(path, httpMethod);
        System.out.println("permissionName: " + permissionName);
        // Request-scope memoization to avoid duplicate checks in same request
        @SuppressWarnings("unchecked")
        Map<String, Boolean> memo = (Map<String, Boolean>) request.getAttribute("_permMemo");
        if (memo == null) {
            memo = new HashMap<>();
            request.setAttribute("_permMemo", memo);
        }
        Boolean cached = memo.get(permissionName);
        boolean allowed;
        if (cached != null) {
            allowed = cached.booleanValue();
        } else {
            allowed = permissionService.check(permissionName, userId);
            memo.put(permissionName, allowed);
        }

        if (!allowed) {
            throw new AccessDeniedException("Bạn không có quyền truy cập tài nguyên này");
        }

        return true;
    }

    private String extractPermissionName(String path, String httpMethod) {
        try {
            String[] segments = path.split("/");
            // "/api/v1/user-service/departments"
            String service = segments[3]; // user-service
            String resource = segments[4]; // departments

            // Xử lý các endpoint đặc biệt - tất cả đều là "manage"
            if (hasSpecialAction(segments)) {
                return service + ":" + resource + ":manage";
            }

            // Map HTTP method: GET -> "read", các method khác -> "manage"
            String action = mapHttpMethodToAction(httpMethod);
            return service + ":" + resource + ":" + action;
        } catch (Exception e) {
            return "unknown:unknown:unknown";
        }
    }

    private boolean hasSpecialAction(String[] segments) {
        // Kiểm tra các segment sau resource để tìm action đặc biệt
        // Ví dụ: /api/v1/candidate-service/applications/{id}/accept
        // segments: ["", "api", "v1", "candidate-service", "applications", "{id}",
        // "accept"]

        // Kiểm tra từ segment 5 trở đi (sau resource)
        for (int i = 5; i < segments.length; i++) {
            String segment = segments[i];

            // Bỏ qua các segment là path variable hoặc số
            if (segment.startsWith("{") || segment.matches("\\d+")) {
                continue;
            }

            // Các endpoint đặc biệt như approve, publish, close, reject, etc. đều là
            // "manage"
            switch (segment) {
                case "status":
                case "accept":
                case "reject":
                case "publish":
                case "close":
                case "reopen":
                case "stage":
                case "approve":
                case "return":
                case "cancel":
                case "withdraw":
                case "submit":
                case "update-status":
                case "calendar":
                case "initialize":
                case "match":
                case "upload-avatar":
                    return true;
                case "pending":
                case "by-request":
                    // pending và by-request là read operations
                    return false;
                case "actions":
                    // Xử lý /actions/withdraw
                    if (i + 1 < segments.length && "withdraw".equals(segments[i + 1])) {
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    private String mapHttpMethodToAction(String method) {
        return switch (method) {
            case "GET" -> "read";
            case "POST", "PUT", "PATCH", "DELETE" -> "manage";
            default -> "unknown";
        };
    }
}