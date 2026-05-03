package com.example.workflow_service.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
/* 
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Bơm một secret key giả (phải đủ dài theo chuẩn thuật toán HS256) và thời gian hết hạn vào để test
        ReflectionTestUtils.setField(jwtUtil, "secret", "DayLaMotChuoiBaoMatCucKyDaiVaKhoDoanChoHeThongTuyenDung2026!@#");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 tiếng (3600000 ms)
    }

    @Test
    void kiemTra_TaoVaGiaiMaToken_PhaiChinhXac() {
        // 1. Arrange: Chuẩn bị một email HR
        String email = "hr_manager@company.com";

        // 2. Act: Tạo token từ email, sau đó lại dùng token đó để giải mã ngược ra email
        String token = jwtUtil.generateToken(email);
        String extractedEmail = jwtUtil.extractUsername(token);

        // 3. Assert: NFR-02 - Kiểm tra tính toàn vẹn của dữ liệu
        assertNotNull(token, "Token không được phép Null");
        assertEquals(email, extractedEmail, "Email giải mã ra phải khớp 100% với email ban đầu");
    }

    @Test
    void kiemTra_TokenGiaMao_PhaiBiTuChoi() {
        // 1. Arrange: Tạo ra một token bậy bạ (Giả lập hacker cố tình đổi mã)
        String fakeToken = "eyJIUzI1NiJ9.FakePayload.FakeSignature";

        // 2 & 3. Act & Assert: Negative Testing
        // Hệ thống BẮT BUỘC phải ném ra ngoại lệ (Exception) khi token bị sai lệch
        assertThrows(Exception.class, () -> {
            jwtUtil.isTokenValid(fakeToken, "user@test.com");
        }, "Hệ thống phải văng lỗi đánh chặn ngay lập tức khi phát hiện Token giả mạo");
    }
        */
}