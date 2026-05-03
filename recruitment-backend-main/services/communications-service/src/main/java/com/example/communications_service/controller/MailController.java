package com.example.communications_service.controller;

import com.example.communications_service.dto.PaginationDTO;
import com.example.communications_service.dto.mail.SimpleSendMailRequest;
import com.example.communications_service.model.MailMessage;
import com.example.communications_service.service.MailboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/communications-service/mail")
public class MailController {

    private final MailboxService mailboxService;

    public MailController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    // Gửi email qua Gmail với 3 trường: toEmail, subject, content
    @PostMapping("/send/gmail")
    public ResponseEntity<MailMessage> sendEmailViaGmail(@RequestBody SimpleSendMailRequest request) {
        return ResponseEntity.ok(
                mailboxService.sendGmail(
                        request.getToEmail(),
                        request.getSubject(),
                        request.getContent()));
    }

    // Inbox - Hộp thư đến
    @GetMapping("/inbox")
    public ResponseEntity<PaginationDTO> inbox(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(mailboxService.getInboxAll(page, limit));
    }

    // Sent - Hộp thư đã gửi
    @GetMapping("/sent")
    public ResponseEntity<PaginationDTO> sent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(mailboxService.getSent(page, limit));
    }

    // Đánh dấu đã đọc
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        mailboxService.markRead(id);
        return ResponseEntity.ok().build();
    }

    // Xóa vĩnh viễn
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable Long id) {
        mailboxService.permanentDelete(id);
        return ResponseEntity.ok().build();
    }

    // Get all emails với filter và phân trang
    @GetMapping
    public ResponseEntity<PaginationDTO> getAllEmails(
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(mailboxService.getAllEmailsWithFilters(
                folder, read, keyword,
                sortBy, sortOrder, page, limit));
    }
}
