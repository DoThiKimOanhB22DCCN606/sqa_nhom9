package com.example.upload_service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.upload_service.service.CloudinaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CloudinaryService.java
 *
 * Theo docs:
 * TC-UPLOAD-SER-001 -> TC-UPLOAD-SER-024
 */
@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @Test
    @DisplayName("TC-UPLOAD-SER-001 - upload - Upload file PDF hợp lệ")
    void TC_UPLOAD_SER_001() throws IOException {
        byte[] fileBytes = bytes("pdf-content");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "cv_pdf_001",
                "secure_url", "https://res.cloudinary.com/demo/cv.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("cv_pdf_001", result.get("public_id"));
        assertEquals("https://res.cloudinary.com/demo/cv.pdf", result.get("secure_url"));

        verify(multipartFile, times(1)).getBytes();
        verify(cloudinary, times(1)).uploader();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-002 - upload - Upload file ảnh PNG hợp lệ")
    void TC_UPLOAD_SER_002() throws IOException {
        byte[] fileBytes = bytes("png-content");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "avatar_png_001",
                "secure_url", "https://res.cloudinary.com/demo/avatar.png"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("avatar_png_001", result.get("public_id"));
        assertEquals("https://res.cloudinary.com/demo/avatar.png", result.get("secure_url"));

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-003 - upload - Upload file DOCX hợp lệ")
    void TC_UPLOAD_SER_003() throws IOException {
        byte[] fileBytes = bytes("docx-content");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "cv_docx_001",
                "secure_url", "https://res.cloudinary.com/demo/cv.docx"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("cv_docx_001", result.get("public_id"));
        assertEquals("https://res.cloudinary.com/demo/cv.docx", result.get("secure_url"));

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-004 - upload - Upload file rỗng")
    void TC_UPLOAD_SER_004() throws IOException {
        byte[] fileBytes = new byte[0];

        Map<String, Object> uploadResult = resultMap(
                "public_id", "empty_file",
                "secure_url", "https://res.cloudinary.com/demo/empty.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("empty_file", result.get("public_id"));
        assertEquals("https://res.cloudinary.com/demo/empty.pdf", result.get("secure_url"));

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-005 - upload - File có tên null")
    void TC_UPLOAD_SER_005() throws IOException {
        byte[] fileBytes = bytes("file-with-null-name");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "null_name_file",
                "secure_url", "https://res.cloudinary.com/demo/file"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("null_name_file", result.get("public_id"));

        verify(multipartFile, times(1)).getBytes();
        verify(multipartFile, never()).getOriginalFilename();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-006 - upload - File có contentType null")
    void TC_UPLOAD_SER_006() throws IOException {
        byte[] fileBytes = bytes("file-with-null-content-type");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "null_content_type_file",
                "secure_url", "https://res.cloudinary.com/demo/file"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("null_content_type_file", result.get("public_id"));

        verify(multipartFile, times(1)).getBytes();
        verify(multipartFile, never()).getContentType();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-007 - upload - Lỗi đọc file từ MultipartFile")
    void TC_UPLOAD_SER_007() throws IOException {
        when(multipartFile.getBytes())
                .thenThrow(new IOException("Cannot read file"));

        IOException exception = assertThrows(
                IOException.class,
                () -> cloudinaryService.upload(multipartFile)
        );

        assertEquals("Cannot read file", exception.getMessage());

        verify(multipartFile, times(1)).getBytes();
        verify(cloudinary, never()).uploader();
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-008 - upload - Cloudinary upload bị IOException")
    void TC_UPLOAD_SER_008() throws IOException {
        byte[] fileBytes = bytes("cloudinary-error");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap()))
                .thenThrow(new IOException("Cloudinary error"));

        IOException exception = assertThrows(
                IOException.class,
                () -> cloudinaryService.upload(multipartFile)
        );

        assertEquals("Cloudinary error", exception.getMessage());

        verify(multipartFile, times(1)).getBytes();
        verify(cloudinary, times(1)).uploader();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-009 - upload - Cloudinary trả Map thiếu secure_url")
    void TC_UPLOAD_SER_009() throws IOException {
        byte[] fileBytes = bytes("missing-secure-url");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "file_without_secure_url"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertEquals("file_without_secure_url", result.get("public_id"));
        assertFalse(result.containsKey("secure_url"));

        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-010 - upload - Cloudinary trả Map rỗng")
    void TC_UPLOAD_SER_010() throws IOException {
        byte[] fileBytes = bytes("empty-result");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Collections.emptyMap());

        Map result = cloudinaryService.upload(multipartFile);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-011 - upload - File null")
    void TC_UPLOAD_SER_011() {
        assertThrows(
                NullPointerException.class,
                () -> cloudinaryService.upload(null)
        );

        verifyNoInteractions(cloudinary);
        verifyNoInteractions(uploader);
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-012 - uploadFile - Upload CV PDF thành công")
    void TC_UPLOAD_SER_012() throws IOException {
        byte[] fileBytes = bytes("cv-pdf-content");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/cv.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/cv.pdf", result);

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-013 - uploadFile - Upload ảnh ứng viên thành công")
    void TC_UPLOAD_SER_013() throws IOException {
        byte[] fileBytes = bytes("avatar-content");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/avatar.jpg"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/avatar.jpg", result);

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-014 - uploadFile - Upload file DOCX thành công")
    void TC_UPLOAD_SER_014() throws IOException {
        byte[] fileBytes = bytes("docx-content");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/cv.docx"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/cv.docx", result);

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-015 - uploadFile - Upload file rỗng")
    void TC_UPLOAD_SER_015() throws IOException {
        byte[] fileBytes = new byte[0];

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/empty.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/empty.pdf", result);

        verify(multipartFile, times(1)).getBytes();
        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                anyMap()
        );
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-016 - uploadFile - Lỗi đọc file")
    void TC_UPLOAD_SER_016() throws IOException {
        when(multipartFile.getBytes())
                .thenThrow(new IOException("Cannot read file"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cloudinaryService.uploadFile(multipartFile)
        );

        assertEquals("Không thể upload file", exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Cannot read file", exception.getCause().getMessage());

        verify(multipartFile, times(1)).getBytes();
        verify(cloudinary, never()).uploader();
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-017 - uploadFile - Cloudinary upload lỗi")
    void TC_UPLOAD_SER_017() throws IOException {
        byte[] fileBytes = bytes("cloudinary-error");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap()))
                .thenThrow(new IOException("Cloudinary error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cloudinaryService.uploadFile(multipartFile)
        );

        assertEquals("Không thể upload file", exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Cloudinary error", exception.getCause().getMessage());

        verify(multipartFile, times(1)).getBytes();
        verify(cloudinary, times(1)).uploader();
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-018 - uploadFile - Cloudinary trả thiếu secure_url")
    void TC_UPLOAD_SER_018() throws IOException {
        byte[] fileBytes = bytes("missing-secure-url");

        Map<String, Object> uploadResult = resultMap(
                "public_id", "file_without_secure_url"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertNull(result);

        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-019 - uploadFile - Cloudinary trả secure_url null")
    void TC_UPLOAD_SER_019() throws IOException {
        byte[] fileBytes = bytes("secure-url-null");

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", null);

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertNull(result);

        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-020 - uploadFile - Cloudinary trả secure_url không phải String")
    void TC_UPLOAD_SER_020() throws IOException {
        byte[] fileBytes = bytes("secure-url-wrong-type");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", 12345
        );

        mockUploadSuccess(fileBytes, uploadResult);

        assertThrows(
                ClassCastException.class,
                () -> cloudinaryService.uploadFile(multipartFile)
        );

        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-021 - uploadFile - Cloudinary trả URL http không an toàn")
    void TC_UPLOAD_SER_021() throws IOException {
        byte[] fileBytes = bytes("http-url");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "http://res.cloudinary.com/demo/file.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("http://res.cloudinary.com/demo/file.pdf", result);

        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-022 - uploadFile - File null")
    void TC_UPLOAD_SER_022() {
        assertThrows(
                NullPointerException.class,
                () -> cloudinaryService.uploadFile(null)
        );

        verifyNoInteractions(cloudinary);
        verifyNoInteractions(uploader);
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-023 - uploadFile - Kiểm tra option upload")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void TC_UPLOAD_SER_023() throws IOException {
        byte[] fileBytes = bytes("option-check");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/option.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/option.pdf", result);

        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(uploader, times(1)).upload(
                argThat(argument -> sameBytes(argument, fileBytes)),
                optionsCaptor.capture()
        );

        Map options = optionsCaptor.getValue();

        assertEquals("auto", options.get("resource_type"));
        assertEquals("upload", options.get("type"));
    }

    @Test
    @DisplayName("TC-UPLOAD-SER-024 - uploadFile - Kiểm tra thứ tự tương tác")
    void TC_UPLOAD_SER_024() throws IOException {
        byte[] fileBytes = bytes("in-order-check");

        Map<String, Object> uploadResult = resultMap(
                "secure_url", "https://res.cloudinary.com/demo/in-order.pdf"
        );

        mockUploadSuccess(fileBytes, uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/in-order.pdf", result);

        InOrder inOrder = inOrder(multipartFile, cloudinary, uploader);

        inOrder.verify(multipartFile).getBytes();
        inOrder.verify(cloudinary).uploader();
        inOrder.verify(uploader).upload(any(), anyMap());
    }

    private void mockUploadSuccess(
            byte[] fileBytes,
            Map<String, Object> uploadResult
    ) throws IOException {
        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(uploadResult);
    }

    private static byte[] bytes(String content) {
        return content.getBytes();
    }

    private static boolean sameBytes(Object argument, byte[] expected) {
        if (!(argument instanceof byte[])) {
            return false;
        }

        return Arrays.equals((byte[]) argument, expected);
    }

    private static Map<String, Object> resultMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }

        return map;
    }
}