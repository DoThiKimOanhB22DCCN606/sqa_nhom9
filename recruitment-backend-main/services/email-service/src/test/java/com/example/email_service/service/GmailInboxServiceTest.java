package com.example.email_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    // Test Case ID: TCGIS001 - CheckDB save path for a normal Gmail message.
    @DisplayName("TCGIS001 - saveEmailFromGmail() saves full email data when message is valid")
    void tcgis001_saveEmailFromGmail_whenMessageValid_savesFullMailMessage() throws Exception {
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
        assertNotNull(savedMail.getCreatedAt());
    }

    @Test
    // Test Case ID: TCGIS002 - duplicate Message-ID branch must not write DB.
    @DisplayName("TCGIS002 - saveEmailFromGmail() skips save when Gmail message id already exists")
    void tcgis002_saveEmailFromGmail_whenMessageIdExists_skipsSave() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<duplicate@gmail>" });
        when(mailRepo.existsByGmailMessageId("<duplicate@gmail>")).thenReturn(true);

        ReflectionTestUtils.invokeMethod(gmailInboxService, "saveEmailFromGmail", message);

        verify(mailRepo, never()).save(any(MailMessage.class));
    }

    @Test
    // Test Case ID: TCGIS003 - missing Message-ID branch generates fallback id.
    @DisplayName("TCGIS003 - saveEmailFromGmail() generates id when Gmail message id is missing")
    void tcgis003_saveEmailFromGmail_whenMessageIdMissing_generatesFallbackId() throws Exception {
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
        assertNotNull(savedMail.getGmailMessageId());
        assertEquals("(No Subject)", savedMail.getSubject());
        assertEquals("No header body", savedMail.getContent());
        assertNull(savedMail.getFromEmail());
        assertNull(savedMail.getToEmail());
        assertNotNull(savedMail.getCreatedAt());
    }

    @Test
    // Test Case ID: TCGIS004 - private getMessageId true branch.
    @DisplayName("TCGIS004 - getMessageId() returns first Message-ID header")
    void tcgis004_getMessageId_whenHeaderExists_returnsFirstHeader() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[] { "<first>", "<second>" });

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        assertEquals("<first>", result);
    }

    @Test
    // Test Case ID: TCGIS005 - private getMessageId false branch.
    @DisplayName("TCGIS005 - getMessageId() returns null when header is absent")
    void tcgis005_getMessageId_whenHeaderAbsent_returnsNull() throws Exception {
        when(message.getHeader("Message-ID")).thenReturn(new String[0]);

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "getMessageId", message);

        assertNull(result);
    }

    @Test
    // Test Case ID: TCGIS006 - content extraction for plain String content.
    @DisplayName("TCGIS006 - extractContent() returns string content directly")
    void tcgis006_extractContent_whenContentIsString_returnsContent() throws Exception {
        when(message.getContent()).thenReturn("Plain text");

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("Plain text", result);
    }

    @Test
    // Test Case ID: TCGIS007 - content extraction for multipart plain and html bodies.
    @DisplayName("TCGIS007 - extractContent() concatenates text and html multipart bodies")
    void tcgis007_extractContent_whenContentIsMultipart_returnsCombinedText() throws Exception {
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
    // Test Case ID: TCGIS008 - unsupported content fail/empty branch.
    @DisplayName("TCGIS008 - extractContent() returns empty string for unsupported content type")
    void tcgis008_extractContent_whenContentUnsupported_returnsEmptyString() throws Exception {
        when(message.getContent()).thenReturn(Optional.empty());

        String result = ReflectionTestUtils.invokeMethod(gmailInboxService, "extractContent", message);

        assertEquals("", result);
    }

    @Test
    // Test Case ID: TCGIS009 - fail path: malformed message is not persisted.
    @DisplayName("TCGIS009 - saveEmailFromGmail() throws and does not save when message content fails")
    void tcgis009_saveEmailFromGmail_whenMessageContentFails_throwsAndDoesNotSave() throws Exception {
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
    // Test Case ID: TCGIS010 - empty from array branch should leave fromEmail null.
    @DisplayName("TCGIS010 - saveEmailFromGmail() leaves fromEmail null when from array is empty")
    void tcgis010_saveEmailFromGmail_whenFromArrayEmpty_savesNullFromEmail() throws Exception {
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
    // Test Case ID: TCGIS011 - empty to array branch should leave toEmail null.
    @DisplayName("TCGIS011 - saveEmailFromGmail() leaves toEmail null when recipient array is empty")
    void tcgis011_saveEmailFromGmail_whenRecipientArrayEmpty_savesNullToEmail() throws Exception {
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
    // Test Case ID: TCGIS012 - explicit html multipart branch.
    @DisplayName("TCGIS012 - extractContent() appends html body part")
    void tcgis012_extractContent_whenMultipartContainsHtmlOnly_returnsHtmlContent() throws Exception {
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
    // Test Case ID: TCGIS013 - fetch loop saves valid messages and continues after one message fails.
    @DisplayName("TCGIS013 - fetchAndSaveEmails() saves valid Gmail messages and skips failed message")
    void tcgis013_fetchAndSaveEmails_whenOneMessageFails_continuesAndClosesResources() throws Exception {
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
    // Test Case ID: TCGIS014 - outer catch branch must not crash or write DB when Gmail connection fails.
    @DisplayName("TCGIS014 - fetchAndSaveEmails() handles Gmail connection failure without saving")
    void tcgis014_fetchAndSaveEmails_whenStoreCannotOpen_doesNotThrowAndDoesNotSave() throws Exception {
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

}
