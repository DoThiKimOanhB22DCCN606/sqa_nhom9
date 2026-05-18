package com.example.email_service.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.email_service.model.MailMessage;
import com.example.email_service.repository.MailMessageRepository;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

/**
 * Unit test for GmailInboxService.
 * CheckDB: MailMessageRepository is mocked as the DB boundary; save/duplicate
 * tests verify persisted MailMessage state or verify that save is not called.
 * Rollback: no real Gmail or database is touched. Mockito isolates all test data
 * per test method, so no persisted state remains after execution.
 */
@ExtendWith(MockitoExtension.class)
class GmailInboxServiceTest {

    @Mock
    private MailMessageRepository mailRepo;

    @InjectMocks
    private GmailInboxService gmailInboxService;

    @Mock
    private Message message;

    @Test
    // Test Case ID: TC-GIS-001 - CheckDB save path for a normal Gmail message.
    @DisplayName("TC-GIS-001 - saveEmailFromGmail() saves full email data when message is valid")
    void tc_gis_001_saveEmailFromGmail_whenMessageValid_savesFullMailMessage() throws Exception {
        
        // Arrange: Khởi tạo ngữ cảnh dữ liệu Email hợp lệ (Mocking & Stubbing)
        // Thiết lập các thuộc tính giả lập cho đối tượng Message đầu vào và cấu hình DB chưa tồn tại ID này.
        Date sentDate = Date.from(LocalDateTime.of(2026, 5, 3, 9, 30).atZone(ZoneId.systemDefault()).toInstant());
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<msg-001@gmail>" });
        when(mailRepo.existsByGmailMessageId("<msg-001@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("Interview invitation");
        when(message.getContent()).thenReturn("Plain body");
        when(message.getSentDate()).thenReturn(sentDate);

        // Act: Kích hoạt phương thức xử lý nội bộ (Test Execution)
        // Thực thi hàm private thông qua công cụ ReflectionTestUtils độc lập.
        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);
        
        // Assert: Xác minh tương tác cơ sở dữ liệu và tính toàn vẹn dữ liệu (CheckDB & Data Integrity)
        // 1. Xác thực hệ thống có truy cập DB đọc để đối chiếu kiểm tra trùng lặp ID hay không.
        verify(mailRepo).existsByGmailMessageId("<msg-001@gmail>"); 
        
        // 2. Sử dụng ArgumentCaptor để can thiệp và kiểm thử cấu trúc thực thể MailMessage trước khi lưu xuống DB.
        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        MailMessage savedMail = mailCaptor.getValue();
        assertEquals("sender@example.com", savedMail.getFromEmail());
        assertEquals("receiver@example.com", savedMail.getToEmail());
        assertEquals("Interview invitation", savedMail.getSubject());
        assertEquals("Plain body", savedMail.getContent());
        assertEquals("<msg-001@gmail>", savedMail.getGmailMessageId());
        assertFalse(savedMail.isSent());
        assertTrue(savedMail.getCreatedAt() != null);
    }

    @Test
    // Test Case ID: TC-GIS-002 - duplicate Message-ID branch must not write DB.
    @DisplayName("TC-GIS-002 - saveEmailFromGmail() skips save when Gmail message id already exists")
    void tc_gis_002_saveEmailFromGmail_whenMessageIdExists_skipsSave() throws Exception {
        
        // Arrange: Giả lập kịch bản email trùng lặp mã định danh (Mocking Context)
        // Stubbing cấu hình Repository trả về giá trị 'true' nhằm xác nhận bản ghi đã tồn tại trong DB.
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<duplicate@gmail>" });
        when(mailRepo.existsByGmailMessageId("<duplicate@gmail>")).thenReturn(true);

        // Act: Đẩy dữ liệu email trùng lặp vào hàm thực thi nội bộ (Execution)
        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);
        
        // Assert: Kiểm soát hành vi và bảo vệ biên dữ liệu (CheckDB Isolation)
        // Xác minh bắt buộc hệ thống có truy cập đọc DB đối chiếu, nhưng tuyệt đối không kích hoạt lệnh lưu mới (never()).
        verify(mailRepo).existsByGmailMessageId("<duplicate@gmail>"); 
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-003 - missing Message-ID branch generates fallback id.
    @DisplayName("TC-GIS-003 - saveEmailFromGmail() generates id when Gmail message id is missing")
    void tc_gis_003_saveEmailFromGmail_whenMessageIdMissing_generatesFallbackId() throws Exception {
        
        // Arrange: Cấu hình dữ liệu biên cực đoan - Email bị khuyết thiếu toàn bộ thông tin quan trọng
        // Trạng thái header, tiêu đề, ngày tháng đều nhận giá trị null để kiểm thử nhánh rẽ xử lý biên (Fallback logic).
        when(message.getHeader("Message-ID")).thenReturn(null);
        when(message.getFrom()).thenReturn(null);
        when(message.getRecipients(RecipientType.TO)).thenReturn(null);
        when(message.getSubject()).thenReturn(null);
        when(message.getContent()).thenReturn("No header body");
        when(message.getSentDate()).thenReturn(null);

        // Act: Thực thi xử lý lưu thông tin email khuyết thiếu (Execution)
        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

        // Assert: Kiểm tra cơ chế tự sinh định danh và xác minh DB (CheckDB Logic)
        // 1. Kiểm tra tính đúng đắn của logic: Do ID gốc bị null, hệ thống bỏ qua kiểm tra trùng lặp trên DB (never()).
        verify(mailRepo, never()).existsByGmailMessageId(any(String.class));

        // 2. Bắt thực thể lưu trữ và xác nhận hệ thống tự tạo tiêu đề mặc định cùng ID ngẫu nhiên không rỗng.
        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        MailMessage savedMail = mailCaptor.getValue();
        assertTrue(savedMail.getGmailMessageId() != null && !savedMail.getGmailMessageId().isBlank());
        assertEquals("(No Subject)", savedMail.getSubject());
        assertEquals("No header body", savedMail.getContent());
        assertNull(savedMail.getFromEmail());
        assertNull(savedMail.getToEmail());
        assertTrue(savedMail.getCreatedAt() != null);
    }

    @Test
    // Test Case ID: TC-GIS-004 - private getMessageId true branch.
    @DisplayName("TC-GIS-004 - getMessageId() returns first Message-ID header")
    void tc_gis_004_getMessageId_whenHeaderExists_returnsFirstHeader() throws Exception {
        
        // Arrange: Thiết lập mảng tiêu đề chứa nhiều mã Message-ID phân tách (Dữ liệu thử nghiệm)
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<first>", "<second>" });

        // Act: Kích hoạt hàm nội bộ bóc tách thông tin ID (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        // Assert: Xác thực thuật toán (Data Integrity - No DB Interaction)
        // Đảm bảo hệ thống tuân thủ thiết kế: Luôn ưu tiên trích xuất chính xác phần tử đầu tiên trong chuỗi.
        assertEquals("<first>", result);
    }

    @Test
    // Test Case ID: TC-GIS-005 - private getMessageId false branch.
    @DisplayName("TC-GIS-005 - getMessageId() returns null when header is absent")
    void tc_gis_005_getMessageId_whenHeaderAbsent_returnsNull() throws Exception {
        
        // Arrange: Giả lập mảng tiêu đề rỗng (Edge Case Setup)
        when(message.getHeader("Message-ID")).thenReturn(new String[0]);

        // Act: Kích hoạt phương thức kiểm tra xử lý mảng trống (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        // Assert: Kiểm tra an toàn bộ nhớ (Null Safety Verification)
        // Đảm bảo hàm trả về giá trị null an toàn thay vì phát sinh lỗi tràn chỉ mục mảng (ArrayIndexOutOfBoundsException).
        assertNull(result);
    }

    @Test
    // Test Case ID: TC-GIS-006 - content extraction for plain String content.
    @DisplayName("TC-GIS-006 - extractContent() returns string content directly")
    void tc_gis_006_extractContent_whenContentIsString_returnsContent() throws Exception {
        
        // Arrange: Giả lập nội dung email ở định dạng văn bản thuần đơn giản (Plain Text String)
        when(message.getContent()).thenReturn("Plain text");

        // Act: Gọi phương thức trích xuất nội dung tin nhắn (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        // Assert: Kiểm tra tính nguyên vẹn của dữ liệu văn bản văn bản thô (String Validation)
        assertEquals("Plain text", result);
    }

    @Test
    // Test Case ID: TC-GIS-007 - content extraction for multipart plain and html bodies.
    @DisplayName("TC-GIS-007 - extractContent() concatenates text and html multipart bodies")
    void tc_gis_007_extractContent_whenContentIsMultipart_returnsCombinedText() throws Exception {
        
        // Arrange: Khởi tạo cấu trúc Email phức hợp (MimeMultipart)
        // Thiết lập thủ công một phân đoạn văn bản thô (Plain Text Part) kết hợp một phân đoạn mã HTML (HTML Body Part).
        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText("Plain part");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<b>Html part</b>", "text/html");
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(plainPart);
        multipart.addBodyPart(htmlPart);
        when(message.getContent()).thenReturn(multipart);

        // Act: Tiến hành trích xuất tổng hợp nội dung email phức hợp (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        // Assert: Kiểm tra sự hội tụ dữ liệu (Multi-part Content Merging Validation)
        // Xác minh chuỗi văn bản đầu ra bắt buộc phải chứa trọn vẹn cả nội dung thô lẫn từ khóa trong thẻ HTML.
        assertTrue(result.contains("Plain part"));
        assertTrue(result.contains("Html part"));
    }

    @Test
    // Test Case ID: TC-GIS-008 - unsupported content fail/empty branch.
    @DisplayName("TC-GIS-008 - extractContent() returns empty string for unsupported content type")
    void tc_gis_008_extractContent_whenContentUnsupported_returnsEmptyString() throws Exception {
        
        // Arrange: Thiết lập đối tượng Content không thuộc định dạng được hỗ trợ (Optional.empty())
        when(message.getContent()).thenReturn(Optional.empty());

        // Act: Thực thi phương thức bóc tách dữ liệu lạ (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        // Assert: Cơ chế phòng vệ an toàn hệ thống (Graceful Degradation Validation)
        // Hệ thống buộc phải trả về một chuỗi ký tự rỗng thay vì ném ra lỗi Runtime phá vỡ luồng chạy.
        assertEquals("", result);
    }

    @Test
    // Test Case ID: TC-GIS-009 - fail path: malformed message is not persisted.
    @DisplayName("TC-GIS-009 - saveEmailFromGmail() throws and does not save when message content fails")
    void tc_gis_009_saveEmailFromGmail_whenMessageContentFails_throwsAndDoesNotSave() throws Exception {
        
        // Arrange: Cấu hình kịch bản email bị lỗi kỹ thuật nghiêm trọng tại luồng nội dung (Error Simulation)
        // Thiết lập thuộc tính cơ bản hợp lệ nhưng hàm kích hoạt lấy Content cố ý ném ra một ngoại lệ RuntimeException.
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<bad-content@gmail>" });
        when(mailRepo.existsByGmailMessageId("<bad-content@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("Broken content");
        when(message.getContent()).thenThrow(new RuntimeException("cannot read content"));

        // Act & Assert: Đánh giá khả năng xử lý ngoại lệ và an toàn dữ liệu (Exception Handling & CheckDB)
        // 1. Kiểm tra xác nhận phương thức ném trả đúng loại ngoại lệ kỹ thuật ra ngoài.
        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message));
        
        // 2. Xác minh nghiêm ngặt: Hệ thống có đọc DB check trùng nhưng tuyệt đối KHÔNG được gọi hàm save() để lưu dữ liệu lỗi.
        verify(mailRepo).existsByGmailMessageId("<bad-content@gmail>");
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-010 - empty from array branch should leave fromEmail null.
    @DisplayName("TC-GIS-010 - saveEmailFromGmail() leaves fromEmail null when from array is empty")
    void tc_gis_010_saveEmailFromGmail_whenFromArrayEmpty_savesNullFromEmail() throws Exception {
        
        // Arrange: Thiết lập dữ liệu biên - Email có danh sách mảng người gửi trống rỗng (From Array Empty)
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<empty-from@gmail>" });
        when(mailRepo.existsByGmailMessageId("<empty-from@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[0]);
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("No from");
        when(message.getContent()).thenReturn("Body");
        when(message.getSentDate()).thenReturn(null);

        // Act: Thực thi xử lý lưu trữ bản ghi có mảng From trống (Execution)
        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);
        
        // Assert: Xác minh dữ liệu biên lưu trữ trong DB (CheckDB Fields)
        // Xác thực hệ thống có truy cập DB check trùng, lưu thành công và trường thông tin từ người gửi buộc phải nhận giá trị null.
        verify(mailRepo).existsByGmailMessageId("<empty-from@gmail>");
        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertNull(mailCaptor.getValue().getFromEmail());
    }

    @Test
    // Test Case ID: TC-GIS-011 - empty to array branch should leave toEmail null.
    @DisplayName("TC-GIS-011 - saveEmailFromGmail() leaves toEmail null when recipient array is empty")
    void tc_gis_011_saveEmailFromGmail_whenRecipientArrayEmpty_savesNullToEmail() throws Exception {
        
        // Arrange: Thiết lập dữ liệu biên - Email có danh sách mảng người nhận trống rỗng (Recipient TO Empty)
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<empty-to@gmail>" });
        when(mailRepo.existsByGmailMessageId("<empty-to@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO)).thenReturn(new InternetAddress[0]);
        when(message.getSubject()).thenReturn("No to");
        when(message.getContent()).thenReturn("Body");
        when(message.getSentDate()).thenReturn(null);

        // Act: Thực thi quy trình lưu trữ bản ghi có mảng To trống (Execution)
        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);
        
        // Assert: Xác minh tính chính xác dữ liệu biên lưu trữ trong DB (CheckDB Fields)
        // Xác thực hệ thống thực thi check trùng trên DB, lưu thành công thực thể và trường thông tin người nhận lưu trữ là null.
        verify(mailRepo).existsByGmailMessageId("<empty-to@gmail>");
        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertNull(mailCaptor.getValue().getToEmail());
    }

    @Test
    // Test Case ID: TC-GIS-012 - explicit html multipart branch.
    @DisplayName("TC-GIS-012 - extractContent() appends html body part")
    void tc_gis_012_extractContent_whenMultipartContainsHtmlOnly_returnsHtmlContent() throws Exception {
        
        // Arrange: Thiết lập cấu trúc Email đa phần có tổ chức Mock chuyên sâu (Deep Mocking Structure)
        // Giả lập đối tượng Multipart chỉ chứa duy nhất một BodyPart định dạng đặc thù là "text/html".
        MimeMultipart multipart = mock(MimeMultipart.class);
        BodyPart htmlPart = mock(BodyPart.class);
        when(message.getContent()).thenReturn(multipart);
        when(multipart.getCount()).thenReturn(1);
        when(multipart.getBodyPart(0)).thenReturn(htmlPart);
        when(htmlPart.isMimeType("text/plain")).thenReturn(false);
        when(htmlPart.isMimeType("text/html")).thenReturn(true);
        when(htmlPart.getContent()).thenReturn("<p>HTML only</p>");

        // Act: Gọi phương thức trích xuất xử lý văn bản HTML (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        // Assert: Xác minh dữ liệu chuỗi HTML thô trích xuất thành công (HTML Extraction Verification)
        assertEquals("<p>HTML only</p>", result);
    }

    @Test
    // Test Case ID: TC-GIS-013 - fetch loop saves valid messages and continues after one message fails.
    @DisplayName("TC-GIS-013 - fetchAndSaveEmails() saves valid Gmail messages and skips failed message")
    void tc_gis_013_fetchAndSaveEmails_whenOneMessageFails_continuesAndClosesResources() throws Exception {
        
        // Arrange: Giả lập môi trường kiểm thử tích hợp ở tầng dịch vụ (Integration-Level Setup)
        // 1. Tạo lập các cấu trúc Mock cho Session, Store, Folder và danh sách 2 tin nhắn (1 tin lỗi content, 1 tin hợp lệ).
        Session session = mock(Session.class);
        Store store = mock(Store.class);
        Folder inbox = mock(Folder.class);
        Message validMessage = mock(Message.class);
        Message brokenMessage = mock(Message.class);

        // 2. Sử dụng Reflection để tiêm các thông số tài khoản bảo mật nội bộ vào Service.
        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        // 3. Cấu hình Stubbing luồng tương tác mạng hòm thư Gmail và hành vi chi tiết cho từng Message.
        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.getMessages()).thenReturn(new Message[] { validMessage, brokenMessage });

        when(brokenMessage.getHeader("Message-ID")).thenReturn(new String[] { "<broken@gmail>" });
        when(mailRepo.existsByGmailMessageId("<broken@gmail>")).thenReturn(false);
        when(brokenMessage.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("bad@example.com") });
        when(brokenMessage.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(brokenMessage.getSubject()).thenReturn("Broken message");
        when(brokenMessage.getContent())
                .thenThrow(new RuntimeException("cannot read", new IllegalStateException("raw cause")));
        when(brokenMessage.getMessageNumber()).thenReturn(2);

        when(validMessage.getHeader("Message-ID")).thenReturn(new String[] { "<valid@gmail>" });
        when(mailRepo.existsByGmailMessageId("<valid@gmail>")).thenReturn(false);
        when(validMessage.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("ok@example.com") });
        when(validMessage.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(validMessage.getSubject()).thenReturn("Valid message");
        when(validMessage.getContent()).thenReturn("Body");
        when(validMessage.getSentDate()).thenReturn(null);

        // Act: Kích hoạt toàn bộ luồng tải và đồng bộ hóa thư trong môi trường Mock tĩnh Session (Execution)
        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        // Assert: Xác minh nguyên tắc xử lý liên tục, CheckDB và đóng tài nguyên an toàn (Fault Tolerance Validation)
        // 1. Kiểm tra nghiêm ngặt: Hệ thống buộc phải thực hiện truy cập đọc DB check trùng cho CẢ HAI thư.
        verify(mailRepo).existsByGmailMessageId("<broken@gmail>");
        verify(mailRepo).existsByGmailMessageId("<valid@gmail>");  

        // 2. Xác thực việc ghi dữ liệu: Đảm bảo luồng xử lý lỗi tự cô lập, chỉ lưu trữ duy nhất tin nhắn hợp lệ xuống DB.
        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertEquals("<valid@gmail>", mailCaptor.getValue().getGmailMessageId());
        
        // 3. Đảm bảo an toàn hệ thống: Các tài nguyên kết nối mạng (Inbox, Store) bắt buộc phải đóng lại để tránh rò rỉ bộ nhớ.
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-014 - outer catch branch must not crash or write DB when Gmail connection fails.
    @DisplayName("TC-GIS-014 - fetchAndSaveEmails() handles Gmail connection failure without saving")
    void tc_gis_014_fetchAndSaveEmails_whenStoreCannotOpen_doesNotThrowAndDoesNotSave() throws Exception {
        
        // Arrange: Mô phỏng sự cố sập kết nối mạng ngoại vi (Network Error Simulation)
        // Cấu hình hàm lấy Store từ Session ném lỗi ngay từ bước đầu khởi động kết nối mạng.
        Session session = mock(Session.class);
        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenThrow(new RuntimeException("imap unavailable"));

        // Act: Kích hoạt quét hòm thư trong tình trạng mất kết nối (Execution)
        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        // Assert: Kiểm soát cơ chế nuốt lỗi an toàn và CheckDB (Graceful Catch Verification)
        // Chứng minh ngoại lệ được bao bọc an toàn tuyệt đối không làm sập hệ thống và DB không bị tác động ghi nhiễu.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-015 - fetch loop must skip duplicate Gmail messages and close resources.
    @DisplayName("TC-GIS-015 - fetchAndSaveEmails() skips duplicate Gmail message without saving")
    void tc_gis_015_fetchAndSaveEmails_whenMessageIdAlreadyExists_skipsSaveAndClosesResources() throws Exception {
        
        // Arrange: Mô phỏng tiến trình đồng bộ gặp thư trùng lặp trong hòm thư thực tế (Duplicate Sync Setup)
        Session session = mock(Session.class);
        Store store = mock(Store.class);
        Folder inbox = mock(Folder.class);
        Message duplicateMessage = mock(Message.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.getMessages()).thenReturn(new Message[] { duplicateMessage });
        when(duplicateMessage.getHeader("Message-ID")).thenReturn(new String[] { "<duplicate@gmail>" });
        when(mailRepo.existsByGmailMessageId("<duplicate@gmail>")).thenReturn(true);

        // Act: Thực thi quy trình đồng bộ tự động lặp thư (Execution)
        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        // Assert: Xác minh hành vi đọc và ngăn chặn ghi dữ liệu (CheckDB Duplication Path)
        // Xác nhận luồng chạy có tra cứu đối chiếu DB, phát hiện trùng, hủy lệnh lưu mới và thực hiện giải phóng tài nguyên mạng.
        verify(mailRepo).existsByGmailMessageId("<duplicate@gmail>");
        verify(mailRepo, never()).save(any(MailMessage.class));
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-016 - outer catch must handle authentication/connect failure.
    @DisplayName("TC-GIS-016 - fetchAndSaveEmails() handles Gmail connect failure without saving")
    void tc_gis_016_fetchAndSaveEmails_whenStoreConnectFails_doesNotThrowAndDoesNotSave() throws Exception {
        
        // Arrange: Mô phỏng lỗi bảo mật - Thất bại khi xác thực tài khoản IMAP (Authentication Failure)
        // Cấu hình lệnh store.connect() ném ngoại lệ RuntimeException mô phỏng sai mật khẩu ứng dụng.
        Session session = mock(Session.class);
        Store store = mock(Store.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenReturn(store);
        doThrow(new RuntimeException("auth fail"))
                .when(store).connect("imap.gmail.com", "tester@gmail.com", "app-password");

        // Act: Kích hoạt chạy tiến trình đồng bộ hòm thư (Execution)
        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        // Assert: Kiểm soát lỗi an toàn hệ thống nâng cao (Security Exception Handling & CheckDB)
        // Ngoại lệ xác thực phải được bọc bắt gọn gàng, không gây gián đoạn ứng dụng chính và không tác động ghi xuống DB.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-017 - empty Gmail inbox should close resources and not save.
    @DisplayName("TC-GIS-017 - fetchAndSaveEmails() closes resources when Gmail inbox is empty")
    void tc_gis_017_fetchAndSaveEmails_whenInboxIsEmpty_shouldCloseResourcesAndNotSave() throws Exception {
        
        // Arrange: Thiết lập kịch bản biên - Hòm thư đến INBOX hoàn toàn rỗng (Empty Inbox Case)
        // Hàm inbox.getMessages() trả về một mảng rỗng không chứa thực thể tin nhắn nào.
        Session session = mock(Session.class);
        Store store = mock(Store.class);
        Folder inbox = mock(Folder.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.getMessages()).thenReturn(new Message[0]);

        // Act: Kích hoạt quét hòm thư rỗng (Execution)
        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        // Assert: Xác minh tối ưu hóa hiệu năng và quản lý kết nối (Resource Lifecycle Validation)
        // Đảm bảo không có hành vi ghi DB vô nghĩa nào xảy ra, đồng thời toàn bộ cổng kết nối mạng vẫn phải được thu hồi đúng quy chuẩn.
        verify(mailRepo, never()).save(any(MailMessage.class));
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-018 - unsupported multipart body part should be ignored.
    @DisplayName("TC-GIS-018 - extractContent() ignores unsupported multipart body parts")
    void tc_gis_018_extractContent_whenMultipartPartIsUnsupported_shouldReturnEmptyString() throws Exception {
        
        // Arrange: Thiết lập cấu trúc Email chứa dữ liệu lạ đính kèm không thuộc nhóm văn bản (E.g., File đính kèm)
        // Cấu hình các hàm định vị MimeType trả về kết quả 'false' cho cả định dạng văn bản thô lẫn HTML.
        MimeMultipart multipart = mock(MimeMultipart.class);
        BodyPart attachmentPart = mock(BodyPart.class);
        when(message.getContent()).thenReturn(multipart);
        when(multipart.getCount()).thenReturn(1);
        when(multipart.getBodyPart(0)).thenReturn(attachmentPart);
        when(attachmentPart.isMimeType("text/plain")).thenReturn(false);
        when(attachmentPart.isMimeType("text/html")).thenReturn(false);

        // Act: Thực thi bóc tách thông tin phân đoạn không hỗ trợ (Execution)
        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        // Assert: Xác thực cơ chế lọc dữ liệu (Content Filter Validation)
        // Hệ thống bắt buộc phải bỏ qua dữ liệu lạ này một cách an toàn và trả về một chuỗi ký tự trống.
        assertEquals("", result);
    }

}
