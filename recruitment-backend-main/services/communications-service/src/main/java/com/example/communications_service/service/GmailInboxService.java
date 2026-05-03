package com.example.communications_service.service;

import com.example.communications_service.model.MailMessage;
import com.example.communications_service.repository.MailMessageRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class GmailInboxService {

    private final MailMessageRepository mailRepo;

    @Value("${spring.mail.username}")
    private String gmailUsername;

    @Value("${spring.mail.password}")
    private String gmailPassword;

    public GmailInboxService(MailMessageRepository mailRepo) {
        this.mailRepo = mailRepo;
    }

    /**
     * Đọc email từ Gmail inbox và lưu vào database
     */
    public void fetchAndSaveEmails() {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", "imap.gmail.com");
            props.put("mail.imaps.port", "993");
            props.put("mail.imaps.ssl.enable", "true");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", gmailUsername, gmailPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Lấy các email chưa đọc
            Message[] messages = inbox.getMessages();

            // Đọc từ mới nhất đến cũ nhất
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                try {
                    saveEmailFromGmail(message);
                } catch (Exception e) {
                    // Log lỗi nhưng tiếp tục xử lý email tiếp theo
                    System.err.println("Lỗi khi lưu email (Message-ID: " +
                            (message.getMessageNumber()) + "): " + e.getMessage());
                    // Không in stack trace đầy đủ để tránh log quá dài
                    if (e.getCause() != null) {
                        System.err.println("  Nguyên nhân: " + e.getCause().getMessage());
                    }
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("Lỗi khi đọc email từ Gmail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lưu một email từ Gmail vào database
     * Sử dụng REQUIRES_NEW để mỗi email có transaction riêng, tránh lỗi khi có
     * exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveEmailFromGmail(Message message) throws Exception {
        // Kiểm tra xem email đã được lưu chưa (dựa vào Message-ID)
        String messageId = getMessageId(message);
        if (messageId != null && mailRepo.existsByGmailMessageId(messageId)) {
            return; // Email đã tồn tại, bỏ qua
        }
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }

        MailMessage mailMessage = new MailMessage();

        // Lấy thông tin người gửi
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            String fromEmail = ((InternetAddress) fromAddresses[0]).getAddress();
            mailMessage.setFromEmail(fromEmail);
        }

        // Lấy thông tin người nhận (lấy địa chỉ đầu tiên)
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            String toEmail = ((InternetAddress) toAddresses[0]).getAddress();
            mailMessage.setToEmail(toEmail);
        }

        // Subject
        mailMessage.setSubject(message.getSubject() != null ? message.getSubject() : "(No Subject)");

        // Content
        String content = extractContent(message);
        mailMessage.setContent(content);

        // Gmail Message ID
        mailMessage.setGmailMessageId(messageId);

        // Date
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            mailMessage.setCreatedAt(LocalDateTime.ofInstant(
                    sentDate.toInstant(), ZoneId.systemDefault()));
        } else {
            mailMessage.setCreatedAt(LocalDateTime.now());
        }

        // Đánh dấu là thư nhận
        mailMessage.setSent(false);

        // Lưu vào database
        mailRepo.save(mailMessage);
    }

    /**
     * Trích xuất nội dung email
     */
    private String extractContent(Message message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            StringBuilder textContent = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (bodyPart.isMimeType("text/plain")) {
                    textContent.append((String) bodyPart.getContent());
                } else if (bodyPart.isMimeType("text/html")) {
                    // Có thể parse HTML nếu cần
                    textContent.append((String) bodyPart.getContent());
                }
            }

            return textContent.toString();
        }

        return "";
    }

    /**
     * Lấy Message-ID từ email
     */
    private String getMessageId(Message message) throws Exception {
        String[] messageIds = message.getHeader("Message-ID");
        if (messageIds != null && messageIds.length > 0) {
            return messageIds[0];
        }
        return null;
    }

}
