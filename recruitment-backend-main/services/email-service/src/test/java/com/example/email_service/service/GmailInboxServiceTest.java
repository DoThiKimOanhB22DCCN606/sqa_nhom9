package com.example.email_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.email_service.model.MailMessage;
import com.example.email_service.repository.MailMessageRepository;

import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
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
        Date sentDate = Date.from(LocalDateTime.of(2026, 5, 3, 9, 30).atZone(ZoneId.systemDefault()).toInstant());
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<msg-001@gmail>" });
        when(mailRepo.existsByGmailMessageId("<msg-001@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("Interview invitation");
        when(message.getContent()).thenReturn("Plain body");
        when(message.getSentDate()).thenReturn(sentDate);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

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
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<duplicate@gmail>" });
        when(mailRepo.existsByGmailMessageId("<duplicate@gmail>")).thenReturn(true);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-003 - missing Message-ID branch generates fallback id.
    @DisplayName("TC-GIS-003 - saveEmailFromGmail() generates id when Gmail message id is missing")
    void tc_gis_003_saveEmailFromGmail_whenMessageIdMissing_generatesFallbackId() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(null);
        when(message.getFrom()).thenReturn(null);
        when(message.getRecipients(RecipientType.TO)).thenReturn(null);
        when(message.getSubject()).thenReturn(null);
        when(message.getContent()).thenReturn("No header body");
        when(message.getSentDate()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

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
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<first>", "<second>" });

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        assertEquals("<first>", result);
    }

    @Test
    // Test Case ID: TC-GIS-005 - private getMessageId false branch.
    @DisplayName("TC-GIS-005 - getMessageId() returns null when header is absent")
    void tc_gis_005_getMessageId_whenHeaderAbsent_returnsNull() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[0]);

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        assertNull(result);
    }

    @Test
    // Test Case ID: TC-GIS-006 - content extraction for plain String content.
    @DisplayName("TC-GIS-006 - extractContent() returns string content directly")
    void tc_gis_006_extractContent_whenContentIsString_returnsContent() throws Exception {
        when(message.getContent()).thenReturn("Plain text");

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("Plain text", result);
    }

    @Test
    // Test Case ID: TC-GIS-007 - content extraction for multipart plain and html bodies.
    @DisplayName("TC-GIS-007 - extractContent() concatenates text and html multipart bodies")
    void tc_gis_007_extractContent_whenContentIsMultipart_returnsCombinedText() throws Exception {
        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText("Plain part");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<b>Html part</b>", "text/html");
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(plainPart);
        multipart.addBodyPart(htmlPart);
        when(message.getContent()).thenReturn(multipart);

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertTrue(result.contains("Plain part"));
        assertTrue(result.contains("Html part"));
    }

    @Test
    // Test Case ID: TC-GIS-008 - unsupported content fail/empty branch.
    @DisplayName("TC-GIS-008 - extractContent() returns empty string for unsupported content type")
    void tc_gis_008_extractContent_whenContentUnsupported_returnsEmptyString() throws Exception {
        when(message.getContent()).thenReturn(Optional.empty());

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("", result);
    }

    @Test
    // Test Case ID: TC-GIS-009 - fail path: malformed message is not persisted.
    @DisplayName("TC-GIS-009 - saveEmailFromGmail() throws and does not save when message content fails")
    void tc_gis_009_saveEmailFromGmail_whenMessageContentFails_throwsAndDoesNotSave() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<bad-content@gmail>" });
        when(mailRepo.existsByGmailMessageId("<bad-content@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("Broken content");
        when(message.getContent()).thenThrow(new RuntimeException("cannot read content"));

        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message));
        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-010 - empty from array branch should leave fromEmail null.
    @DisplayName("TC-GIS-010 - saveEmailFromGmail() leaves fromEmail null when from array is empty")
    void tc_gis_010_saveEmailFromGmail_whenFromArrayEmpty_savesNullFromEmail() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<empty-from@gmail>" });
        when(mailRepo.existsByGmailMessageId("<empty-from@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[0]);
        when(message.getRecipients(RecipientType.TO))
                .thenReturn(new InternetAddress[] { new InternetAddress("receiver@example.com") });
        when(message.getSubject()).thenReturn("No from");
        when(message.getContent()).thenReturn("Body");
        when(message.getSentDate()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertNull(mailCaptor.getValue().getFromEmail());
    }

    @Test
    // Test Case ID: TC-GIS-011 - empty to array branch should leave toEmail null.
    @DisplayName("TC-GIS-011 - saveEmailFromGmail() leaves toEmail null when recipient array is empty")
    void tc_gis_011_saveEmailFromGmail_whenRecipientArrayEmpty_savesNullToEmail() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<empty-to@gmail>" });
        when(mailRepo.existsByGmailMessageId("<empty-to@gmail>")).thenReturn(false);
        when(message.getFrom()).thenReturn(new InternetAddress[] { new InternetAddress("sender@example.com") });
        when(message.getRecipients(RecipientType.TO)).thenReturn(new InternetAddress[0]);
        when(message.getSubject()).thenReturn("No to");
        when(message.getContent()).thenReturn("Body");
        when(message.getSentDate()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertNull(mailCaptor.getValue().getToEmail());
    }

    @Test
    // Test Case ID: TC-GIS-012 - explicit html multipart branch.
    @DisplayName("TC-GIS-012 - extractContent() appends html body part")
    void tc_gis_012_extractContent_whenMultipartContainsHtmlOnly_returnsHtmlContent() throws Exception {
        MimeMultipart multipart = mock(MimeMultipart.class);
        BodyPart htmlPart = mock(BodyPart.class);
        when(message.getContent()).thenReturn(multipart);
        when(multipart.getCount()).thenReturn(1);
        when(multipart.getBodyPart(0)).thenReturn(htmlPart);
        when(htmlPart.isMimeType("text/plain")).thenReturn(false);
        when(htmlPart.isMimeType("text/html")).thenReturn(true);
        when(htmlPart.getContent()).thenReturn("<p>HTML only</p>");

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("<p>HTML only</p>", result);
    }

    @Test
    // Test Case ID: TC-GIS-013 - fetch loop saves valid messages and continues after one message fails.
    @DisplayName("TC-GIS-013 - fetchAndSaveEmails() saves valid Gmail messages and skips failed message")
    void tc_gis_013_fetchAndSaveEmails_whenOneMessageFails_continuesAndClosesResources() throws Exception {
        Session session = mock(Session.class);
        Store store = mock(Store.class);
        Folder inbox = mock(Folder.class);
        Message validMessage = mock(Message.class);
        Message brokenMessage = mock(Message.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

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

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        ArgumentCaptor<MailMessage> mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailRepo).save(mailCaptor.capture());
        assertEquals("<valid@gmail>", mailCaptor.getValue().getGmailMessageId());
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-014 - outer catch branch must not crash or write DB when Gmail connection fails.
    @DisplayName("TC-GIS-014 - fetchAndSaveEmails() handles Gmail connection failure without saving")
    void tc_gis_014_fetchAndSaveEmails_whenStoreCannotOpen_doesNotThrowAndDoesNotSave() throws Exception {
        Session session = mock(Session.class);
        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenThrow(new RuntimeException("imap unavailable"));

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-015 - fetch loop must skip duplicate Gmail messages and close resources.
    @DisplayName("TC-GIS-015 - fetchAndSaveEmails() skips duplicate Gmail message without saving")
    void tc_gis_015_fetchAndSaveEmails_whenMessageIdAlreadyExists_skipsSaveAndClosesResources() throws Exception {
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

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        verify(mailRepo).existsByGmailMessageId("<duplicate@gmail>");
        verify(mailRepo, never()).save(any(MailMessage.class));
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-016 - outer catch must handle authentication/connect failure.
    @DisplayName("TC-GIS-016 - fetchAndSaveEmails() handles Gmail connect failure without saving")
    void tc_gis_016_fetchAndSaveEmails_whenStoreConnectFails_doesNotThrowAndDoesNotSave() throws Exception {
        Session session = mock(Session.class);
        Store store = mock(Store.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenReturn(store);
        doThrow(new RuntimeException("auth fail"))
                .when(store).connect("imap.gmail.com", "tester@gmail.com", "app-password");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TC-GIS-017 - empty Gmail inbox should close resources and not save.
    @DisplayName("TC-GIS-017 - fetchAndSaveEmails() closes resources when Gmail inbox is empty")
    void tc_gis_017_fetchAndSaveEmails_whenInboxIsEmpty_shouldCloseResourcesAndNotSave() throws Exception {
        Session session = mock(Session.class);
        Store store = mock(Store.class);
        Folder inbox = mock(Folder.class);

        ReflectionTestUtils.setField(gmailInboxService, "gmailUsername", "tester@gmail.com");
        ReflectionTestUtils.setField(gmailInboxService, "gmailPassword", "app-password");

        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.getMessages()).thenReturn(new Message[0]);

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.getInstance(any(Properties.class))).thenReturn(session);

            gmailInboxService.fetchAndSaveEmails();
        }

        verify(mailRepo, never()).save(any(MailMessage.class));
        verify(inbox).close(false);
        verify(store).close();
    }

    @Test
    // Test Case ID: TC-GIS-018 - unsupported multipart body part should be ignored.
    @DisplayName("TC-GIS-018 - extractContent() ignores unsupported multipart body parts")
    void tc_gis_018_extractContent_whenMultipartPartIsUnsupported_shouldReturnEmptyString() throws Exception {
        MimeMultipart multipart = mock(MimeMultipart.class);
        BodyPart attachmentPart = mock(BodyPart.class);
        when(message.getContent()).thenReturn(multipart);
        when(multipart.getCount()).thenReturn(1);
        when(multipart.getBodyPart(0)).thenReturn(attachmentPart);
        when(attachmentPart.isMimeType("text/plain")).thenReturn(false);
        when(attachmentPart.isMimeType("text/html")).thenReturn(false);

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("", result);
    }

}
