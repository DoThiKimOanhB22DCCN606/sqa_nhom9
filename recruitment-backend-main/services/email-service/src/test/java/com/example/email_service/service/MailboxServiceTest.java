package com.example.email_service.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    // Test Case ID: TCMS001 - sendGmail() should send email and save mail record

    @DisplayName("TCMS001 - sendGmail() should send email and save mail record")
    void tcms001_sendGmail_whenInputIsValid_shouldSendAndSaveMessage() {
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        when(mailRepo.save(any(MailMessage.class))).thenAnswer(invocation -> {
            MailMessage persisted = invocation.getArgument(0);
            persisted.setId(1L);
            return persisted;
        });

        MailMessage result = mailboxService.sendGmail("recipient@example.com", "Subject", "Hello content");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("noreply@example.com", result.getFromEmail());
        assertEquals("recipient@example.com", result.getToEmail());
        assertEquals("Subject", result.getSubject());
        assertEquals("Hello content", result.getContent());
        assertTrue(result.isSent());
        assertNotNull(result.getGmailMessageId());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        assertEquals("recipient@example.com", mailCaptor.getValue().getTo()[0]);
        verify(mailRepo, times(1)).save(any(MailMessage.class));
    }

    @Test

    // Test Case ID: TCMS002 - deleteMail() should soft delete mail when exists

    @DisplayName("TCMS002 - deleteMail() should soft delete mail when exists")
    void tcms002_deleteMail_whenMailExists_shouldMarkDeletedAndSave() {
        MailMessage mail = createMail(2L, false, false, false, "Hi", "Body");
        when(mailRepo.findById(2L)).thenReturn(Optional.of(mail));

        mailboxService.deleteMail(2L);

        assertTrue(mail.isDeleted());
        verify(mailRepo, times(1)).save(mail);
    }

    @Test

    // Test Case ID: TCMS003 - deleteMail() should do nothing when mail does not exist

    @DisplayName("TCMS003 - deleteMail() should do nothing when mail does not exist")
    void tcms003_deleteMail_whenMailMissing_shouldNotSave() {
        when(mailRepo.findById(3L)).thenReturn(Optional.empty());

        mailboxService.deleteMail(3L);

        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TCMS004 - permanentDelete() should delete mail when exists

    @DisplayName("TCMS004 - permanentDelete() should delete mail when exists")
    void tcms004_permanentDelete_whenMailExists_shouldDeleteRecord() {
        MailMessage mail = createMail(4L, true, false, false, "Subject", "Body");
        when(mailRepo.findById(4L)).thenReturn(Optional.of(mail));

        mailboxService.permanentDelete(4L);

        verify(mailRepo, times(1)).delete(mail);
    }

    @Test

    // Test Case ID: TCMS005 - getMailById() should return mail when present

    @DisplayName("TCMS005 - getMailById() should return mail when present")
    void tcms005_getMailById_whenMailExists_shouldReturnMail() {
        MailMessage mail = createMail(5L, false, false, false, "Hello", "World");
        when(mailRepo.findById(5L)).thenReturn(Optional.of(mail));

        MailMessage result = mailboxService.getMailById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test

    // Test Case ID: TCMS006 - getMailById() should throw when mail missing

    @DisplayName("TCMS006 - getMailById() should throw when mail missing")
    void tcms006_getMailById_whenMailMissing_shouldThrowRuntimeException() {
        when(mailRepo.findById(6L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> mailboxService.getMailById(6L));
        assertNotNull(exception.getMessage());
    }

    @Test

    // Test Case ID: TCMS007 - getAllEmailsWithFilters() should filter inbox, read and keyword correctly

    @DisplayName("TCMS007 - getAllEmailsWithFilters() should filter inbox, read and keyword correctly")
    void tcms007_getAllEmailsWithFilters_whenInboxReadAndKeyword_shouldReturnFilteredPagination() {
        MailMessage mail1 = createMail(7L, false, false, false, "Hello", "Meeting today");
        MailMessage mail2 = createMail(8L, false, true, false, "Hello", "Meeting today");
        MailMessage mail3 = createMail(9L, true, false, false, "Hello", "Meeting today");
        MailMessage mail4 = createMail(10L, false, false, false, "Update", "Hello world");

        Pageable pageable = PageRequest.of(0, 10);
        Page<MailMessage> allPage = new PageImpl<>(List.of(mail1, mail2, mail3, mail4), pageable, 4);
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(allPage);

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("inbox", false, "Hello", "createdAt", "desc", 1, 10);

        assertNotNull(result);
        assertEquals(2, ((List<?>) result.getResult()).size());
        assertEquals(2, result.getMeta().getTotal());
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
    }

    @Test

    // Test Case ID: TCMS008 - getUnreadCount() should return unread count from repository

    @DisplayName("TCMS008 - getUnreadCount() should return unread count from repository")
    void tcms008_getUnreadCount_shouldReturnCountFromRepository() {
        when(mailRepo.countBySentFalseAndReadFalseAndDeletedFalse()).thenReturn(7L);

        Long result = mailboxService.getUnreadCount();

        assertEquals(7L, result);
        verify(mailRepo, times(1)).countBySentFalseAndReadFalseAndDeletedFalse();
    }

    @Test

    // Test Case ID: TCMS009 - permanentDelete() should do nothing when mail does not exist

    @DisplayName("TCMS009 - permanentDelete() should do nothing when mail does not exist")
    void tcms009_permanentDelete_whenMailMissing_shouldNotDelete() {
        when(mailRepo.findById(209L)).thenReturn(Optional.empty());

        mailboxService.permanentDelete(209L);

        verify(mailRepo, never()).delete(any());
    }

    @Test

    // Test Case ID: TCMS010 - getInbox() should normalize invalid pagination and return inbox page

    @DisplayName("TCMS010 - getInbox() should normalize invalid pagination and return inbox page")
    void tcms010_getInbox_whenInvalidPagination_shouldNormalizeAndReturnInbox() {
        MailMessage mail = createMail(210L, false, false, false, "Inbox", "Body");
        when(mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 1), 1));

        PaginationDTO result = mailboxService.getInbox(0, 0);

        assertEquals(1, result.getMeta().getPage());
        assertEquals(1, result.getMeta().getPageSize());
        assertEquals(1, ((List<?>) result.getResult()).size());
    }

    @Test

    // Test Case ID: TCMS011 - getInboxAll() should use inbox repository and cap limit at 100

    @DisplayName("TCMS011 - getInboxAll() should use inbox repository and cap limit at 100")
    void tcms011_getInboxAll_whenLimitTooLarge_shouldCapAt100() {
        when(mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        PaginationDTO result = mailboxService.getInboxAll(1, 999);

        assertEquals(100, result.getMeta().getPageSize());
        verify(mailRepo).findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test

    // Test Case ID: TCMS012 - getSent() should return sent emails page

    @DisplayName("TCMS012 - getSent() should return sent emails page")
    void tcms012_getSent_shouldReturnSentEmails() {
        MailMessage sent = createMail(212L, true, true, false, "Sent", "Body");
        when(mailRepo.findByDeletedFalseAndSentTrueOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sent), PageRequest.of(0, 10), 1));

        PaginationDTO result = mailboxService.getSent(1, 10);

        assertEquals(1, result.getMeta().getTotal());
        assertSame(sent, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TCMS013 - getDeleted() should return repository page for deleted folder implementation

    @DisplayName("TCMS013 - getDeleted() should return repository page for deleted folder implementation")
    void tcms013_getDeleted_shouldReturnConfiguredRepositoryPage() {
        MailMessage mail = createMail(213L, false, false, false, "Deleted", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 10), 1));

        PaginationDTO result = mailboxService.getDeleted(1, 10);

        assertEquals(1, result.getMeta().getTotal());
        verify(mailRepo).findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test

    // Test Case ID: TCMS014 - markRead() should set read true and save when mail exists

    @DisplayName("TCMS014 - markRead() should set read true and save when mail exists")
    void tcms014_markRead_whenMailExists_shouldPersistReadState() {
        MailMessage mail = createMail(214L, false, false, false, "Unread", "Body");
        when(mailRepo.findById(214L)).thenReturn(Optional.of(mail));

        mailboxService.markRead(214L);

        assertTrue(mail.isRead());
        verify(mailRepo).save(mail);
    }

    @Test

    // Test Case ID: TCMS015 - markRead() should do nothing when mail is missing

    @DisplayName("TCMS015 - markRead() should do nothing when mail is missing")
    void tcms015_markRead_whenMailMissing_shouldNotSave() {
        when(mailRepo.findById(215L)).thenReturn(Optional.empty());

        mailboxService.markRead(215L);

        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TCMS016 - restoreMail() should set deleted false and save when mail exists

    @DisplayName("TCMS016 - restoreMail() should set deleted false and save when mail exists")
    void tcms016_restoreMail_whenMailExists_shouldPersistDeletedFalse() {
        MailMessage mail = createMail(216L, false, false, true, "Deleted", "Body");
        when(mailRepo.findById(216L)).thenReturn(Optional.of(mail));

        mailboxService.restoreMail(216L);

        assertFalse(mail.isDeleted());
        verify(mailRepo).save(mail);
    }

    @Test

    // Test Case ID: TCMS017 - restoreMail() should do nothing when mail is missing

    @DisplayName("TCMS017 - restoreMail() should do nothing when mail is missing")
    void tcms017_restoreMail_whenMailMissing_shouldNotSave() {
        when(mailRepo.findById(217L)).thenReturn(Optional.empty());

        mailboxService.restoreMail(217L);

        verify(mailRepo, never()).save(any());
    }

    @Test

    // Test Case ID: TCMS018 - getAllEmailsWithFilters() should default folder sort and pagination when inputs are blank

    @DisplayName("TCMS018 - getAllEmailsWithFilters() should default folder sort and pagination when inputs are blank")
    void tcms018_getAllEmailsWithFilters_whenInputsBlank_shouldUseDefaults() {
        MailMessage mail = createMail(218L, false, false, false, "Default", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 1), 1));

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("", null, "", "", "", -1, 0);

        assertEquals(1, result.getMeta().getPage());
        assertEquals(1, result.getMeta().getPageSize());
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test

    // Test Case ID: TCMS019 - getAllEmailsWithFilters() should filter sent folder

    @DisplayName("TCMS019 - getAllEmailsWithFilters() should filter sent folder")
    void tcms019_getAllEmailsWithFilters_whenSentFolder_shouldReturnOnlySentMessages() {
        MailMessage inbox = createMail(219L, false, false, false, "Inbox", "Body");
        MailMessage sent = createMail(220L, true, false, false, "Sent", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inbox, sent), PageRequest.of(0, 10), 2));

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("sent", null, null, "createdAt", "asc", 1, 10);

        assertEquals(1, result.getMeta().getTotal());
        assertSame(sent, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TCMS020 - getAllEmailsWithFilters() should return empty deleted folder because base query excludes deleted

    @DisplayName("TCMS020 - getAllEmailsWithFilters() should return empty deleted folder because base query excludes deleted")
    void tcms020_getAllEmailsWithFilters_whenDeletedFolder_shouldReturnEmptyDueToBaseQuery() {
        MailMessage active = createMail(220L, false, false, false, "Active", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 10), 1));

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("deleted", null, null, "createdAt", "desc", 1, 10);

        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());
    }

    @Test

    // Test Case ID: TCMS021 - getAllEmailsWithFilters() should match keyword in content when subject does not match

    @DisplayName("TCMS021 - getAllEmailsWithFilters() should match keyword in content when subject does not match")
    void tcms021_getAllEmailsWithFilters_whenKeywordMatchesContent_shouldReturnMessage() {
        MailMessage mail = createMail(221L, false, false, false, "No match", "Important interview details");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mail), PageRequest.of(0, 10), 1));

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("all", null, "interview", "createdAt", "desc", 1, 10);

        assertEquals(1, result.getMeta().getTotal());
        assertSame(mail, ((List<?>) result.getResult()).get(0));
    }

    @Test

    // Test Case ID: TCMS022 - getAllEmailsWithFilters() should return requested manual page slice

    @DisplayName("TCMS022 - getAllEmailsWithFilters() should return requested manual page slice")
    void tcms022_getAllEmailsWithFilters_whenSecondPageRequested_shouldReturnSecondSlice() {
        MailMessage first = createMail(222L, false, false, false, "A", "Body");
        MailMessage second = createMail(223L, false, false, false, "B", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 1), 2));

        PaginationDTO result = mailboxService.getAllEmailsWithFilters("all", null, null, "createdAt", "desc", 2, 1);

        assertEquals(2, result.getMeta().getPage());
        assertEquals(2, result.getMeta().getPages());
        assertSame(second, ((List<?>) result.getResult()).get(0));
    }

    @Test
    // Test Case ID: TCMS023 - EXPECTED FAIL/Bug exposed: service currently accepts empty subject.
    @DisplayName("TCMS023 - EXPECTED FAIL - sendGmail() should reject empty subject and not save email history")
    void tcms023_sendGmail_whenSubjectEmpty_shouldThrowAndNotSaveMailHistory() {
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail("candidate@example.com", "", "Interview content"));
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TCMS024 - EXPECTED FAIL/Bug exposed: service currently accepts empty content.
    @DisplayName("TCMS024 - EXPECTED FAIL - sendGmail() should reject empty content and not save email history")
    void tcms024_sendGmail_whenContentEmpty_shouldThrowAndNotSaveMailHistory() {
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail("candidate@example.com", "Interview invitation", ""));
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TCMS025 - EXPECTED FAIL/Bug exposed: service currently accepts null recipient.
    @DisplayName("TCMS025 - EXPECTED FAIL - sendGmail() should reject null recipient and not save email history")
    void tcms025_sendGmail_whenRecipientIsNull_shouldThrowAndNotSaveMailHistory() {
        ReflectionTestUtils.setField(mailboxService, "fromEmail", "noreply@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> mailboxService.sendGmail(null, "Interview invitation", "Interview content"));
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TCMS026 - EXPECTED FAIL/Bug exposed: deleted folder query can expose active emails.
    @DisplayName("TCMS026 - EXPECTED FAIL - getDeleted() should not return non-deleted emails")
    void tcms026_getDeleted_whenRepositoryReturnsActiveMail_shouldNotExposeActiveMailInDeletedFolder() {
        MailMessage activeMail = createMail(226L, false, false, false, "Active", "Body");
        when(mailRepo.findByDeletedFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeMail), PageRequest.of(0, 10), 1));

        PaginationDTO result = mailboxService.getDeleted(1, 10);

        assertTrue(((List<?>) result.getResult()).isEmpty());
    }


}
