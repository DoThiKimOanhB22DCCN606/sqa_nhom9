package com.example.candidate_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.candidate_service.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCandidate_Id(Long candidateId);
}
