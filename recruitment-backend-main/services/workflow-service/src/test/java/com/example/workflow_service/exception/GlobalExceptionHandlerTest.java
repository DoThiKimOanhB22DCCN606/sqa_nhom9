package com.example.workflow_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.workflow_service.dto.Response; // Thêm dòng import này để nhận diện cấu trúc JSON của dự án

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void kiemTra_LoiIdInvalidException_PhaiTraVe400_VaCoChuanJSON() {
        // 1. Arrange
        String thongBaoLoi = "ID hồ sơ tuyển dụng không hợp lệ";
        IdInvalidException mockException = new IdInvalidException(thongBaoLoi);

        // 2. Act (ĐÃ SỬA LẠI TÊN HÀM THÀNH handleException cho khớp với code thật)
        ResponseEntity<Response<Object>> response = exceptionHandler.handleException(mockException); 

        // 3. Assert
        assertNotNull(response, "Phản hồi không được Null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Lỗi sai ID phải trả về mã HTTP 400");
        
        assertNotNull(response.getBody(), "Body JSON không được Null");
        // Kiểm tra xem phản hồi có lấy đúng message từ Exception không
        assertTrue(response.getBody().getError().contains(thongBaoLoi), "Phản hồi phải chứa thông báo lỗi để Frontend đọc được");
    }

    @Test
    void kiemTra_LoiExceptionChung_PhaiTraVe500() {
        // 1. Arrange
        Exception mockException = new Exception("Đứt cáp mạng, lỗi hệ thống bất ngờ");

        // 2. Act (ĐÃ SỬA LẠI TÊN HÀM THÀNH handleAllException cho khớp với code thật)
        ResponseEntity<Response<Object>> response = exceptionHandler.handleAllException(mockException);

        // 3. Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "Lỗi hệ thống chung phải báo mã HTTP 500");
    }
}