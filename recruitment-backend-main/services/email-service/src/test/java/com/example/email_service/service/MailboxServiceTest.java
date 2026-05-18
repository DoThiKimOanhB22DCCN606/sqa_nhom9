package com.example.email_service.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.email_service.dto.PaginationDTO;
import com.example.email_service.model.MailMessage;
import com.example.email_service.repository.MailMessageRepository;

/**
 * Unit test for MailboxService.
 * CheckDB: MailMessageRepository is mocked as the database boundary; write tests
 * assert saved/deleted object state and repository calls.
 * Rollback: no real database is touched, so Mockito creates isolated mock state
 * for every test method.
 */
@ExtendWith(MockitoExtension.class)
class MailboxServiceTest {

    @Mock
    private MailMessageRepository mailRepo;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailboxService mailboxService;

    private MailMessage createMail(Long id, boolean sent, boolean read, boolean deleted, String subject, String content) {
        MailMessage mail = new MailMessage();
        mail.setId(id);
        mail.setFromEmail("noreply@example.com");
        mail.setToEmail("user@example.com");
        mail.setSent(sent);
        mail.setRead(read);
        mail.setDeleted(deleted);
        mail.setSubject(subject);
        mail.setContent(content);
        return mail;
    }

    @Test

    // Test Case ID: TC-MS-001 - sendGmail() should send email and save mail record

