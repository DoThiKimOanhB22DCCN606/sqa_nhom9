package com.example.email_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_message")
@Getter
@Setter
public class MailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_email")
    private String fromEmail; // Email thực của người gửi

    @Column(name = "to_email")
    private String toEmail; // Email thực của người nhận

    @Column(name = "is_sent")
    private boolean sent; // true nếu là thư đã gửi từ hệ thống, false nếu là thư nhận

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_read")
    private boolean read;

    @Column(name = "is_deleted")
    private boolean deleted;

    @Column(name = "gmail_message_id")
    private String gmailMessageId; // Message ID từ Gmail để tránh duplicate

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
