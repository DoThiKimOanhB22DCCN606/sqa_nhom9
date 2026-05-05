package com.example.candidate_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.example.candidate_service.dto.PaginationDTO;
import com.example.candidate_service.dto.review.CreateReviewCandidateDTO;
import com.example.candidate_service.dto.review.ReviewCandidateResponseDTO;
import com.example.candidate_service.dto.review.UpdateReviewCandidateDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.model.ReviewCandidate;
import com.example.candidate_service.repository.CandidateRepository;
import com.example.candidate_service.repository.ReviewCandidateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit test for ReviewCandidateService.
 * CheckDB: repository mocks are used as the DB boundary; state-changing tests assert saved/deleted mock state.
 * Rollback: no real DB transaction is opened, so Mockito resets test data between test methods.
 */
@ExtendWith(MockitoExtension.class)
class ReviewCandidateServiceTest {

    @Mock private ReviewCandidateRepository reviewCandidateRepository;
    @Mock private CandidateRepository candidateRepository;
    @Mock private UserService userService;
    @InjectMocks private ReviewCandidateService reviewCandidateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Candidate candidate(Long id) {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setName("Candidate A");
        return candidate;
    }

    private ReviewCandidate review(Long id, Long candidateId, Long reviewerId) {
        ReviewCandidate review = new ReviewCandidate();
        review.setId(id);
        review.setReviewerId(reviewerId);
        review.setCandidate(candidate(candidateId));
        review.setProfessionalSkillScore(4);
        review.setCommunicationSkillScore(5);
        review.setWorkExperienceScore(3);
        review.setStrengths("Good attitude");
        review.setWeaknesses("Needs domain knowledge");
        review.setConclusion(true);
        review.setCreatedAt(LocalDateTime.of(2026, 5, 3, 10, 0));
        review.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 11, 0));
        return review;
    }

    @Test
    // Test Case ID: TC-RCS-001 - CheckDB create success state.
    @DisplayName("TC-RCS-001 - create() saves review when candidate exists")
    void tc_rcs_001_create_whenCandidateExists_savesReview() throws IdInvalidException {
        CreateReviewCandidateDTO dto = new CreateReviewCandidateDTO();
        dto.setCandidateId(1L); dto.setProfessionalSkillScore(4); dto.setCommunicationSkillScore(4); dto.setWorkExperienceScore(4); dto.setStrengths("S"); dto.setWeaknesses("W"); dto.setConclusion(true);
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate(1L)));
        when(reviewCandidateRepository.save(any(ReviewCandidate.class))).thenAnswer(invocation -> { ReviewCandidate saved = invocation.getArgument(0); saved.setId(101L); return saved; });
        ReviewCandidateResponseDTO result = reviewCandidateService.create(dto, 1001L);
        assertEquals(101L, result.getId());
        assertEquals(1001L, result.getReviewerId());
        assertEquals(4.0, result.getAverageScore());
        verify(reviewCandidateRepository).save(argThat(saved -> saved.getCandidate().getId().equals(1L)));
    }

    @Test
    // Test Case ID: TC-RCS-002 - create validation fail when candidateId is null.
    @DisplayName("TC-RCS-002 - create() throws when candidateId is null")
    void tc_rcs_002_create_whenCandidateIdNull_throwsException() {
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.create(new CreateReviewCandidateDTO(), 1L));
    }

    @Test
    // Test Case ID: TC-RCS-003 - create validation fail when candidate does not exist.
    @DisplayName("TC-RCS-003 - create() throws when candidate is missing")
    void tc_rcs_003_create_whenCandidateMissing_throwsException() {
        CreateReviewCandidateDTO dto = new CreateReviewCandidateDTO(); dto.setCandidateId(3L);
        when(candidateRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.create(dto, 1L));
    }

    @Test
    // Test Case ID: TC-RCS-004 - create average false branch when one score is null.
    @DisplayName("TC-RCS-004 - create() leaves average null when any score is null")
    void tc_rcs_004_create_whenScoreMissing_averageIsNull() throws IdInvalidException {
        CreateReviewCandidateDTO dto = new CreateReviewCandidateDTO(); dto.setCandidateId(4L); dto.setProfessionalSkillScore(4); dto.setWorkExperienceScore(5);
        when(candidateRepository.findById(4L)).thenReturn(Optional.of(candidate(4L)));
        when(reviewCandidateRepository.save(any(ReviewCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCandidateResponseDTO result = reviewCandidateService.create(dto, 4L);

        assertNull(result.getAverageScore());
        ArgumentCaptor<ReviewCandidate> reviewCaptor = ArgumentCaptor.forClass(ReviewCandidate.class);
        verify(reviewCandidateRepository).save(reviewCaptor.capture());
        assertEquals(4L, reviewCaptor.getValue().getReviewerId());
        assertEquals(4, reviewCaptor.getValue().getProfessionalSkillScore());
        assertNull(reviewCaptor.getValue().getCommunicationSkillScore());
        assertEquals(5, reviewCaptor.getValue().getWorkExperienceScore());
    }

    @Test
    // Test Case ID: TC-RCS-005 - getById success with reviewer name enrichment.
    @DisplayName("TC-RCS-005 - getById() returns review with reviewer name when token exists")
    void tc_rcs_005_getById_whenTokenExists_returnsReviewerName() throws IdInvalidException {
        when(reviewCandidateRepository.findById(5L)).thenReturn(Optional.of(review(5L, 50L, 500L)));
        ObjectNode names = objectMapper.createObjectNode(); names.put("500", "Reviewer");
        when(userService.getEmployeeNames(List.of(500L), "token")).thenReturn(ResponseEntity.ok(names));
        assertEquals("Reviewer", reviewCandidateService.getById(5L, "token").getReviewerName());
    }

    @Test
    // Test Case ID: TC-RCS-006 - getById false branch skips enrichment.
    @DisplayName("TC-RCS-006 - getById() skips reviewer lookup when token is blank")
    void tc_rcs_006_getById_whenTokenBlank_skipsReviewerLookup() throws IdInvalidException {
        when(reviewCandidateRepository.findById(6L)).thenReturn(Optional.of(review(6L, 60L, 600L)));
        assertNull(reviewCandidateService.getById(6L, "").getReviewerName());
        verify(userService, never()).getEmployeeNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-RCS-007 - getById fail path.
    @DisplayName("TC-RCS-007 - getById() throws when review is missing")
    void tc_rcs_007_getById_whenMissing_throwsException() {
        when(reviewCandidateRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.getById(7L, "token"));
    }

    @Test
    // Test Case ID: TC-RCS-008 - list by candidate with batch reviewer names.
    @DisplayName("TC-RCS-008 - getByCandidateId() returns reviews with reviewer names")
    void tc_rcs_008_getByCandidateId_whenNamesExist_returnsNames() {
        when(reviewCandidateRepository.findByCandidate_Id(8L)).thenReturn(List.of(review(8L, 8L, 800L)));
        ObjectNode names = objectMapper.createObjectNode(); names.put("800", "Reviewer 800");
        when(userService.getEmployeeNames(List.of(800L), "token")).thenReturn(ResponseEntity.ok(names));
        assertEquals("Reviewer 800", reviewCandidateService.getByCandidateId(8L, "token").get(0).getReviewerName());
    }

    @Test
    // Test Case ID: TC-RCS-009 - user service failure branch keeps reviewerName null.
    @DisplayName("TC-RCS-009 - getByCandidateId() returns review without names when user service fails")
    void tc_rcs_009_getByCandidateId_whenNameServiceFails_returnsWithoutName() {
        when(reviewCandidateRepository.findByCandidate_Id(9L)).thenReturn(List.of(review(9L, 9L, 900L)));
        when(userService.getEmployeeNames(List.of(900L), "token")).thenReturn(ResponseEntity.status(500).build());
        assertNull(reviewCandidateService.getByCandidateId(9L, "token").get(0).getReviewerName());
    }

    @Test
    // Test Case ID: TC-RCS-010 - pagination normalization and default sort branches.
    @DisplayName("TC-RCS-010 - getAllWithFilters() normalizes invalid pagination")
    void tc_rcs_010_getAllWithFilters_whenPaginationInvalid_normalizesValues() {
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        when(reviewCandidateRepository.findByFilters(null, null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(review(10L, 10L, 1000L)), pageable, 1));
        PaginationDTO result = reviewCandidateService.getAllWithFilters(null, null, null, null, 0, 0, null, null, "");
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
    }

    @Test
    // Test Case ID: TC-RCS-011 - ascending sort branch.
    @DisplayName("TC-RCS-011 - getAllWithFilters() uses ascending sort when requested")
    void tc_rcs_011_getAllWithFilters_whenAscRequested_usesAscendingSort() {
        Pageable pageable = PageRequest.of(1, 5, org.springframework.data.domain.Sort.by("updatedAt").ascending());
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0); LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(reviewCandidateRepository.findByFilters(11L, 1100L, start, end, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertEquals(2, reviewCandidateService.getAllWithFilters(11L, 1100L, start, end, 2, 5, "updatedAt", "asc", null).getMeta().getPage());
    }

    @Test
    // Test Case ID: TC-RCS-012 - CheckDB update success and owner true branch.
    @DisplayName("TC-RCS-012 - update() updates every provided field when reviewer owns review")
    void tc_rcs_012_update_whenOwner_updatesState() throws IdInvalidException {
        ReviewCandidate existing = review(12L, 12L, 1200L); when(reviewCandidateRepository.findById(12L)).thenReturn(Optional.of(existing)); when(reviewCandidateRepository.save(existing)).thenReturn(existing);
        UpdateReviewCandidateDTO dto = new UpdateReviewCandidateDTO(); dto.setProfessionalSkillScore(1); dto.setCommunicationSkillScore(2); dto.setWorkExperienceScore(3); dto.setStrengths("S2"); dto.setWeaknesses("W2"); dto.setConclusion(false);
        ReviewCandidateResponseDTO result = reviewCandidateService.update(12L, dto, 1200L);
        assertEquals(2.0, result.getAverageScore());
        assertFalse(result.getConclusion());
        verify(reviewCandidateRepository).save(existing);
    }

    @Test
    // Test Case ID: TC-RCS-013 - null update branch preserves state.
    @DisplayName("TC-RCS-013 - update() preserves state when DTO fields are null")
    void tc_rcs_013_update_whenFieldsNull_preservesState() throws IdInvalidException {
        ReviewCandidate existing = review(13L, 13L, 1300L); when(reviewCandidateRepository.findById(13L)).thenReturn(Optional.of(existing)); when(reviewCandidateRepository.save(existing)).thenReturn(existing);

        ReviewCandidateResponseDTO result = reviewCandidateService.update(13L, new UpdateReviewCandidateDTO(), 1300L);

        assertEquals("Good attitude", result.getStrengths());
        ArgumentCaptor<ReviewCandidate> reviewCaptor = ArgumentCaptor.forClass(ReviewCandidate.class);
        verify(reviewCandidateRepository).save(reviewCaptor.capture());
        assertEquals("Good attitude", reviewCaptor.getValue().getStrengths());
        assertEquals("Needs structure", reviewCaptor.getValue().getWeaknesses());
    }

    @Test
    // Test Case ID: TC-RCS-014 - owner false branch for update.
    @DisplayName("TC-RCS-014 - update() throws when reviewer is not owner")
    void tc_rcs_014_update_whenNotOwner_throwsException() {
        when(reviewCandidateRepository.findById(14L)).thenReturn(Optional.of(review(14L, 14L, 1400L)));
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.update(14L, new UpdateReviewCandidateDTO(), 1L));
    }

    @Test
    // Test Case ID: TC-RCS-015 - update missing review fail path.
    @DisplayName("TC-RCS-015 - update() throws when review is missing")
    void tc_rcs_015_update_whenMissing_throwsException() {
        when(reviewCandidateRepository.findById(15L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.update(15L, new UpdateReviewCandidateDTO(), 1L));
    }

    @Test
    // Test Case ID: TC-RCS-016 - CheckDB delete success and owner true branch.
    @DisplayName("TC-RCS-016 - delete() deletes review when reviewer owns review")
    void tc_rcs_016_delete_whenOwner_deletesReview() throws IdInvalidException {
        when(reviewCandidateRepository.findById(16L)).thenReturn(Optional.of(review(16L, 16L, 1600L)));
        reviewCandidateService.delete(16L, 1600L);
        verify(reviewCandidateRepository).deleteById(16L);
    }

    @Test
    // Test Case ID: TC-RCS-017 - owner false branch for delete.
    @DisplayName("TC-RCS-017 - delete() throws when reviewer is not owner")
    void tc_rcs_017_delete_whenNotOwner_throwsException() {
        when(reviewCandidateRepository.findById(17L)).thenReturn(Optional.of(review(17L, 17L, 1700L)));
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.delete(17L, 1L));
        verify(reviewCandidateRepository, never()).deleteById(anyLong());
    }

    @Test
    // Test Case ID: TC-RCS-018 - delete missing review fail path.
    @DisplayName("TC-RCS-018 - delete() throws when review is missing")
    void tc_rcs_018_delete_whenMissing_throwsException() {
        when(reviewCandidateRepository.findById(18L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> reviewCandidateService.delete(18L, 1L));
    }

    @Test
    // Test Case ID: TC-RCS-019 - EXPECTED FAIL/Bug exposed: user service timeout currently crashes review detail.
    @DisplayName("TC-RCS-019 - EXPECTED FAIL - getById() should return review when user service throws")
    void tc_rcs_019_getById_whenUserServiceThrows_shouldReturnReviewWithoutReviewerName() {
        ReviewCandidate existing = review(19L, 19L, 1900L);
        when(reviewCandidateRepository.findById(19L)).thenReturn(Optional.of(existing));
        when(userService.getEmployeeNames(List.of(1900L), "token")).thenThrow(new RuntimeException("user-service timeout"));

        ReviewCandidateResponseDTO result = assertDoesNotThrow(() -> reviewCandidateService.getById(19L, "token"));

        assertEquals(19L, result.getId());
        assertNull(result.getReviewerName());
    }

    @Test
    // Test Case ID: TC-RCS-020 - EXPECTED FAIL/Bug exposed: service currently accepts score outside allowed range.
    @DisplayName("TC-RCS-020 - EXPECTED FAIL - create() should reject score outside allowed range")
    void tc_rcs_020_create_whenScoreOutOfRange_shouldThrowAndNotSave() {
        CreateReviewCandidateDTO request = new CreateReviewCandidateDTO();
        request.setCandidateId(20L);
        request.setProfessionalSkillScore(6);
        request.setCommunicationSkillScore(4);
        request.setWorkExperienceScore(4);
        when(candidateRepository.findById(20L)).thenReturn(Optional.of(candidate(20L)));
        lenient().when(reviewCandidateRepository.save(any(ReviewCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> reviewCandidateService.create(request, 2000L));
        verify(reviewCandidateRepository, never()).save(any(ReviewCandidate.class));
    }

    @Test
    // Test Case ID: TC-RCS-021 - EXPECTED FAIL/Bug exposed: service currently does not clear averageScore when one score is null.
    @DisplayName("TC-RCS-021 - EXPECTED FAIL - update() should clear averageScore when one score is set to null")
    void tc_rcs_021_update_whenOneScoreIsNull_shouldClearScoreAndAverage() throws IdInvalidException {
        ReviewCandidate existing = review(21L, 21L, 2100L);
        UpdateReviewCandidateDTO request = new UpdateReviewCandidateDTO();
        request.setProfessionalSkillScore(null);
        when(reviewCandidateRepository.findById(21L)).thenReturn(Optional.of(existing));
        when(reviewCandidateRepository.save(existing)).thenReturn(existing);

        ReviewCandidateResponseDTO result = reviewCandidateService.update(21L, request, 2100L);

        verify(reviewCandidateRepository).save(existing);
        assertNull(result.getProfessionalSkillScore());
        assertNull(result.getAverageScore());
    }


}
