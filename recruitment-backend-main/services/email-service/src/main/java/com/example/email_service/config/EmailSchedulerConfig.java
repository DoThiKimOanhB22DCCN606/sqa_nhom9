package com.example.email_service.config;

import com.example.email_service.service.GmailInboxService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class EmailSchedulerConfig {

    private final GmailInboxService gmailInboxService;

    public EmailSchedulerConfig(GmailInboxService gmailInboxService) {
        this.gmailInboxService = gmailInboxService;
    }

    /**
     * Đọc email từ Gmail inbox mỗi 5 phút
     * Có thể điều chỉnh thời gian theo nhu cầu
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300000 milliseconds
    public void fetchEmailsFromGmail() {
        System.out.println("Đang đọc email từ Gmail inbox...");
        gmailInboxService.fetchAndSaveEmails();
        System.out.println("Hoàn thành đọc email từ Gmail inbox.");
    }
}
