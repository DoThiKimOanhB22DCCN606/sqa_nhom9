package com.example.candidate_service.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.candidate_service.dto.comment.CommentResponseDTO;
import com.example.candidate_service.dto.comment.CreateCommentDTO;
import com.example.candidate_service.dto.comment.UpdateCommentDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.model.Comment;
import com.example.candidate_service.repository.CandidateRepository;
import com.example.candidate_service.repository.CommentRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CandidateRepository candidateRepository;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, CandidateRepository candidateRepository,
            UserService userService) {
        this.commentRepository = commentRepository;
        this.candidateRepository = candidateRepository;
        this.userService = userService;
    }

    public List<CommentResponseDTO> getByCandidateId(Long candidateId, String token) throws IdInvalidException {
        ensureCandidateExists(candidateId);
        List<Comment> comments = commentRepository.findByCandidate_Id(candidateId);

        // Batch fetch employee names
        Set<Long> employeeIds = comments.stream()
                .map(Comment::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        JsonNode idToName = null;
        if (!employeeIds.isEmpty()) {
            idToName = userService.getEmployeeNames(employeeIds.stream().toList(), token).getBody();
        }

        final JsonNode finalIdToName = idToName;
        return comments.stream()
                .map(c -> toResponse(c, finalIdToName))
                .collect(Collectors.toList());
    }

    public CommentResponseDTO getById(Long id) throws IdInvalidException {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Bình luận không tồn tại"));
        return toResponse(comment, null);
    }

    @Transactional
    public CommentResponseDTO create(CreateCommentDTO dto, Long employeeId) throws IdInvalidException {
        Candidate candidate = candidateRepository.findById(dto.getCandidateId())
                .orElseThrow(() -> new IdInvalidException("Ứng viên không tồn tại"));
        Comment c = new Comment();
        c.setCandidate(candidate);
        c.setEmployeeId(employeeId);
        c.setContent(dto.getContent());
        Comment saved = commentRepository.save(c);
        return toResponse(saved, null);
    }

    @Transactional
    public CommentResponseDTO update(Long id, UpdateCommentDTO dto, Long employeeId) throws IdInvalidException {
        Comment c = commentRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Bình luận không tồn tại"));
        if (dto.getContent() != null) {
            c.setContent(dto.getContent());
        }
        Comment saved = commentRepository.save(c);
        return toResponse(saved, null);
    }

    @Transactional
    public void delete(Long id) throws IdInvalidException {
        if (!commentRepository.existsById(id)) {
            throw new IdInvalidException("Bình luận không tồn tại");
        }
        commentRepository.deleteById(id);
    }

    private void ensureCandidateExists(Long candidateId) throws IdInvalidException {
        if (!candidateRepository.existsById(candidateId)) {
            throw new IdInvalidException("Ứng viên không tồn tại");
        }
    }

    private CommentResponseDTO toResponse(Comment c, JsonNode idToName) {
        CommentResponseDTO d = new CommentResponseDTO();
        d.setId(c.getId());
        d.setEmployeeId(c.getEmployeeId());
        d.setContent(c.getContent());
        d.setCreatedAt(c.getCreatedAt());
        if (idToName != null && c.getEmployeeId() != null) {
            JsonNode nameNode = idToName.get(String.valueOf(c.getEmployeeId()));
            if (nameNode != null) {
                d.setEmployeeName(nameNode.asText());
            }
        }
        return d;
    }
}
