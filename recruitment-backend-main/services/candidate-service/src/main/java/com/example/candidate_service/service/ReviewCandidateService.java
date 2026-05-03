package com.example.candidate_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.candidate_service.dto.Meta;
import com.example.candidate_service.dto.PaginationDTO;
import com.example.candidate_service.dto.review.CreateReviewCandidateDTO;
import com.example.candidate_service.dto.review.ReviewCandidateResponseDTO;
import com.example.candidate_service.dto.review.UpdateReviewCandidateDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.model.ReviewCandidate;
import com.example.candidate_service.repository.CandidateRepository;
import com.example.candidate_service.repository.ReviewCandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ReviewCandidateService {

    private final ReviewCandidateRepository reviewCandidateRepository;
    private final CandidateRepository candidateRepository;
    private final UserService userService;

    public ReviewCandidateService(ReviewCandidateRepository reviewCandidateRepository,
            CandidateRepository candidateRepository,
            UserService userService) {
        this.reviewCandidateRepository = reviewCandidateRepository;
        this.candidateRepository = candidateRepository;
        this.userService = userService;
    }

    public PaginationDTO getAllWithFilters(Long candidateId, Long reviewerId,
            LocalDateTime startDate, LocalDateTime endDate,
            int page, int limit, String sortBy, String sortOrder,
            String token) {
        // Validate pagination parameters
        if (page < 1)
            page = 1;
        if (limit < 1 || limit > 100)
            limit = 10;

        // Create sort object
        Sort.Direction direction = sortOrder != null && sortOrder.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort sort = Sort.by(direction, sortField);

        // Create pageable object
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        // Query with filters
        Page<ReviewCandidate> reviewPage = reviewCandidateRepository.findByFilters(
                candidateId, reviewerId, startDate, endDate, pageable);

        // Convert to DTOs
        List<ReviewCandidateResponseDTO> reviewDTOs = convertToResponseList(reviewPage.getContent(), token);

        // Create pagination metadata
        Meta meta = new Meta();
        meta.setPage(page);
        meta.setPageSize(limit);
        meta.setPages(reviewPage.getTotalPages());
        meta.setTotal(reviewPage.getTotalElements());

        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setMeta(meta);
        paginationDTO.setResult(reviewDTOs);

        return paginationDTO;
    }

    public List<ReviewCandidateResponseDTO> getByCandidateId(Long candidateId, String token) {
        List<ReviewCandidate> reviews = reviewCandidateRepository.findByCandidate_Id(candidateId);
        return convertToResponseList(reviews, token);
    }

    public ReviewCandidateResponseDTO getById(Long id, String token) throws IdInvalidException {
        ReviewCandidate review = reviewCandidateRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        JsonNode idToName = null;
        if (review.getReviewerId() != null && token != null && !token.isEmpty()) {
            var response = userService.getEmployeeNames(List.of(review.getReviewerId()), token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                idToName = response.getBody();
            }
        }

        return convertToResponse(review, idToName);
    }

    @Transactional
    public ReviewCandidateResponseDTO create(CreateReviewCandidateDTO dto, Long reviewerId) throws IdInvalidException {
        // Validation cho INTERVIEW
        if (dto.getCandidateId() == null) {
            throw new IdInvalidException("Candidate ID là bắt buộc cho đánh giá phỏng vấn");
        }
        Candidate candidate = candidateRepository.findById(dto.getCandidateId())
                .orElseThrow(() -> new IdInvalidException("Ứng viên không tồn tại"));

        ReviewCandidate review = new ReviewCandidate();
        review.setReviewerId(reviewerId);
        review.setCandidate(candidate);

        // INTERVIEW chỉ cần 3 tiêu chí: chuyên môn, giao tiếp, kinh nghiệm
        review.setProfessionalSkillScore(dto.getProfessionalSkillScore());
        review.setCommunicationSkillScore(dto.getCommunicationSkillScore());
        review.setWorkExperienceScore(dto.getWorkExperienceScore());

        // Các trường chung
        review.setStrengths(dto.getStrengths());
        review.setWeaknesses(dto.getWeaknesses());
        review.setConclusion(dto.getConclusion());

        ReviewCandidate saved = reviewCandidateRepository.save(review);
        return convertToResponse(saved, null);
    }

    @Transactional
    public ReviewCandidateResponseDTO update(Long id, UpdateReviewCandidateDTO dto, Long reviewerId)
            throws IdInvalidException {
        ReviewCandidate review = reviewCandidateRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        // Chỉ cho phép người tạo review mới được update
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IdInvalidException("Bạn không có quyền cập nhật đánh giá này");
        }

        // Update INTERVIEW fields
        if (dto.getProfessionalSkillScore() != null) {
            review.setProfessionalSkillScore(dto.getProfessionalSkillScore());
        }
        if (dto.getCommunicationSkillScore() != null) {
            review.setCommunicationSkillScore(dto.getCommunicationSkillScore());
        }
        if (dto.getWorkExperienceScore() != null) {
            review.setWorkExperienceScore(dto.getWorkExperienceScore());
        }

        // Update các trường chung
        if (dto.getStrengths() != null) {
            review.setStrengths(dto.getStrengths());
        }
        if (dto.getWeaknesses() != null) {
            review.setWeaknesses(dto.getWeaknesses());
        }
        if (dto.getConclusion() != null) {
            review.setConclusion(dto.getConclusion());
        }

        ReviewCandidate saved = reviewCandidateRepository.save(review);
        return convertToResponse(saved, null);
    }

    @Transactional
    public void delete(Long id, Long reviewerId) throws IdInvalidException {
        ReviewCandidate review = reviewCandidateRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        // Chỉ cho phép người tạo review mới được xóa
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IdInvalidException("Bạn không có quyền xóa đánh giá này");
        }

        reviewCandidateRepository.deleteById(id);
    }

    private List<ReviewCandidateResponseDTO> convertToResponseList(List<ReviewCandidate> reviews, String token) {
        // Batch fetch reviewer names
        Set<Long> reviewerIds = reviews.stream()
                .map(ReviewCandidate::getReviewerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        JsonNode idToName = null;
        if (!reviewerIds.isEmpty() && token != null && !token.isEmpty()) {
            var response = userService.getEmployeeNames(reviewerIds.stream().toList(), token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                idToName = response.getBody();
            }
        }

        final JsonNode finalIdToName = idToName;
        return reviews.stream()
                .map(r -> convertToResponse(r, finalIdToName))
                .collect(Collectors.toList());
    }

    private ReviewCandidateResponseDTO convertToResponse(ReviewCandidate review, JsonNode idToName) {
        ReviewCandidateResponseDTO dto = new ReviewCandidateResponseDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getReviewerId());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());

        // Set candidateId
        if (review.getCandidate() != null) {
            dto.setCandidateId(review.getCandidate().getId());
        }

        // INTERVIEW: chỉ 3 tiêu chí
        dto.setProfessionalSkillScore(review.getProfessionalSkillScore());
        dto.setCommunicationSkillScore(review.getCommunicationSkillScore());
        dto.setWorkExperienceScore(review.getWorkExperienceScore());

        // Tính điểm trung bình từ 3 tiêu chí
        if (review.getProfessionalSkillScore() != null &&
                review.getCommunicationSkillScore() != null &&
                review.getWorkExperienceScore() != null) {
            double sum = review.getProfessionalSkillScore() +
                    review.getCommunicationSkillScore() +
                    review.getWorkExperienceScore();
            dto.setAverageScore(sum / 3.0);
        }

        // Các trường chung
        dto.setStrengths(review.getStrengths());
        dto.setWeaknesses(review.getWeaknesses());
        dto.setConclusion(review.getConclusion());

        // Set reviewer name
        if (idToName != null && review.getReviewerId() != null) {
            JsonNode nameNode = idToName.get(String.valueOf(review.getReviewerId()));
            if (nameNode != null) {
                dto.setReviewerName(nameNode.asText());
            }
        }

        return dto;
    }
}
