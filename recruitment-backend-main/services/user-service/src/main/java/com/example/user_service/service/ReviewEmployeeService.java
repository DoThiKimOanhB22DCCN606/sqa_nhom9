package com.example.user_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.dto.Meta;
import com.example.user_service.dto.PaginationDTO;
import com.example.user_service.dto.review.CreateReviewEmployeeDTO;
import com.example.user_service.dto.review.ReviewEmployeeResponseDTO;
import com.example.user_service.dto.review.UpdateReviewEmployeeDTO;
import com.example.user_service.exception.IdInvalidException;
import com.example.user_service.model.Employee;
import com.example.user_service.model.ReviewEmployee;
import com.example.user_service.repository.EmployeeRepository;
import com.example.user_service.repository.ReviewEmployeeRepository;

@Service
public class ReviewEmployeeService {

    private final ReviewEmployeeRepository reviewEmployeeRepository;
    private final EmployeeRepository employeeRepository;

    public ReviewEmployeeService(ReviewEmployeeRepository reviewEmployeeRepository,
            EmployeeRepository employeeRepository) {
        this.reviewEmployeeRepository = reviewEmployeeRepository;
        this.employeeRepository = employeeRepository;
    }

    public PaginationDTO getAllWithFilters(Long employeeId, Long reviewerId,
            LocalDateTime startDate, LocalDateTime endDate,
            int page, int limit, String sortBy, String sortOrder) {
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
        Page<ReviewEmployee> reviewPage = reviewEmployeeRepository.findByFilters(
                employeeId, reviewerId, startDate, endDate, pageable);

        // Convert to DTOs
        List<ReviewEmployeeResponseDTO> reviewDTOs = convertToResponseList(reviewPage.getContent());

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

    public List<ReviewEmployeeResponseDTO> getByEmployeeId(Long employeeId) {
        List<ReviewEmployee> reviews = reviewEmployeeRepository.findByEmployee_Id(employeeId);
        return convertToResponseList(reviews);
    }

    public ReviewEmployeeResponseDTO getById(Long id) throws IdInvalidException {
        ReviewEmployee review = reviewEmployeeRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        return convertToResponse(review);
    }

    @Transactional
    public ReviewEmployeeResponseDTO create(CreateReviewEmployeeDTO dto, Long reviewerId) throws IdInvalidException {
        // Validation cho PROBATION
        if (dto.getEmployeeId() == null) {
            throw new IdInvalidException("Employee ID là bắt buộc cho đánh giá thử việc");
        }
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new IdInvalidException("Nhân viên không tồn tại"));

        ReviewEmployee review = new ReviewEmployee();
        review.setReviewerId(reviewerId);
        review.setEmployee(employee);

        // PROBATION - Đánh giá năng lực chuyên môn (4 tiêu chí)
        review.setOnTimeCompletionScore(dto.getOnTimeCompletionScore());
        review.setWorkEfficiencyScore(dto.getWorkEfficiencyScore());
        review.setProfessionalSkillScore(dto.getProfessionalSkillScore());
        review.setSelfLearningScore(dto.getSelfLearningScore());

        // PROBATION - Đánh giá tính cách và tác phong (4 tiêu chí)
        review.setWorkAttitudeScore(dto.getWorkAttitudeScore());
        review.setCommunicationSkillScore(dto.getCommunicationSkillScore());
        review.setHonestyResponsibilityScore(dto.getHonestyResponsibilityScore());
        review.setTeamIntegrationScore(dto.getTeamIntegrationScore());

        // Kết quả thử việc và nhận xét bổ sung
        review.setProbationResult(dto.getProbationResult());
        review.setAdditionalComments(dto.getAdditionalComments());

        ReviewEmployee saved = reviewEmployeeRepository.save(review);
        return convertToResponse(saved);
    }

    @Transactional
    public ReviewEmployeeResponseDTO update(Long id, UpdateReviewEmployeeDTO dto, Long reviewerId)
            throws IdInvalidException {
        ReviewEmployee review = reviewEmployeeRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        // Chỉ cho phép người tạo review mới được update
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IdInvalidException("Bạn không có quyền cập nhật đánh giá này");
        }

        // Update PROBATION fields - Đánh giá năng lực chuyên môn
        if (dto.getOnTimeCompletionScore() != null) {
            review.setOnTimeCompletionScore(dto.getOnTimeCompletionScore());
        }
        if (dto.getWorkEfficiencyScore() != null) {
            review.setWorkEfficiencyScore(dto.getWorkEfficiencyScore());
        }
        if (dto.getProfessionalSkillScore() != null) {
            review.setProfessionalSkillScore(dto.getProfessionalSkillScore());
        }
        if (dto.getSelfLearningScore() != null) {
            review.setSelfLearningScore(dto.getSelfLearningScore());
        }

