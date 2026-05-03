package com.example.communications_service.service;

import com.example.communications_service.dto.Meta;
import com.example.communications_service.dto.PaginationDTO;
import com.example.communications_service.model.MailMessage;
import com.example.communications_service.repository.MailMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

@Service
@Transactional
public class MailboxService {

    private final MailMessageRepository mailRepo;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailboxService(MailMessageRepository mailRepo, JavaMailSender mailSender) {
        this.mailRepo = mailRepo;
        this.mailSender = mailSender;
    }

    // Gửi email qua Gmail và lưu DB
    public MailMessage sendGmail(String toEmail, String subject, String content) {
        // Gửi email thực
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);

        // Lưu vào database
        MailMessage mailMessage = new MailMessage();
        mailMessage.setFromEmail(fromEmail);
        mailMessage.setToEmail(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setContent(content);
        mailMessage.setSent(true);
        mailMessage.setGmailMessageId(UUID.randomUUID().toString());

        return mailRepo.save(mailMessage);
    }

    // Xóa email (soft delete)
    public void deleteMail(Long id) {
        mailRepo.findById(id).ifPresent(m -> {
            m.setDeleted(true);
            mailRepo.save(m);
        });
    }

    // Xóa vĩnh viễn
    public void permanentDelete(Long id) {
        mailRepo.findById(id).ifPresent(mailRepo::delete);
    }

    // Đánh dấu quan trọng
    // Important / Starred không hỗ trợ trong phiên bản đơn giản

    // Lấy email theo ID
    public MailMessage getMailById(Long id) {
        return mailRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
    }

