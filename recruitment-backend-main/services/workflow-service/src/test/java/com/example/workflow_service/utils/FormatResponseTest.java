package com.example.workflow_service.utils;

// Import 2 thư viện quan trọng nhất của JUnit 5
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class FormatResponseTest {

    // Ký hiệu @Test báo cho máy tính biết đây là một kịch bản kiểm thử, không phải hàm bình thường
    @Test
    void kiemTraMoiTruong_PhepCongCoBan() {
        // Bước 1: Arrange (Chuẩn bị dữ liệu)
        int ketQuaMongDoi = 2;
        
        // Bước 2: Act (Thực thi hành động)
        int ketQuaThucTe = 1 + 1;
        
        // Bước 3: Assert (So sánh - Kiểm tra)
        // Cú pháp: assertEquals(Mong đợi, Thực tế, Câu thông báo nếu lỗi)
        assertEquals(ketQuaMongDoi, ketQuaThucTe, "Máy tính tính sai, 1 + 1 phải bằng 2!");
    }
}