package com.example.candidate_service.dto.comment;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String content;
    private LocalDateTime createdAt;
}
