package com.example.job_service.utils;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtUtil {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    public static String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Token không hợp lệ");
    }
}
