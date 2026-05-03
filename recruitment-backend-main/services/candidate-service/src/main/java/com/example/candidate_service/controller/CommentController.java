package com.example.candidate_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.candidate_service.dto.comment.CommentResponseDTO;
import com.example.candidate_service.dto.comment.CreateCommentDTO;
import com.example.candidate_service.dto.comment.UpdateCommentDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.service.CommentService;
import com.example.candidate_service.utils.SecurityUtil;
import com.example.candidate_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/candidate-service/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách bình luận theo candidate")
    public ResponseEntity<List<CommentResponseDTO>> listByCandidate(@RequestParam Long candidateId)
            throws IdInvalidException {
        String token = SecurityUtil.getCurrentUserJWT().orElse("");
        return ResponseEntity.ok(commentService.getByCandidateId(candidateId, token));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết bình luận")
    public ResponseEntity<CommentResponseDTO> getById(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(commentService.getById(id));
    }

    @PostMapping
    @ApiMessage("Tạo bình luận")
    public ResponseEntity<CommentResponseDTO> create(@Validated @RequestBody CreateCommentDTO dto)
            throws IdInvalidException {
        Long employeeId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(commentService.create(dto, employeeId));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật bình luận")
    public ResponseEntity<CommentResponseDTO> update(@PathVariable Long id,
            @Validated @RequestBody UpdateCommentDTO dto) throws IdInvalidException {
        Long employeeId = SecurityUtil.extractEmployeeId();
        return ResponseEntity.ok(commentService.update(id, dto, employeeId));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa bình luận")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IdInvalidException {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