    // Lấy inbox (chỉ email chưa xóa)
    public PaginationDTO getInbox(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, limit), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailMessage> p = mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(pageable);
        Meta meta = new Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(p.getTotalElements());
        meta.setPages(p.getTotalPages());
        PaginationDTO dto = new PaginationDTO();
        dto.setMeta(meta);
        dto.setResult(p.getContent());
        return dto;
    }

    // Lấy inbox cho tất cả tài khoản (không lọc theo user)
    public PaginationDTO getInboxAll(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, limit), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailMessage> p = mailRepo.findByDeletedFalseAndSentFalseOrderByCreatedAtDesc(pageable);
        Meta meta = new Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(p.getTotalElements());
        meta.setPages(p.getTotalPages());
        PaginationDTO dto = new PaginationDTO();
        dto.setMeta(meta);
        dto.setResult(p.getContent());
        return dto;
    }

    // public PaginationDTO getInbox(int page, int limit) {
    // Pageable pageable = PageRequest.of(Math.max(0, page - 1),
    // Math.min(Math.max(1, limit), 100),
    // Sort.by(Sort.Direction.DESC, "createdAt"));
    // Page<MailMessage> p =
    // mailRepo.findByToTypeAndDeletedFalseOrderByCreatedAtDesc("USER",
    // pageable);
    // Meta meta = new Meta();
    // meta.setPage(pageable.getPageNumber() + 1);
    // meta.setPageSize(pageable.getPageSize());
    // meta.setTotal(p.getTotalElements());
    // meta.setPages(p.getTotalPages());
    // PaginationDTO dto = new PaginationDTO();
    // dto.setMeta(meta);
    // dto.setResult(p.getContent());
    // return dto;
    // }
    // Lấy sent (chỉ email chưa xóa)
    public PaginationDTO getSent(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, limit), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailMessage> p = mailRepo.findByDeletedFalseAndSentTrueOrderByCreatedAtDesc(pageable);
        Meta meta = new Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(p.getTotalElements());
        meta.setPages(p.getTotalPages());
        PaginationDTO dto = new PaginationDTO();
        dto.setMeta(meta);
        dto.setResult(p.getContent());
        return dto;
    }

    // Lấy email đã xóa
    public PaginationDTO getDeleted(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, limit), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MailMessage> p = mailRepo.findByDeletedFalseOrderByCreatedAtDesc(pageable);
        Meta meta = new Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(p.getTotalElements());
        meta.setPages(p.getTotalPages());
        PaginationDTO dto = new PaginationDTO();
        dto.setMeta(meta);
        dto.setResult(p.getContent());
        return dto;
    }

    // Lấy email quan trọng
    // Important / Starred không hỗ trợ trong phiên bản đơn giản

    public void markRead(Long id) {
        mailRepo.findById(id).ifPresent(m -> {
            m.setRead(true);
            mailRepo.save(m);
        });
    }

    // Khôi phục email đã xóa
    public void restoreMail(Long id) {
        mailRepo.findById(id).ifPresent(m -> {
            m.setDeleted(false);
            mailRepo.save(m);
        });
    }

    // Đếm email chưa đọc
    public Long getUnreadCount() {
        return mailRepo.countBySentFalseAndReadFalseAndDeletedFalse();
    }

    /**
     * Lấy tất cả email với nhiều filter và phân trang
     * 
     * @param employeeId ID của nhân viên (bắt buộc)
     * @param folder     Folder: inbox, sent, deleted, important, starred, all (mặc
     *                   định: all)
     * @param read       true/false/null - lọc theo trạng thái đã đọc
     * @param important  true/false/null - lọc theo email quan trọng
     * @param starred    true/false/null - lọc theo email đã đánh dấu sao
     * @param external   true/false/null - lọc theo email bên ngoài
     * @param keyword    Từ khóa tìm kiếm trong subject (không search trong content
     *                   do CLOB)
     * @param sortBy     Trường để sort (mặc định: createdAt)
     * @param sortOrder  asc hoặc desc (mặc định: desc)
     * @param page       Số trang (mặc định: 1)
     * @param limit      Số lượng mỗi trang (mặc định: 10, tối đa: 100)
     * @return PaginationDTO chứa danh sách email và metadata
     */
    public PaginationDTO getAllEmailsWithFilters(
            String folder,
            Boolean read,
            String keyword,
            String sortBy,
            String sortOrder,
            int page,
            int limit) {

        // Validate và normalize parameters
        if (folder == null || folder.isEmpty()) {
            folder = "all";
        }

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "desc";
        }

        // Validate pagination
        page = Math.max(1, page);
        limit = Math.min(Math.max(1, limit), 100);

        // Create sort
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        Page<MailMessage> mailPage;
        String normalizedKeyword = keyword != null && !keyword.isEmpty() ? keyword.toLowerCase() : null;
        boolean isInbox = folder.equalsIgnoreCase("inbox");
        boolean isSent = folder.equalsIgnoreCase("sent");
        boolean isDeleted = folder.equalsIgnoreCase("deleted");

        // Simple filtering in-memory for readability (small dataset expected)
        mailPage = mailRepo.findByDeletedFalseOrderByCreatedAtDesc(pageable);

        var filtered = mailPage.getContent().stream()
                .filter(m -> {
                    if (isDeleted)
                        return m.isDeleted();
                    if (isInbox)
                        return !m.isSent() && !m.isDeleted();
                    if (isSent)
                        return m.isSent() && !m.isDeleted();
                    return !m.isDeleted();
                })
                .filter(m -> read == null || m.isRead() == read)
                .filter(m -> normalizedKeyword == null
                        || (m.getSubject() != null && m.getSubject().toLowerCase().contains(normalizedKeyword))
                        || (m.getContent() != null && m.getContent().toLowerCase().contains(normalizedKeyword)))
                .toList();

        // manual paging
        int from = Math.min(page * limit - limit, filtered.size());
        int to = Math.min(from + limit, filtered.size());
        var pageContent = filtered.subList(from, to);

        // Build response
        Meta meta = new Meta();
        meta.setPage(page);
        meta.setPageSize(limit);
        meta.setTotal(filtered.size());
        meta.setPages((int) Math.ceil((double) filtered.size() / limit));

        PaginationDTO dto = new PaginationDTO();
        dto.setMeta(meta);
        dto.setResult(pageContent);

        return dto;
    }

    // Phương thức gửi nội bộ / tới candidate không còn dùng trong phiên bản đơn
    // giản
}
