package com.example.user_service.utils;

import com.example.user_service.dto.login.ResponseLoginDTO;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 1. Khởi tạo một chuỗi Secret Key dạng Base64 dài đủ 64 byte (để thỏa mãn thuật toán HS512 của hệ thống)
        String base64Secret = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNA==";

        // 2. Tạo một "cỗ máy mã hóa" JwtEncoder thật để đưa vào JwtUtil
        byte[] keyBytes = Base64.from(base64Secret).decode();
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secretKey);
        JwtEncoder realEncoder = new NimbusJwtEncoder(jwkSource);

        // 3. Khởi tạo đối tượng JwtUtil với cỗ máy mã hóa ở trên
        jwtUtil = new JwtUtil(realEncoder);

        // 4. Bơm các tham số thời gian vào (Giả lập cấu hình application.properties)
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", base64Secret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600L); // 1 tiếng
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 86400L); // 1 ngày
    }

    @Test
    void kiemTra_TaoToken_ThanhCong() {
        // Arrange: Chuẩn bị email và tạo một đối tượng ảo (Mock) cho UserToken
        String email = "hr_manager@company.com";
        ResponseLoginDTO.UserToken mockUserToken = mock(ResponseLoginDTO.UserToken.class);

        // Act: Gọi hàm tạo token của đội Dev
        String token = jwtUtil.createAccessToken(email, mockUserToken);

        // Assert: Đảm bảo token được tạo ra thành công
        assertNotNull(token, "Token không được phép Null");
        assertTrue(token.startsWith("ey"), "Token JWT chuẩn luôn phải bắt đầu bằng chữ 'ey'");
    }

    @Test
    void kiemTra_TokenGiaMao_PhaiBiTuChoi_DamBaoNFR02() {
        // Arrange: Giả lập một token sai lệch (Ví dụ: Hacker cố tình sửa đuôi token)
        String fakeToken = "eyJhbGciOiJIUzUxMiJ9.FakePayload.FakeSignature_Hacker_Dien_Vao";

        // Act & Assert: Hệ thống BẮT BUỘC phải ném ra lỗi khi cố tình giải mã token giả mạo
        assertThrows(Exception.class, () -> {
            jwtUtil.checkValidRefreshToken(fakeToken);
        }, "Hệ thống phải văng lỗi đánh chặn ngay lập tức khi phát hiện Token bị chỉnh sửa");
    }
}