    @DisplayName("TC-MS-001 - sendGmail() should send email and save mail record")
    void tc_ms_001_sendGmail_whenInputIsValid_shouldSendAndSaveMessage() {
        // Arrange: Thiết lập giả lập cấu hình và hành vi của tầng Database (Mocking)
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        when(mailRepo.save(any(MailMessage.class))).thenAnswer(invocation -> {
            MailMessage persisted = invocation.getArgument(0);
            persisted.setId(1L);
            return persisted;
        });

        // Act: Thực thi phương thức gửi Gmail (Test Execution)
        MailMessage result = mailboxService.sendGmail("recipient@example.com", "Subject", "Hello content");

        // Assert: Xác minh tính toàn vẹn của dữ liệu thực thể trả về (Data Integrity)
        assertEquals(1L, result.getId());
        assertEquals("noreply@example.com", result.getFromEmail());
        assertEquals("recipient@example.com", result.getToEmail());
        assertEquals("Subject", result.getSubject());
        assertEquals("Hello content", result.getContent());
        assertTrue(result.isSent());
        assertTrue(result.getGmailMessageId() != null && !result.getGmailMessageId().isBlank());

        // Assert: Xác minh tương tác của MailSender độc lập bên ngoài (Behavior Verification)
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        assertEquals("recipient@example.com", mailCaptor.getValue().getTo()[0]);

        // CheckDB: Xác minh cơ sở dữ liệu nhận đúng thực thể dữ liệu yêu cầu lưu trữ.
        // Sử dụng ArgumentCaptor can thiệp sâu vào tham số truyền xuống hàm save() của Repository.
        ArgumentCaptor<MailMessage> savedMailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo, times(1)).save(savedMailCaptor.capture());
        MailMessage persistedMail = savedMailCaptor.getValue();
        assertEquals("recipient@example.com", persistedMail.getToEmail());
        assertEquals("Subject", persistedMail.getSubject());
        assertTrue(persistedMail.isSent());
    }

    @Test

    // Test Case ID: TC-MS-002 - deleteMail() should soft delete mail when exists

    @DisplayName("TC-MS-002 - deleteMail() should soft delete mail when exists")
    void tc_ms_002_deleteMail_whenMailExists_shouldMarkDeletedAndSave() {
        // Arrange: Khởi tạo dữ liệu mẫu và cấu hình hành vi tìm kiếm bản ghi hiện hữu
        MailMessage mail = createMail(2L, false, false, false, "Hi", "Body");
        when(mailRepo.findById(2L)).thenReturn(Optional.of(mail));

        // Act: Thực thi nghiệp vụ xóa mềm (Soft Delete)
        mailboxService.deleteMail(2L);

        // Assert Core Logic: Xác nhận cờ trạng thái xóa đã được chuyển đổi logic thành công
        assertTrue(mail.isDeleted());
        
        // CheckDB: Xác minh cơ sở dữ liệu thực hiện cập nhật lại trạng thái mới của bản ghi.
        verify(mailRepo, times(1)).save(mail);
    }

    @Test

    // Test Case ID: TC-MS-003 - deleteMail() should do nothing when mail does not exist

    @DisplayName("TC-MS-003 - deleteMail() should do nothing when mail does not exist")
    void tc_ms_003_deleteMail_whenMailMissing_shouldNotSave() {
        // Arrange: Giả lập kịch bản truy vấn không tìm thấy dữ liệu trong Database
        when(mailRepo.findById(3L)).thenReturn(Optional.empty());

        // Act: Gọi hàm thực thi nghiệp vụ xóa mềm trên id không tồn tại
        mailboxService.deleteMail(3L);

        // CheckDB: Đảm bảo cơ sở dữ liệu không bị thay đổi hoặc thao tác lưu trữ ngoài ý muốn.
        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TC-MS-004 - permanentDelete() should delete mail when exists

    @DisplayName("TC-MS-004 - permanentDelete() should delete mail when exists")
    void tc_ms_004_permanentDelete_whenMailExists_shouldDeleteRecord() {
        // Arrange: Thiết lập thực thể tồn tại sẵn sàng để xóa vĩnh viễn khỏi hệ thống
        MailMessage mail = createMail(4L, true, false, false, "Subject", "Body");
        when(mailRepo.findById(4L)).thenReturn(Optional.of(mail));

        // Act: Thực thi xóa cứng dữ liệu (Hard Delete Execution)
        mailboxService.permanentDelete(4L);

        // CheckDB: Xác minh lệnh xóa bản ghi vật lý đã gửi xuống Database đúng tần suất quy định.
        verify(mailRepo, times(1)).delete(mail);
    }

    @Test

    // Test Case ID: TC-MS-005 - getMailById() should return mail when present

    @DisplayName("TC-MS-005 - getMailById() should return mail when present")
    void tc_ms_005_getMailById_whenMailExists_shouldReturnMail() {
        // Arrange: Chuẩn bị dữ liệu mục tiêu để phục vụ kiểm tra truy vấn chính xác
        MailMessage mail = createMail(5L, false, false, false, "Hello", "World");
        when(mailRepo.findById(5L)).thenReturn(Optional.of(mail));

        // Act: Truy xuất thông tin thư điện tử qua mã ID định danh
        MailMessage result = mailboxService.getMailById(5L);

        // Assert: Đảm bảo dữ liệu truy xuất trùng khớp thông tin đã cấu hình trong DB boundary
        assertEquals(5L, result.getId());
        assertEquals("Hello", result.getSubject());
        
        // CheckDB: Xác nhận hàm tìm kiếm của Repository đã được kích hoạt đúng tham số ID.
        verify(mailRepo).findById(5L);
    }

    @Test

    // Test Case ID: TC-MS-006 - getMailById() should throw when mail missing

    @DisplayName("TC-MS-006 - getMailById() should throw when mail missing")
    void tc_ms_006_getMailById_whenMailMissing_shouldThrowRuntimeException() {
        // Arrange: Giả lập môi trường Database hoàn toàn trống đối với bản ghi ID chỉ định
        when(mailRepo.findById(6L)).thenReturn(Optional.empty());

        // Act & Assert: Kích hoạt hành vi và đón đầu ngoại lệ nghiệp vụ (Exception Testing)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> mailboxService.getMailById(6L));
        
        // Assert Core Logic: Xác minh thông tin chi tiết lỗi không được để trống nhằm hỗ trợ truy vết lỗi
        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
        
        // CheckDB: Đo lường và xác nhận luồng nghiệp vụ có thực thi truy vấn tìm kiếm trước khi báo lỗi.
        verify(mailRepo).findById(6L);
    }

    @Test

    // Test Case ID: TC-MS-007 - getAllEmailsWithFilters() should filter inbox, read and keyword correctly

    @DisplayName("TC-MS-007 - getAllEmailsWithFilters() should filter inbox, read and keyword correctly")
    void tc_ms_007_getAllEmailsWithFilters_whenInboxReadAndKeyword_shouldReturnFilteredPagination() {
        // Arrange: Tạo tập dữ liệu đa dạng để kiểm nghiệm khả năng phân tách bộ lọc (In-memory dataset mockup)
        MailMessage mail1 = createMail(7L, false, false, false, "Hello", "Meeting today");
        MailMessage mail2 = createMail(8L, false, true, false, "Hello", "Meeting today");
        MailMessage mail3 = createMail(9L, true, false, false, "Hello", "Meeting today");
        MailMessage mail4 = createMail(10L, false, false, false, "Update", "Hello world");

        Pageable pageable = PageRequest.of(0, 10);
        Page<MailMessage> allPage = new PageImpl<>(List.of(mail1, mail2, mail3, mail4), pageable, 4);
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(allPage);

        // Act: Thực hiện lọc nâng cao với điều kiện: folder="inbox", read=false (unread), keyword="Hello"
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("inbox", false, "Hello", "createdAt", "desc", 1, 10);

        // Assert: Xác minh dữ liệu đầu ra sau lọc chỉ giữ lại 2 thực thể hợp lệ (mail1 và mail4)
        assertEquals(2, ((List<?>) result.getResult()).size());
        assertEquals(2, result.getMeta().getTotal());
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
        
        // CheckDB: Xác minh Service ủy thác truy vấn phân trang thông qua Repository gốc.
        verify(mailRepo).findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test

    // Test Case ID: TC-MS-008 - getUnreadCount() should return unread count from repository

    @DisplayName("TC-MS-008 - getUnreadCount() should return unread count from repository")
    void tc_ms_008_getUnreadCount_shouldReturnCountFromRepository() {
        // Arrange: Giả lập kết quả đếm tổng số thư chưa đọc từ kho lưu trữ bằng hằng số 7L
        when(mailRepo.countBySentFalseAndReadFalseAndDeletedFalse()).thenReturn(7L);

        // Act: Lấy tổng số lượng thư chưa đọc
        Long result = mailboxService.getUnreadCount();

        // Assert: Kiểm tra tính toàn vẹn số liệu thống kê trả về cho Client
        assertEquals(7L, result);
        
        // CheckDB: Đảm bảo trigger đúng phương thức đếm tối ưu hóa của Repository hạ tầng.
        verify(mailRepo, times(1)).countBySentFalseAndReadFalseAndDeletedFalse();
    }

    @Test

    // Test Case ID: TC-MS-009 - permanentDelete() should do nothing when mail does not exist

    @DisplayName("TC-MS-009 - permanentDelete() should do nothing when mail does not exist")
    void tc_ms_009_permanentDelete_whenMailMissing_shouldNotDelete() {
        // Arrange: Giả lập DB không chứa thực thể có ID 209L khi tiến hành xóa cứng
        when(mailRepo.findById(209L)).thenReturn(Optional.empty());

        // Act: Tiến hành gọi chức năng xóa vĩnh viễn bản ghi vắng mặt
        mailboxService.permanentDelete(209L);

        // CheckDB: Ngăn chặn tuyệt đối hành vi ra lệnh xóa lỗi gửi xuống cơ sở dữ liệu hạ tầng.
        verify(mailRepo, never()).delete(any());
    }

    @Test

    // Test Case ID: TC-MS-010 - getInbox() should normalize invalid pagination and return inbox page

    @DisplayName("TC-MS-010 - getInbox() should normalize invalid pagination and return inbox page")
    void tc_ms_010_getInbox_whenInvalidPagination_shouldNormalizeAndReturnInbox() {
        // Arrange: Cấu hình dữ liệu phân trang chuẩn bị sẵn cho kịch bản biên giá trị lỗi
        MailMessage mail = createMail(210L, false, false, false, "Inbox", "Body");
        when(mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 1), 1));

        // Act: Truyền cố ý cặp tham số vô lý page=0, size=0 nhằm kiểm thử khả năng phòng vệ hệ thống (Boundary Testing)
        PaginationDTO result = mailboxService.getInbox(0, 0);

        // Assert Core Logic: Chứng minh hệ thống tự động chuẩn hóa dữ liệu đầu vào (Normalize) về tối thiểu trang 1 kích thước 1
        assertEquals(1, result.getMeta().getPage());
        assertEquals(1, result.getMeta().getPageSize());
        assertEquals(1, ((List<?>) result.getResult()).size());
    }

    @Test

    // Test Case ID: TC-MS-011 - getInboxAll() should use inbox repository and cap limit at 100

    @DisplayName("TC-MS-011 - getInboxAll() should use inbox repository and cap limit at 100")
    void tc_ms_011_getInboxAll_whenLimitTooLarge_shouldCapAt100() {
        // Arrange: Giả lập cơ chế phân trang phía DB trả về cấu hình kích thước tối đa quy định
        when(mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        // Act: Đưa giá trị kích thước yêu cầu vượt trần cực lớn (limit=999)
        PaginationDTO result = mailboxService.getInboxAll(1, 999);

        // Assert Core Logic: Xác thực tính năng giới hạn trần (Upper Cap Limit) hoạt động để bảo vệ tài nguyên hệ thống
        assertEquals(100, result.getMeta().getPageSize());
        
        // CheckDB: Đảm bảo luồng đi đúng phân hệ dữ liệu Inbox của Repository.
        verify(mailRepo).findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test

    // Test Case ID: TC-MS-012 - getSent() should return sent emails page

    @DisplayName("TC-MS-012 - getSent() should return sent emails page")
    void tc_ms_012_getSent_shouldReturnSentEmails() {
        // Arrange: Khởi tạo dữ liệu hộp thư đi (sent=true)
        MailMessage sent = createMail(212L, true, true, false, "Sent", "Body");
        when(mailRepo.findByDeletedFalseAndSentTrueOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sent), PageRequest.of(0, 10), 1));

        // Act: Lấy danh sách thư đã gửi phân trang
        PaginationDTO result = mailboxService.getSent(1, 10);

        // Assert: Xác thực kết quả đầu ra chứa chính xác thực thể thư đi mong muốn và đúng định danh vị trí tham chiếu
        assertEquals(1, result.getMeta().getTotal());
        assertSame(sent, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TC-MS-013 - implementation guard: getDeleted() currently delegates to the active-mail repository.

    @DisplayName("TC-MS-013 - getDeleted() documents current active-mail repository delegation")
    void tc_ms_013_getDeleted_documentsCurrentActiveMailRepositoryDelegation() {
        // Arrange: Khởi tạo bản ghi phục vụ kịch bản kiểm thử việc ủy thác hàm truy xuất
        MailMessage mail = createMail(213L, false, false, false, "Deleted", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 10), 1));

        // Act: Gọi hàm xem thư mục rác theo thiết kế hiện tại của Service
        PaginationDTO result = mailboxService.getDeleted(1, 10);

        // Assert & CheckDB: Ghi nhận và khóa hành vi hiện tại (Implementation Guard) của hàm - đang trỏ nhầm sang Repo thư hoạt động
        assertEquals(1, result.getMeta().getTotal());
        verify(mailRepo).findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test

    // Test Case ID: TC-MS-014 - markRead() should set read true and save when mail exists

    @DisplayName("TC-MS-014 - markRead() should set read true and save when mail exists")
    void tc_ms_014_markRead_whenMailExists_shouldPersistReadState() {
        // Arrange: Tạo thư mới ở trạng thái chưa đọc (read=false)
        MailMessage mail = createMail(214L, false, false, false, "Unread", "Body");
        when(mailRepo.findById(214L)).thenReturn(Optional.of(mail));

        // Act: Thực thi thay đổi trạng thái chuyển sang "Đã đọc"
        mailboxService.markRead(214L);

        // Assert Core Logic: Xác thực cờ trạng thái đọc chuyển dịch thành công
        assertTrue(mail.isRead());
        
        // CheckDB: Đảm bảo cập nhật trạng thái mới này xuống DB hạ tầng để đồng bộ hóa dữ liệu.
        verify(mailRepo).save(mail);
    }

    @Test

    // Test Case ID: TC-MS-015 - markRead() should do nothing when mail is missing

    @DisplayName("TC-MS-015 - markRead() should do nothing when mail is missing")
    void tc_ms_015_markRead_whenMailMissing_shouldNotSave() {
        // Arrange: Giả lập môi trường rỗng không tồn tại bản ghi ID 215L
        when(mailRepo.findById(215L)).thenReturn(Optional.empty());

        // Act: Kích hoạt tác vụ đánh dấu đọc trên id ảo
        mailboxService.markRead(215L);

        // CheckDB: Đảm bảo an toàn dữ liệu, tuyệt đối không gọi hàm cập nhật save() khi không có thực thể thay đổi.
        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TC-MS-016 - restoreMail() should set deleted false and save when mail exists

    @DisplayName("TC-MS-016 - restoreMail() should set deleted false and save when mail exists")
    void tc_ms_016_restoreMail_whenMailExists_shouldPersistDeletedFalse() {
        // Arrange: Thiết lập trạng thái ban đầu của thư mục nằm trong thùng rác (deleted=true)
        MailMessage mail = createMail(216L, false, false, true, "Deleted", "Body");
        when(mailRepo.findById(216L)).thenReturn(Optional.of(mail));

        // Act: Thực thi nghiệp vụ khôi phục thư (Restore Mail Execution)
        mailboxService.restoreMail(216L);

        // Assert Core Logic: Khẳng định cờ trạng thái xóa mềm đã được gỡ bỏ logic thành công
        assertFalse(mail.isDeleted());
        
        // CheckDB: Xác minh DB lưu trữ bản ghi trạng thái khôi phục mới sạch sẽ.
        verify(mailRepo).save(mail);
    }

    @Test

    // Test Case ID: TC-MS-017 - restoreMail() should do nothing when mail is missing

    @DisplayName("TC-MS-017 - restoreMail() should do nothing when mail is missing")
    void tc_ms_017_restoreMail_whenMailMissing_shouldNotSave() {
        // Arrange: Cấu hình kịch bản truy vấn DB trả về empty khi tìm kiếm để khôi phục
        when(mailRepo.findById(217L)).thenReturn(Optional.empty());

        // Act: Gọi hàm khôi phục trên mã Id không có thật
        mailboxService.restoreMail(217L);

        // CheckDB: Xác nhận bảo vệ DB, không thực hiện bất kì lệnh cập nhật dư thừa nào.
        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TC-MS-018 - getAllEmailsWithFilters() should default folder sort and pagination when inputs are blank

    @DisplayName("TC-MS-018 - getAllEmailsWithFilters() should default folder sort and pagination when inputs are blank")
    void tc_ms_018_getAllEmailsWithFilters_whenInputsBlank_shouldUseDefaults() {
        // Arrange: Chuẩn bị dữ liệu trả về cho trường hợp áp dụng tham số mặc định cấu hình nền
        MailMessage mail = createMail(218L, false, false, false, "Default", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 1), 1));

        // Act: Gửi loạt giá trị trống rỗng và âm phân trang (" ", null, -1, 0) vào bộ lọc phức hợp
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("", null, "", "", "", -1, 0);

        // Assert: Chứng minh cơ chế Fallback thiết lập dữ liệu mặc định hoạt động chuẩn xác (Page=1, Size=1, Total=1)
        assertEquals(1, result.getMeta().getPage());
        assertEquals(1, result.getMeta().getPageSize());
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test

    // Test Case ID: TC-MS-019 - getAllEmailsWithFilters() should filter sent folder

    @DisplayName("TC-MS-019 - getAllEmailsWithFilters() should filter sent folder")
    void tc_ms_019_getAllEmailsWithFilters_whenSentFolder_shouldReturnOnlySentMessages() {
        // Arrange: Tạo tập bản ghi trộn lẫn bao gồm cả thư đến (inbox) và thư đi (sent)
        MailMessage inbox = createMail(219L, false, false, false, "Inbox", "Body");
        MailMessage sent = createMail(220L, true, false, false, "Sent", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inbox, sent), PageRequest.of(0, 10), 2));

        // Act: Chỉ định lọc riêng phân mục thư đã gửi (folder="sent")
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("sent", null, null, "createdAt", "asc", 1, 10);

        // Assert Core Logic: Đảm bảo kết quả đầu ra đã lược bỏ phần inbox, chỉ trả ra duy nhất bản ghi có thuộc tính sent=true
        assertEquals(1, result.getMeta().getTotal());
        assertSame(sent, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TC-MS-020 - getAllEmailsWithFilters() should return empty deleted folder because base query excludes deleted

    @DisplayName("TC-MS-020 - getAllEmailsWithFilters() should return empty deleted folder because base query excludes deleted")
    void tc_ms_020_getAllEmailsWithFilters_whenDeletedFolder_shouldReturnEmptyDueToBaseQuery() {
        // Arrange: Chuẩn bị thực thể hoạt động bình thường nằm trong tập kết quả thô của Base Query
        MailMessage active = createMail(220L, false, false, false, "Active", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 10), 1));

        // Act: Truy cập folder rác ("deleted") thông qua hàm lọc chung
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("deleted", null, null, "createdAt", "desc", 1, 10);

        // Assert Core Logic: Xác minh danh sách trống do thiết kế Base Query hiện tại chặn loại trừ cứng các bản ghi xóa mềm
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());
    }

    @Test

    // Test Case ID: TC-MS-021 - getAllEmailsWithFilters() should match keyword in content when subject does not match

    @DisplayName("TC-MS-021 - getAllEmailsWithFilters() should match keyword in content when subject does not match")
    void tc_ms_021_getAllEmailsWithFilters_whenKeywordMatchesContent_shouldReturnMessage() {
        // Arrange: Thiết lập mail có tiêu đề lệch pha hoàn toàn nhưng nội dung chứa từ khóa tìm kiếm mục tiêu ("interview")
        MailMessage mail = createMail(221L, false, false, false, "No match", "Important interview details");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 10), 1));

        // Act: Tìm kiếm toàn bộ với từ khóa keyword="interview"
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("all", null, "interview", "createdAt", "desc", 1, 10);

        // Assert Core Logic: Xác nhận bộ lọc thông minh quét trúng trường Content khi trường Subject thất bại
        assertEquals(1, result.getMeta().getTotal());
        assertSame(mail, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TC-MS-022 - getAllEmailsWithFilters() should return requested manual page slice

    @DisplayName("TC-MS-022 - getAllEmailsWithFilters() should return requested manual page slice")
    void tc_ms_022_getAllEmailsWithFilters_whenSecondPageRequested_shouldReturnSecondSlice() {
        // Arrange: Thiết lập phân trang lát cắt giả lập (Page slice), cấu hình Page chỉ số 1 (Tức trang số 2 thực tế)
        MailMessage first = createMail(222L, false, false, false, "A", "Body");
        MailMessage second = createMail(223L, false, false, false, "B", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 1), 2));

        // Act: Gửi yêu cầu tường minh lấy dữ liệu tại Trang số 2 với kích thước hiển thị 1 phần tử trên trang
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("all", null, null, "createdAt", "desc", 2, 1);

        // Assert: Xác thực thông tin phân trang khớp yêu cầu và lát cắt dữ liệu trả đúng bản ghi thứ hai (`second`)
        assertEquals(2, result.getMeta().getPage());
        assertEquals(2, result.getMeta().getPages());
        assertSame(second, ((List<?>) result.getResult()).get(0));
    }

    @Test
    // Test Case ID: TC-MS-023 - EXPECTED FAIL/Bug exposed: service currently accepts empty subject.
    @DisplayName("TC-MS-023 - EXPECTED FAIL - sendGmail() should reject empty subject and not save email history")
    void tc_ms_023_sendGmail_whenSubjectEmpty_shouldThrowAndNotSaveMailHistory() {
        // Arrange: Cấu hình hạ tầng trước khi test lỗi lỗ hổng phòng vệ dữ liệu đầu vào
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        // Act & Assert: Viết sẵn assert ngoại lệ đón đầu - Chạy thực tế sẽ FAIL do Dev thiếu code validate kiểm soát chuỗi rỗng
        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail("candidate@example.com", "", "Interview content"));
        
        // CheckDB: Đảm bảo nguyên tắc bảo vệ DB - Thư lỗi tiêu đề tuyệt đối không được phép sinh bản ghi lịch sử lưu trữ.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-MS-024 - EXPECTED FAIL/Bug exposed: service currently accepts empty content.
    @DisplayName("TC-MS-024 - EXPECTED FAIL - sendGmail() should reject empty content and not save email history")
    void tc_ms_024_sendGmail_whenContentEmpty_shouldThrowAndNotSaveMailHistory() {
        // Arrange: Thiết lập môi trường chạy thử nghiệm bắt lỗi
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        // Act & Assert: Đón đầu lỗi nghiệp vụ hệ thống chấp nhận nội dung email trống rỗng vô lý
        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail("candidate@example.com", "Interview invitation", ""));
        
        // CheckDB: Đảm bảo hệ thống không lưu vết các dữ liệu rác, lỗi cấu trúc nghiệp vụ xuống DB.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-MS-025 - EXPECTED FAIL/Bug exposed: service currently accepts null recipient.
    @DisplayName("TC-MS-025 - EXPECTED FAIL - sendGmail() should reject null recipient and not save email history")
    void tc_ms_025_sendGmail_whenRecipientIsNull_shouldThrowAndNotSaveMailHistory() {
        // Arrange: Thiết lập dữ liệu biên kiểm thử trường hợp địa chỉ người nhận bằng null
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        // Act & Assert: Mong đợi chặn lỗi NullPointerException từ xa bằng IllegalArgumentException có nghĩa
        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail(null, "Interview invitation", "Interview content"));
        
        // CheckDB: Xác minh tầng dữ liệu được bảo vệ an toàn khỏi các giá trị null độc hại phá vỡ cấu trúc bảng.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-MS-026 - EXPECTED FAIL/Bug exposed: deleted folder query can expose active emails.
    @DisplayName("TC-MS-026 - EXPECTED FAIL - getDeleted() should not return non-deleted emails")
    void tc_ms_026_getDeleted_whenRepositoryReturnsActiveMail_shouldNotExposeActiveMailInDeletedFolder() {
        // Arrange: Mô phỏng lỗi Database/Repository trả ra một mail đang hoạt động bình thường (deleted=false) vào thùng rác
        MailMessage activeMail = createMail(226L, false, false, false, "Active", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeMail), PageRequest.of(0, 10), 1));

        // Act: Thực hiện chức năng xem hòm thư mục rác (getDeleted)
        PaginationDTO result = mailboxService.getDeleted(1, 10);

        // Assert Core Logic: Test kì vọng hàm nghiệp vụ thông minh tự lọc bỏ thư hoạt động ra khỏi thùng rác (Kết quả mong muốn trống)
        assertTrue(((List<?>) result.getResult()).isEmpty());
    }

    @Test
    // Test Case ID: TC-MS-027 - mail send failure must not persist history.
    @DisplayName("TC-MS-027 - sendGmail() propagates sender failure and does not save mail history")
    void tc_ms_027_sendGmail_whenMailSenderFails_shouldThrowAndNotSaveMailHistory() {
        // Arrange: Giả lập lỗi kết nối vật lý hạ tầng truyền dẫn SMTP (SMTP Error Connection Link)
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");
        RuntimeException sendError = new RuntimeException("SMTP error");
        doThrow(sendError).when(mailSender).send(any(SimpleMailMessage.class));

        // Act: Thực hiện gửi và tóm bắt ngoại lệ lỗi truyền thông hạ tầng
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> mailboxService.sendGmail("candidate@example.com", "Offer", "Welcome"));

        // Assert: Xác thực lỗi được ném trả trực tiếp ra tầng điều hướng không bị che giấu luồng thông tin
        assertSame(sendError, exception);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        
        // CheckDB: Quy tắc ACID tối cao - Gửi thư thất bại thì tuyệt đối không được ghi nhận lịch sử thành công trong DB.
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-MS-028 - default folder/sort branches and pagination upper cap.
    @DisplayName("TC-MS-028 - getAllEmailsWithFilters() defaults folder and caps limit when parameters are blank")
    void tc_ms_028_getAllEmailsWithFilters_whenFolderAndSortBlank_shouldDefaultAndCapLimit() {
        // Arrange: Chuẩn bị dữ liệu đầu ra trang tối đa 100 thực thể phục vụ kiểm thử giá trị biên cực đại
        MailMessage mail = createMail(228L, false, false, false, "Default", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 100), 1));

        // Act: Đưa đồng thời các tham số dị biệt phân trang biên (-1, 999) và chuỗi trống
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("", null, null, "", "", -1, 999);

        // Assert Core Logic: Xác thực hệ thống chạy cơ chế bảo vệ kép vừa gán mặc định vừa ép trần kích thước trang về 100 bản ghi
        assertEquals(1, result.getMeta().getPage());
        assertEquals(100, result.getMeta().getPageSize());
        assertEquals(1, result.getMeta().getTotal());
        assertSame(mail, ((List<?>) result.getResult()).get(0));
        
        // CheckDB: Xác minh luồng chạy hạ tầng gọi chuẩn xác hàm nghiệp vụ chỉ định.
        verify(mailRepo).findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    // Test Case ID: TC-MS-029 - keyword branch should tolerate null subject and null content.
    @DisplayName("TC-MS-029 - getAllEmailsWithFilters() excludes mail with null subject and content when keyword is provided")
    void tc_ms_029_getAllEmailsWithFilters_whenKeywordProvidedAndMailTextNull_shouldReturnEmptyPage() {
        // Arrange: Tạo thực thể rỗng thông tin nội dung cực đoan trong DB (Subject=null, Content=null) chống NullPointerException
        MailMessage mail = createMail(229L, false, false, false, null, null);
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 10), 1));

        // Act: Thực hiện tìm kiếm từ khóa chuỗi chữ thường "interview" trên thực thể rỗng text
        PaginationDTO result = mailboxService.getAllEmailsWithFilters("all", null, "interview", "createdAt", "desc",
                1, 10);

        // Assert Core Logic: Hệ thống vận hành trơn tru không sập lỗi NPE, loại bỏ chính xác thư rỗng ra khỏi trang kết quả tìm kiếm
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());
        
        // CheckDB: Xác nhận thao tác kiểm tra an toàn dữ liệu truy vấn diễn ra đầy đủ.
        verify(mailRepo).findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

}
