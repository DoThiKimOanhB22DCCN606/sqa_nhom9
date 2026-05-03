package com.example.candidate_service.dto.comment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDTO {
    private Long candidateId;
    private String content;
}
