package com.example.communications_service.repository;

import com.example.communications_service.model.MailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailMessageRepository extends JpaRepository<MailMessage, Long> {

        Page<MailMessage> findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(Pageable pageable);

        Page<MailMessage> findByDeletedFalseAndSentTrueOrderByCreatedAtDesc(Pageable pageable);

        Page<MailMessage> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

        Long countBySentFalseAndReadFalseAndDeletedFalse();

        boolean existsByGmailMessageId(String gmailMessageId);

        MailMessage findByGmailMessageId(String gmailMessageId);

}
