package com.example.candidate_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;

/**
 * Unit test for CloudinaryService in candidate-service.
 * CheckDB: no database is used; Cloudinary uploader is the external boundary and
 * is mocked. Rollback: no real file upload or persistent state is created.
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
    // Test Case ID: TC-CLS-001 - upload() should return Cloudinary result map.
    @DisplayName("TC-CLS-001 - upload() returns Cloudinary upload result with secure_url")
    void tc_cls_001_upload_whenFileValid_returnsUploadResult() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("public_id", "candidate_cv_001");
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/cv.pdf");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), eq(ObjectUtils.emptyMap()))).thenReturn(uploadResult);

        Map<String, Object> result = cloudinaryService.upload(multipartFile);

        assertSame(uploadResult, result);
        assertEquals("https://res.cloudinary.com/demo/cv.pdf", result.get("secure_url"));
        verify(multipartFile).getBytes();
        verify(cloudinary).uploader();
        verify(uploader).upload(eq(fileBytes), eq(ObjectUtils.emptyMap()));
    }

    @Test
    // Test Case ID: TC-CLS-002 - upload() should propagate MultipartFile read failure.
    @DisplayName("TC-CLS-002 - upload() propagates IOException when file bytes cannot be read")
    void tc_cls_002_upload_whenGetBytesFails_propagatesIOException() throws IOException {
        IOException readError = new IOException("Cannot read file");
        when(multipartFile.getBytes()).thenThrow(readError);
        when(cloudinary.uploader()).thenReturn(uploader);

        IOException exception = assertThrows(IOException.class, () -> cloudinaryService.upload(multipartFile));

        assertSame(readError, exception);
        verify(cloudinary).uploader();
        verify(multipartFile).getBytes();
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    // Test Case ID: TC-CLS-003 - upload() should propagate Cloudinary upload failure.
    @DisplayName("TC-CLS-003 - upload() propagates IOException when Cloudinary upload fails")
    void tc_cls_003_upload_whenCloudinaryFails_propagatesIOException() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        IOException uploadError = new IOException("cloudinary down");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), eq(ObjectUtils.emptyMap()))).thenThrow(uploadError);

        IOException exception = assertThrows(IOException.class, () -> cloudinaryService.upload(multipartFile));

        assertSame(uploadError, exception);
        verify(uploader).upload(eq(fileBytes), eq(ObjectUtils.emptyMap()));
    }

    @Test
    // Test Case ID: TC-CLS-004 - uploadFile() should return secure_url and use safe upload options.
    @DisplayName("TC-CLS-004 - uploadFile() returns secure_url and uses auto upload options")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void tc_cls_004_uploadFile_whenFileValid_returnsSecureUrlAndUsesExpectedOptions() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/cv.pdf");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertEquals("https://res.cloudinary.com/demo/cv.pdf", result);
        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(fileBytes), optionsCaptor.capture());
        assertEquals("auto", optionsCaptor.getValue().get("resource_type"));
        assertEquals("upload", optionsCaptor.getValue().get("type"));
    }

    @Test
    // Test Case ID: TC-CLS-005 - uploadFile() should wrap file read failure.
    @DisplayName("TC-CLS-005 - uploadFile() wraps IOException from file read")
    void tc_cls_005_uploadFile_whenGetBytesFails_wrapsIOException() throws IOException {
        IOException readError = new IOException("Cannot read file");
        when(multipartFile.getBytes()).thenThrow(readError);
        when(cloudinary.uploader()).thenReturn(uploader);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cloudinaryService.uploadFile(multipartFile));

        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
        assertSame(readError, exception.getCause());
        verify(cloudinary).uploader();
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    // Test Case ID: TC-CLS-006 - uploadFile() should wrap Cloudinary upload failure.
    @DisplayName("TC-CLS-006 - uploadFile() wraps IOException from Cloudinary upload")
    void tc_cls_006_uploadFile_whenCloudinaryFails_wrapsIOException() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        IOException uploadError = new IOException("cloudinary down");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), anyMap())).thenThrow(uploadError);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cloudinaryService.uploadFile(multipartFile));

        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
        assertSame(uploadError, exception.getCause());
        verify(uploader).upload(eq(fileBytes), anyMap());
    }

    @Test
    // Test Case ID: TC-CLS-007 - missing secure_url branch.
    @DisplayName("TC-CLS-007 - uploadFile() returns null when Cloudinary result misses secure_url")
    void tc_cls_007_uploadFile_whenSecureUrlMissing_returnsNull() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("public_id", "candidate_cv_001");

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadFile(multipartFile);

        assertNull(result);
        verify(uploader).upload(eq(fileBytes), anyMap());
    }

    @Test
    // Test Case ID: TC-CLS-008 - wrong secure_url type exposes unsafe cast.
    @DisplayName("TC-CLS-008 - uploadFile() throws ClassCastException when secure_url is not String")
    void tc_cls_008_uploadFile_whenSecureUrlWrongType_throwsClassCastException() throws IOException {
        byte[] fileBytes = "cv-content".getBytes();
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", 12345);

        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResult);

        assertThrows(ClassCastException.class, () -> cloudinaryService.uploadFile(multipartFile));
        verify(uploader).upload(eq(fileBytes), anyMap());
    }

    @Test
    // Test Case ID: TC-CLS-009 - null file is an invalid caller contract.
    @DisplayName("TC-CLS-009 - uploadFile() throws NullPointerException when file is null")
    void tc_cls_009_uploadFile_whenFileNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cloudinaryService.uploadFile(null));
        verify(cloudinary).uploader();
        verifyNoInteractions(uploader);
    }
}
