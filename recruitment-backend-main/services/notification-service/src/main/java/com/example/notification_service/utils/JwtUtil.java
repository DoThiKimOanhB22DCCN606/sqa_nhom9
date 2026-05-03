package com.example.notification_service.utils;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.stereotype.Service;

@Service
public class JwtUtil {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

}