        // Update PROBATION fields - Đánh giá tính cách và tác phong
        if (dto.getWorkAttitudeScore() != null) {
            review.setWorkAttitudeScore(dto.getWorkAttitudeScore());
        }
        if (dto.getCommunicationSkillScore() != null) {
            review.setCommunicationSkillScore(dto.getCommunicationSkillScore());
        }
        if (dto.getHonestyResponsibilityScore() != null) {
            review.setHonestyResponsibilityScore(dto.getHonestyResponsibilityScore());
        }
        if (dto.getTeamIntegrationScore() != null) {
            review.setTeamIntegrationScore(dto.getTeamIntegrationScore());
        }

        // Update kết quả thử việc
        if (dto.getProbationResult() != null) {
            review.setProbationResult(dto.getProbationResult());
        }
        if (dto.getAdditionalComments() != null) {
            review.setAdditionalComments(dto.getAdditionalComments());
        }

        ReviewEmployee saved = reviewEmployeeRepository.save(review);
        return convertToResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long reviewerId) throws IdInvalidException {
        ReviewEmployee review = reviewEmployeeRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        // Chỉ cho phép người tạo review mới được xóa
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IdInvalidException("Bạn không có quyền xóa đánh giá này");
        }

        reviewEmployeeRepository.deleteById(id);
    }

    private List<ReviewEmployeeResponseDTO> convertToResponseList(List<ReviewEmployee> reviews) {
        // Batch fetch reviewer names
        Set<Long> reviewerIds = reviews.stream()
                .map(ReviewEmployee::getReviewerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> idToName = Map.of();
        if (!reviewerIds.isEmpty()) {
            List<Employee> employees = employeeRepository.findAllById(reviewerIds);
            idToName = employees.stream()
                    .collect(Collectors.toMap(Employee::getId, Employee::getName));
        }

        final Map<Long, String> finalIdToName = idToName;
        return reviews.stream()
                .map(r -> convertToResponse(r, finalIdToName))
                .collect(Collectors.toList());
    }

    private ReviewEmployeeResponseDTO convertToResponse(ReviewEmployee review) {
        Set<Long> reviewerIds = Set.of(review.getReviewerId());
        List<Employee> employees = employeeRepository.findAllById(reviewerIds);
        Map<Long, String> idToName = employees.stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));
        return convertToResponse(review, idToName);
    }

    private ReviewEmployeeResponseDTO convertToResponse(ReviewEmployee review, Map<Long, String> idToName) {
        ReviewEmployeeResponseDTO dto = new ReviewEmployeeResponseDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getReviewerId());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());

        // Set employeeId
        if (review.getEmployee() != null) {
            dto.setEmployeeId(review.getEmployee().getId());
        }

        // PROBATION - Đánh giá năng lực chuyên môn (4 tiêu chí)
        dto.setOnTimeCompletionScore(review.getOnTimeCompletionScore());
        dto.setWorkEfficiencyScore(review.getWorkEfficiencyScore());
        dto.setProfessionalSkillScore(review.getProfessionalSkillScore());
        dto.setSelfLearningScore(review.getSelfLearningScore());

        // PROBATION - Đánh giá tính cách và tác phong (4 tiêu chí)
        dto.setWorkAttitudeScore(review.getWorkAttitudeScore());
        dto.setCommunicationSkillScore(review.getCommunicationSkillScore());
        dto.setHonestyResponsibilityScore(review.getHonestyResponsibilityScore());
        dto.setTeamIntegrationScore(review.getTeamIntegrationScore());

        // Tính điểm trung bình từ 8 tiêu chí
        int count = 0;
        double sum = 0.0;
        if (review.getOnTimeCompletionScore() != null) {
            sum += review.getOnTimeCompletionScore();
            count++;
        }
        if (review.getWorkEfficiencyScore() != null) {
            sum += review.getWorkEfficiencyScore();
            count++;
        }
        if (review.getProfessionalSkillScore() != null) {
            sum += review.getProfessionalSkillScore();
            count++;
        }
        if (review.getSelfLearningScore() != null) {
            sum += review.getSelfLearningScore();
            count++;
        }
        if (review.getWorkAttitudeScore() != null) {
            sum += review.getWorkAttitudeScore();
            count++;
        }
        if (review.getCommunicationSkillScore() != null) {
            sum += review.getCommunicationSkillScore();
            count++;
        }
        if (review.getHonestyResponsibilityScore() != null) {
            sum += review.getHonestyResponsibilityScore();
            count++;
        }
        if (review.getTeamIntegrationScore() != null) {
            sum += review.getTeamIntegrationScore();
            count++;
        }
        if (count > 0) {
            dto.setAverageScore(sum / count);
        }

        // Kết quả thử việc và nhận xét bổ sung
        dto.setProbationResult(review.getProbationResult());
        dto.setAdditionalComments(review.getAdditionalComments());

        // Set reviewer name
        if (idToName != null && review.getReviewerId() != null) {
            String reviewerName = idToName.get(review.getReviewerId());
            if (reviewerName != null) {
                dto.setReviewerName(reviewerName);
            }
        }

        return dto;
    }
}
