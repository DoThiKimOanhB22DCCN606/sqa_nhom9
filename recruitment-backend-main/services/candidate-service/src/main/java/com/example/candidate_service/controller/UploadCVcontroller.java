package com.example.candidate_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import com.example.candidate_service.dto.candidate.UploadCVDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.service.CandidateService;
import com.example.candidate_service.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/candidate-service/public")
public class UploadCVcontroller {

    private final CandidateService candidateService;

    public UploadCVcontroller(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping("/upload-cv")
    @ApiMessage("Upload CV với file (Public API)")
    public ResponseEntity<Candidate> createCandidateFromApplication(@Validated @RequestBody UploadCVDTO dto)
            throws IdInvalidException, IOException {

        return ResponseEntity.ok(candidateService.createCandidateFromApplication(dto));
    }
}
