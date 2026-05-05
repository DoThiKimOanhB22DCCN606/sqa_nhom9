package com.example.candidate_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import com.example.candidate_service.dto.PaginationDTO;
import com.example.candidate_service.dto.candidate.CandidateDetailResponseDTO;
import com.example.candidate_service.dto.candidate.CandidateGetAllResponseDTO;
import com.example.candidate_service.dto.candidate.CreateCandidateDTO;
import com.example.candidate_service.dto.candidate.UpdateCandidateDTO;
import com.example.candidate_service.dto.candidate.UploadCVDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.model.Comment;
import com.example.candidate_service.repository.CandidateRepository;
import com.example.candidate_service.utils.enums.CandidateStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit test for CandidateService.
 * CheckDB: repository mock is the DB boundary; every save/update/delete test verifies the saved entity state and repository interaction.
 * Rollback: no real database is touched, so each Mockito test starts from clean in-memory mocks and leaves no persisted data.
 */
@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock private CandidateRepository candidateRepository;
    @Mock private JobService jobService;
    @Mock private ScheduleService communicationService;
    @Mock private CommentService commentService;
    @Mock private ReviewCandidateService reviewCandidateService;
    @Mock private UserService userService;
    @InjectMocks private CandidateService candidateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Candidate candidate(Long id, Long jobPositionId) {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setName("Nguyen Van A");
        candidate.setEmail("a@example.com");
        candidate.setPhone("0123456789");
        candidate.setAppliedDate(LocalDate.of(2026, 5, 3));
        candidate.setStatus(CandidateStatus.SUBMITTED);
        candidate.setJobPositionId(jobPositionId);
        return candidate;
    }

    private ObjectNode jobNode(String title, long departmentId) {
        ObjectNode job = objectMapper.createObjectNode();
        job.put("title", title);
        job.set("recruitmentRequest", objectMapper.createObjectNode().put("departmentId", departmentId));
        return job;
    }

    @Test
    // Test Case ID: TC-CS-001 - true branch for email existence lookup.
    @DisplayName("TC-CS-001 - existsByEmail() returns true when candidate exists")
    void tc_cs_001_existsByEmail_whenCandidateExists_returnsTrue() {
        when(candidateRepository.existsByEmail("a@example.com")).thenReturn(true);
        assertTrue(candidateService.existsByEmail("a@example.com"));
        verify(candidateRepository).existsByEmail("a@example.com");
    }

    @Test
    // Test Case ID: TC-CS-002 - false branch for email existence lookup.
    @DisplayName("TC-CS-002 - existsByEmail() returns false when candidate does not exist")
    void tc_cs_002_existsByEmail_whenCandidateMissing_returnsFalse() {
        when(candidateRepository.existsByEmail("missing@example.com")).thenReturn(false);
        assertFalse(candidateService.existsByEmail("missing@example.com"));
    }

    @Test
    // Test Case ID: TC-CS-003 - success path for findByEmail.
    @DisplayName("TC-CS-003 - findByEmail() returns candidate when email exists")
    void tc_cs_003_findByEmail_whenEmailExists_returnsCandidate() throws IdInvalidException {
        Candidate existingCandidate = candidate(3L, 30L);
        when(candidateRepository.findByEmail("a@example.com")).thenReturn(Optional.of(existingCandidate));
        assertSame(existingCandidate, candidateService.findByEmail("a@example.com"));
    }

    @Test
    // Test Case ID: TC-CS-004 - fail path for findByEmail.
    @DisplayName("TC-CS-004 - findByEmail() throws when email is missing")
    void tc_cs_004_findByEmail_whenEmailMissing_throwsException() {
        when(candidateRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> candidateService.findByEmail("missing@example.com"));
    }

    @Test
    // Test Case ID: TC-CS-005 - CheckDB save state for direct saveCandidate.
    @DisplayName("TC-CS-005 - saveCandidate() saves and returns repository entity")
    void tc_cs_005_saveCandidate_savesAndReturnsEntity() {
        Candidate newCandidate = candidate(null, 50L);
        when(candidateRepository.save(newCandidate)).thenAnswer(invocation -> { newCandidate.setId(5L); return newCandidate; });
        Candidate result = candidateService.saveCandidate(newCandidate);
        assertEquals(5L, result.getId());
        verify(candidateRepository).save(newCandidate);
    }

    @Test
    // Test Case ID: TC-CS-006 - repository read delegation for count.
    @DisplayName("TC-CS-006 - countCandidatesByJobPositionId() returns repository count")
    void tc_cs_006_countCandidatesByJobPositionId_returnsCount() {
        when(candidateRepository.countByJobPositionId(60L)).thenReturn(2L);
        assertEquals(2L, candidateService.countCandidatesByJobPositionId(60L));
    }

    @Test
    // Test Case ID: TC-CS-007 - department filter true branch with no jobs.
    @DisplayName("TC-CS-007 - getAllWithFilters() returns empty page when department has no jobs")
    void tc_cs_007_getAllWithFilters_whenDepartmentHasNoJobs_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobService.getJobPositionsByDepartmentId(7L, "token")).thenReturn(Map.of());
        PaginationDTO result = candidateService.getAllWithFilters(null, null, null, null, null, null, 7L, pageable, "token");
        assertEquals(0, result.getMeta().getTotal());
        assertTrue(((List<?>) result.getResult()).isEmpty());
        verify(candidateRepository, never()).findByFilters(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    // Test Case ID: TC-CS-008 - filter parsing and enrichment branch.
    @DisplayName("TC-CS-008 - getAllWithFilters() enriches job title and department when job data exists")
    void tc_cs_008_getAllWithFilters_whenJobDataExists_enrichesRows() {
        Candidate row = candidate(8L, 80L);
        Pageable pageable = PageRequest.of(0, 10);
        when(candidateRepository.findByFilters(null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 1));
        when(jobService.getJobPositionsByIdsSimple(List.of(80L), "token")).thenReturn(Map.of(80L, jobNode("Java Dev", 800L)));
        PaginationDTO result = candidateService.getAllWithFilters(null, null, null, "2026-01-01", "2026-01-31", null, null, pageable, "token");
        CandidateGetAllResponseDTO dto = ((List<CandidateGetAllResponseDTO>) result.getResult()).get(0);
        assertEquals("Java Dev", dto.getJobPositionTitle());
        assertEquals(800L, dto.getDepartmentId());
    }

    @Test
    // Test Case ID: TC-CS-009 - token false branch skips enrichment.
    @DisplayName("TC-CS-009 - getAllWithFilters() skips enrichment when token is blank")
    void tc_cs_009_getAllWithFilters_whenTokenBlank_skipsEnrichment() {
        Candidate row = candidate(9L, 90L);
        Pageable pageable = PageRequest.of(0, 10);
        when(candidateRepository.findByFilters(null, null, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 1));
        PaginationDTO result = candidateService.getAllWithFilters(null, null, null, "bad", "", null, null, pageable, "");
        CandidateGetAllResponseDTO dto = ((List<CandidateGetAllResponseDTO>) result.getResult()).get(0);
        assertNull(dto.getJobPositionTitle());
        verify(jobService, never()).getJobPositionsByIdsSimple(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-CS-010 - success path for getById.
    @DisplayName("TC-CS-010 - getById() returns detail DTO when candidate exists")
    void tc_cs_010_getById_whenFound_returnsDetail() throws IdInvalidException {
        when(candidateRepository.findById(10L)).thenReturn(Optional.of(candidate(10L, 100L)));
        assertEquals(10L, candidateService.getById(10L).getId());
    }

    @Test
    // Test Case ID: TC-CS-011 - fail path for getById.
    @DisplayName("TC-CS-011 - getById() throws when candidate is missing")
    void tc_cs_011_getById_whenMissing_throwsException() {
        when(candidateRepository.findById(11L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> candidateService.getById(11L));
    }

    @Test
    // Test Case ID: TC-CS-012 - CheckDB create state with createdBy branch true.
    @DisplayName("TC-CS-012 - create() saves candidate with submitted status and createdBy")
    void tc_cs_012_create_whenValid_savesExpectedState() throws IdInvalidException {
        CreateCandidateDTO dto = new CreateCandidateDTO();
        dto.setName("New Candidate"); dto.setEmail("new@example.com"); dto.setPhone("090"); dto.setJobPositionId(12L); dto.setCvUrl("cv.pdf"); dto.setNotes("note"); dto.setCreatedBy(1200L);
        when(candidateRepository.existsByEmailAndJobPositionId("new@example.com", 12L)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> { Candidate saved = invocation.getArgument(0); saved.setId(12L); return saved; });
        Candidate result = candidateService.create(dto);
        assertEquals(CandidateStatus.SUBMITTED, result.getStatus());
        assertEquals(1200L, result.getCreatedBy());
        verify(candidateRepository).save(argThat(saved -> "new@example.com".equals(saved.getEmail()) && "cv.pdf".equals(saved.getResumeUrl())));
    }

    @Test
    // Test Case ID: TC-CS-013 - fail path for duplicate create.
    @DisplayName("TC-CS-013 - create() throws when candidate already applied to same job")
    void tc_cs_013_create_whenDuplicate_throwsException() {
        CreateCandidateDTO dto = new CreateCandidateDTO(); dto.setEmail("dup@example.com"); dto.setJobPositionId(13L);
        when(candidateRepository.existsByEmailAndJobPositionId("dup@example.com", 13L)).thenReturn(true);
        assertThrows(IdInvalidException.class, () -> candidateService.create(dto));
        verify(candidateRepository, never()).save(any());
    }

    @Test
    // Test Case ID: TC-CS-014 - CheckDB update state for non-null fields.
    @DisplayName("TC-CS-014 - update() updates provided profile fields")
    void tc_cs_014_update_whenFieldsProvided_updatesState() throws IdInvalidException {
        Candidate existing = candidate(14L, 140L);
        when(candidateRepository.findById(14L)).thenReturn(Optional.of(existing));
        when(candidateRepository.save(existing)).thenReturn(existing);
        UpdateCandidateDTO dto = new UpdateCandidateDTO();
        dto.setName("Updated"); dto.setDateOfBirth("1999-01-01"); dto.setGpa(BigDecimal.valueOf(3.5)); dto.setNotes("new note");
        CandidateDetailResponseDTO result = candidateService.update(14L, dto);
        assertEquals("Updated", result.getName());
        assertEquals(LocalDate.of(1999, 1, 1), existing.getDateOfBirth());
        assertEquals(BigDecimal.valueOf(3.5), existing.getGpa());
        verify(candidateRepository).save(existing);
    }

    @Test
    // Test Case ID: TC-CS-015 - null branch preserves existing update values.
    @DisplayName("TC-CS-015 - update() preserves existing state when DTO fields are null")
    void tc_cs_015_update_whenFieldsNull_preservesState() throws IdInvalidException {
        Candidate existing = candidate(15L, 150L); existing.setName("Original");
        when(candidateRepository.findById(15L)).thenReturn(Optional.of(existing)); when(candidateRepository.save(existing)).thenReturn(existing);

        CandidateDetailResponseDTO result = candidateService.update(15L, new UpdateCandidateDTO());

        assertEquals("Original", result.getName());
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertEquals("Original", candidateCaptor.getValue().getName());
    }

    @Test
    // Test Case ID: TC-CS-016 - fail path for update missing candidate.
    @DisplayName("TC-CS-016 - update() throws when candidate is missing")
    void tc_cs_016_update_whenMissing_throwsException() {
        when(candidateRepository.findById(16L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> candidateService.update(16L, new UpdateCandidateDTO()));
    }

    @Test
    // Test Case ID: TC-CS-017 - CheckDB delete interaction.
    @DisplayName("TC-CS-017 - delete() deletes existing candidate")
    void tc_cs_017_delete_whenFound_deletesCandidate() throws IdInvalidException {
        Candidate existing = candidate(17L, 170L); when(candidateRepository.findById(17L)).thenReturn(Optional.of(existing));
        candidateService.delete(17L);
        verify(candidateRepository).delete(existing);
    }

    @Test
    // Test Case ID: TC-CS-018 - fail path for delete missing candidate.
    @DisplayName("TC-CS-018 - delete() throws when candidate is missing")
    void tc_cs_018_delete_whenMissing_throwsException() {
        when(candidateRepository.findById(18L)).thenReturn(Optional.empty());
        assertThrows(IdInvalidException.class, () -> candidateService.delete(18L));
    }

    @Test
    // Test Case ID: TC-CS-019 - batch read path.
    @DisplayName("TC-CS-019 - getByIds() returns detail DTOs for ids")
    void tc_cs_019_getByIds_returnsDetails() {
        when(candidateRepository.findAllById(List.of(19L, 20L))).thenReturn(List.of(candidate(19L, 1L), candidate(20L, 1L)));
        assertEquals(2, candidateService.getByIds(List.of(19L, 20L)).size());
    }

    @Test
    // Test Case ID: TC-CS-020 - CheckDB create application with optional notes true branch.
    @DisplayName("TC-CS-020 - createCandidateFromApplication() saves application with notes")
    void tc_cs_020_createCandidateFromApplication_whenNotesProvided_savesCandidate() throws Exception {
        UploadCVDTO dto = new UploadCVDTO(); dto.setName("Applicant"); dto.setEmail("app@example.com"); dto.setPhone("091"); dto.setJobPositionId(20L); dto.setCvUrl("app.pdf"); dto.setNotes("note");
        when(candidateRepository.existsByEmailAndJobPositionId("app@example.com", 20L)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Candidate result = candidateService.createCandidateFromApplication(dto);
        assertEquals("note", result.getNotes());
        assertEquals(CandidateStatus.SUBMITTED, result.getStatus());
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertEquals("app@example.com", candidateCaptor.getValue().getEmail());
        assertEquals("app.pdf", candidateCaptor.getValue().getResumeUrl());
        assertEquals("note", candidateCaptor.getValue().getNotes());
        assertEquals(CandidateStatus.SUBMITTED, candidateCaptor.getValue().getStatus());
    }

    @Test
    // Test Case ID: TC-CS-021 - optional notes false branch.
    @DisplayName("TC-CS-021 - createCandidateFromApplication() saves application without notes")
    void tc_cs_021_createCandidateFromApplication_whenNotesNull_keepsNotesNull() throws Exception {
        UploadCVDTO dto = new UploadCVDTO(); dto.setName("Applicant"); dto.setEmail("app2@example.com"); dto.setPhone("092"); dto.setJobPositionId(21L); dto.setCvUrl("app2.pdf");
        when(candidateRepository.existsByEmailAndJobPositionId("app2@example.com", 21L)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertNull(candidateService.createCandidateFromApplication(dto).getNotes());
    }

    @Test
    // Test Case ID: TC-CS-022 - duplicate application fail path.
    @DisplayName("TC-CS-022 - createCandidateFromApplication() throws for duplicate application")
    void tc_cs_022_createCandidateFromApplication_whenDuplicate_throwsException() {
        UploadCVDTO dto = new UploadCVDTO(); dto.setEmail("dup@example.com"); dto.setJobPositionId(22L);
        when(candidateRepository.existsByEmailAndJobPositionId("dup@example.com", 22L)).thenReturn(true);
        assertThrows(IdInvalidException.class, () -> candidateService.createCandidateFromApplication(dto));
    }

    @Test
    // Test Case ID: TC-CS-023 - CheckDB rejected branch sets feedback.
    @DisplayName("TC-CS-023 - updateCandidateStatus() stores rejection reason for REJECTED")
    void tc_cs_023_updateCandidateStatus_whenRejected_setsReason() throws IdInvalidException {
        Candidate existing = candidate(23L, 230L); when(candidateRepository.findById(23L)).thenReturn(Optional.of(existing)); when(candidateRepository.save(existing)).thenReturn(existing);
        CandidateDetailResponseDTO result = candidateService.updateCandidateStatus(23L, "REJECTED", "not fit", 2300L);
        assertEquals(CandidateStatus.REJECTED, result.getStatus());
        assertEquals("not fit", existing.getRejectionReason());
        assertEquals(2300L, existing.getUpdatedBy());
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertEquals(CandidateStatus.REJECTED, candidateCaptor.getValue().getStatus());
        assertEquals("not fit", candidateCaptor.getValue().getRejectionReason());
        assertEquals(2300L, candidateCaptor.getValue().getUpdatedBy());
    }

    @Test
    // Test Case ID: TC-CS-024 - non-rejected branch does not set feedback.
    @DisplayName("TC-CS-024 - updateCandidateStatus() does not store reason for non-rejected status")
    void tc_cs_024_updateCandidateStatus_whenNotRejected_doesNotSetReason() throws IdInvalidException {
        Candidate existing = candidate(24L, 240L); when(candidateRepository.findById(24L)).thenReturn(Optional.of(existing)); when(candidateRepository.save(existing)).thenReturn(existing);
        candidateService.updateCandidateStatus(24L, "INTERVIEW", "feedback", 2400L);
        assertNull(existing.getRejectionReason());
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertEquals(CandidateStatus.INTERVIEW, candidateCaptor.getValue().getStatus());
        assertNull(candidateCaptor.getValue().getRejectionReason());
        assertEquals(2400L, candidateCaptor.getValue().getUpdatedBy());
    }

    @Test
    // Test Case ID: TC-CS-025 - invalid status fail path.
    @DisplayName("TC-CS-025 - updateCandidateStatus() throws when status value is invalid")
    void tc_cs_025_updateCandidateStatus_whenInvalidStatus_throwsException() {
        when(candidateRepository.findById(25L)).thenReturn(Optional.of(candidate(25L, 250L)));
        assertThrows(IllegalArgumentException.class, () -> candidateService.updateCandidateStatus(25L, "BAD", null, 1L));
    }

    @Test
    // Test Case ID: TC-CS-026 - detail enrichment success path.
    @DisplayName("TC-CS-026 - getCandidateDetailById() enriches comments reviews job and schedules")
    void tc_cs_026_getCandidateDetailById_whenDependenciesSucceed_enrichesDetail() throws Exception {
        Candidate existing = candidate(26L, 260L); existing.setComments(new HashSet<>(List.of(new Comment())));
        when(candidateRepository.findById(26L)).thenReturn(Optional.of(existing));
        when(commentService.getByCandidateId(26L, "token")).thenReturn(new ArrayList<>());
        when(reviewCandidateService.getByCandidateId(26L, "token")).thenReturn(new ArrayList<>());
        when(jobService.getJobPositionById(260L, "token")).thenReturn(ResponseEntity.ok(jobNode("Dev", 2600L)));
        ArrayNode schedules = objectMapper.createArrayNode(); schedules.addObject().put("id", 1L);
        when(communicationService.getUpcomingSchedulesForCandidate(26L, "token")).thenReturn(ResponseEntity.ok(schedules));
        CandidateDetailResponseDTO result = candidateService.getCandidateDetailById(26L, "token");
        assertTrue(result.getJobPosition().toString().contains("Dev"));
        assertEquals(1, result.getUpcomingSchedules().size());
        verify(candidateRepository).findById(26L);
        verify(jobService).getJobPositionById(260L, "token");
    }

    @Test
    // Test Case ID: TC-CS-027 - review failure branch recovers with empty reviews.
    @DisplayName("TC-CS-027 - getCandidateDetailById() uses empty reviews when review service fails")
    void tc_cs_027_getCandidateDetailById_whenReviewFails_recovers() throws Exception {
        Candidate existing = candidate(27L, null); when(candidateRepository.findById(27L)).thenReturn(Optional.of(existing));
        when(reviewCandidateService.getByCandidateId(27L, "token")).thenThrow(new RuntimeException("down"));
        ObjectNode body = objectMapper.createObjectNode(); ArrayNode data = objectMapper.createArrayNode(); data.addObject().put("id", 1L); body.set("data", data);
        when(communicationService.getUpcomingSchedulesForCandidate(27L, "token")).thenReturn(ResponseEntity.ok(body));
        CandidateDetailResponseDTO result = candidateService.getCandidateDetailById(27L, "token");
        assertTrue(result.getReviews().isEmpty());
        assertEquals(1, result.getUpcomingSchedules().size());
    }

    @Test
    // Test Case ID: TC-CS-028 - department lookup success branch.
    @DisplayName("TC-CS-028 - getDepartmentIdByCandidateId() returns department id from job service")
    void tc_cs_028_getDepartmentIdByCandidateId_whenFound_returnsDepartment() {
        when(candidateRepository.findById(28L)).thenReturn(Optional.of(candidate(28L, 280L)));
        when(jobService.getJobPositionByIdSimple(280L, "token")).thenReturn(ResponseEntity.ok(jobNode("Dev", 2800L)));
        assertEquals(2800L, candidateService.getDepartmentIdByCandidateId(28L, "token"));
    }

    @Test
    // Test Case ID: TC-CS-029 - department lookup false branch.
    @DisplayName("TC-CS-029 - getDepartmentIdByCandidateId() returns null when candidate or job is missing")
    void tc_cs_029_getDepartmentIdByCandidateId_whenMissing_returnsNull() {
        when(candidateRepository.findById(29L)).thenReturn(Optional.empty());
        assertNull(candidateService.getDepartmentIdByCandidateId(29L, "token"));
    }

    @Test
    // Test Case ID: TC-CS-030 - interviewer candidate lookup empty branch.
    @DisplayName("TC-CS-030 - getCandidatesByInterviewer() returns empty list when no candidate ids")
    void tc_cs_030_getCandidatesByInterviewer_whenNoIds_returnsEmptyList() {
        when(communicationService.getCandidateIdsByInterviewer(30L, "token")).thenReturn(List.of());
        assertTrue(candidateService.getCandidatesByInterviewer(30L, "token").isEmpty());
    }

    @Test
    // Test Case ID: TC-CS-031 - interviewer candidate lookup enrichment branch.
    @DisplayName("TC-CS-031 - getCandidatesByInterviewer() enriches job data when token exists")
    void tc_cs_031_getCandidatesByInterviewer_whenJobDataExists_enrichesCandidates() {
        when(communicationService.getCandidateIdsByInterviewer(31L, "token")).thenReturn(List.of(310L));
        when(candidateRepository.findAllById(List.of(310L))).thenReturn(List.of(candidate(310L, 311L)));
        when(jobService.getJobPositionsByIdsSimple(List.of(311L), "token")).thenReturn(Map.of(311L, jobNode("QA", 31L)));
        CandidateGetAllResponseDTO result = candidateService.getCandidatesByInterviewer(31L, "token").get(0);
        assertEquals("QA", result.getJobPositionTitle());
        assertEquals(31L, result.getDepartmentId());
    }

    @Test
    // Test Case ID: TC-CS-032 - conversion success path using provided ids.
    @DisplayName("TC-CS-032 - convertCandidateToEmployee() creates employee when required ids are provided")
    void tc_cs_032_convertCandidateToEmployee_whenIdsProvided_returnsBody() throws IdInvalidException {
        Candidate existing = candidate(32L, 320L); existing.setDateOfBirth(LocalDate.of(2000, 1, 1));
        when(candidateRepository.findById(32L)).thenReturn(Optional.of(existing));
        when(userService.createEmployeeFromCandidate(eq(32L), any(), any(), any(), eq("2000-01-01"), any(), any(), any(), any(), any(), eq(1L), eq(2L), eq("PROBATION"), eq("token")))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("employeeId", 32L)));
        assertEquals(32L, candidateService.convertCandidateToEmployee(32L, 1L, 2L, "token").get("employeeId").asLong());
    }

    @Test
    // Test Case ID: TC-CS-033 - conversion derives department from job service.
    @DisplayName("TC-CS-033 - convertCandidateToEmployee() derives department id when missing")
    void tc_cs_033_convertCandidateToEmployee_whenDepartmentMissing_derivesFromJob() throws IdInvalidException {
        when(candidateRepository.findById(33L)).thenReturn(Optional.of(candidate(33L, 330L)));
        when(jobService.getJobPositionById(330L, "token")).thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().set("data", objectMapper.createObjectNode().put("departmentId", 33L))));
        when(userService.createEmployeeFromCandidate(eq(33L), any(), any(), any(), isNull(), any(), any(), any(), any(), any(), eq(33L), eq(2L), eq("PROBATION"), eq("token")))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("ok", true)));
        assertTrue(candidateService.convertCandidateToEmployee(33L, null, 2L, "token").get("ok").asBoolean());
    }

    @Test
    // Test Case ID: TC-CS-034 - conversion fail paths for required ids.
    @DisplayName("TC-CS-034 - convertCandidateToEmployee() throws when required ids cannot be resolved")
    void tc_cs_034_convertCandidateToEmployee_whenRequiredIdsMissing_throwsException() {
        when(candidateRepository.findById(34L)).thenReturn(Optional.of(candidate(34L, null)));
        assertThrows(IdInvalidException.class, () -> candidateService.convertCandidateToEmployee(34L, null, 2L, "token"));
        assertThrows(IdInvalidException.class, () -> candidateService.convertCandidateToEmployee(34L, 1L, null, "token"));
    }

    @Test
    // Test Case ID: TC-CS-035 - conversion fail path from user service.
    @DisplayName("TC-CS-035 - convertCandidateToEmployee() throws when user service rejects creation")
    void tc_cs_035_convertCandidateToEmployee_whenUserServiceFails_throwsException() {
        when(candidateRepository.findById(35L)).thenReturn(Optional.of(candidate(35L, null)));
        when(userService.createEmployeeFromCandidate(eq(35L), any(), any(), any(), isNull(), any(), any(), any(), any(), any(), eq(1L), eq(2L), eq("PROBATION"), eq("token")))
                .thenReturn(ResponseEntity.badRequest().body(objectMapper.createObjectNode().put("message", "bad")));
        assertThrows(IdInvalidException.class, () -> candidateService.convertCandidateToEmployee(35L, 1L, 2L, "token"));
    }

    @Test
    // Test Case ID: TC-CS-036 - statistics without department filter.
    @DisplayName("TC-CS-036 - getCandidatesForStatistics() returns candidate statistics with department")
    void tc_cs_036_getCandidatesForStatistics_withoutDepartmentFilter_returnsStats() {
        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "appliedDate"));
        when(candidateRepository.findByFilters(null, CandidateStatus.SUBMITTED, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(candidate(36L, 360L)), pageable, 1));
        when(jobService.getJobPositionsByIds(List.of(360L), "token")).thenReturn(Map.of(360L, jobNode("Dev", 36L)));
        assertEquals(36L, candidateService.getCandidatesForStatistics(CandidateStatus.SUBMITTED, "bad", "", null, null, "token").get(0).getDepartmentId());
    }

    @Test
    // Test Case ID: TC-CS-037 - statistics department filter true branch.
    @DisplayName("TC-CS-037 - getCandidatesForStatistics() filters by department id")
    void tc_cs_037_getCandidatesForStatistics_withDepartmentFilter_returnsOnlyMatchingDepartment() {
        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "appliedDate"));
        when(candidateRepository.findByFilters(null, null, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(candidate(37L, 370L), candidate(38L, 380L)), pageable, 2));
        when(jobService.getJobPositionsByIds(anyList(), eq("token"))).thenReturn(Map.of(370L, jobNode("A", 37L), 380L, jobNode("B", 99L)));
        assertEquals(1, candidateService.getCandidatesForStatistics(null, null, null, null, 37L, "token").size());
    }

    @Test
    // Test Case ID: TC-CS-038 - EXPECTED FAIL/Bug exposed: service currently accepts candidate with null email.
    @DisplayName("TC-CS-038 - EXPECTED FAIL - create() should reject candidate when email is null")
    void tc_cs_038_create_whenEmailIsNull_shouldThrowAndNotSave() {
        CreateCandidateDTO request = new CreateCandidateDTO();
        request.setName("Missing Email");
        request.setEmail(null);
        request.setPhone("0900000000");
        request.setJobPositionId(10L);
        request.setCvUrl("cv.pdf");

        assertThrows(IdInvalidException.class, () -> candidateService.create(request));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    // Test Case ID: TC-CS-039 - EXPECTED FAIL/Bug exposed: service currently does not clear notes to null.
    @DisplayName("TC-CS-039 - EXPECTED FAIL - update() should allow clearing notes to null")
    void tc_cs_039_update_whenNotesIsNull_shouldClearExistingNotes() throws IdInvalidException {
        Candidate existingCandidate = candidate(39L, 390L);
        existingCandidate.setNotes("old notes");
        UpdateCandidateDTO request = new UpdateCandidateDTO();
        request.setNotes(null);
        when(candidateRepository.findById(39L)).thenReturn(Optional.of(existingCandidate));
        when(candidateRepository.save(existingCandidate)).thenReturn(existingCandidate);

        candidateService.update(39L, request);

        verify(candidateRepository).save(existingCandidate);
        assertEquals(null, existingCandidate.getNotes());
    }

    @Test
    // Test Case ID: TC-CS-040 - DB failure path for direct saveCandidate.
    @DisplayName("TC-CS-040 - saveCandidate() propagates repository exception when DB save fails")
    void tc_cs_040_saveCandidate_whenRepositoryThrows_shouldPropagateException() {
        Candidate newCandidate = candidate(null, 400L);
        when(candidateRepository.save(newCandidate)).thenThrow(new RuntimeException("DB connection lost"));

        assertThrows(RuntimeException.class, () -> candidateService.saveCandidate(newCandidate));
        verify(candidateRepository).save(newCandidate);
    }

    @Test
    // Test Case ID: TC-CS-041 - repository read delegation for zero count.
    @DisplayName("TC-CS-041 - countCandidatesByJobPositionId() returns zero when job has no candidates")
    void tc_cs_041_countCandidatesByJobPositionId_whenNoCandidate_returnsZero() {
        when(candidateRepository.countByJobPositionId(410L)).thenReturn(0L);

        assertEquals(0L, candidateService.countCandidatesByJobPositionId(410L));
    }

    @Test
    // Test Case ID: TC-CS-042 - EXPECTED FAIL/Bug exposed: service currently does not block duplicate email update.
    @DisplayName("TC-CS-042 - EXPECTED FAIL - update() should reject email already used by another candidate")
    void tc_cs_042_update_whenEmailBelongsToAnotherCandidate_shouldThrowAndNotSave() {
        Candidate existingCandidate = candidate(42L, 420L);
        UpdateCandidateDTO request = new UpdateCandidateDTO();
        request.setEmail("someone_else@example.com");
        when(candidateRepository.findById(42L)).thenReturn(Optional.of(existingCandidate));
        lenient().when(candidateRepository.save(existingCandidate)).thenReturn(existingCandidate);

        assertThrows(IdInvalidException.class, () -> candidateService.update(42L, request));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    // Test Case ID: TC-CS-043 - EXPECTED FAIL/Bug exposed: service currently treats empty dateOfBirth as invalid format.
    @DisplayName("TC-CS-043 - EXPECTED FAIL - update() should clear dateOfBirth when request date is empty")
    void tc_cs_043_update_whenDateOfBirthIsEmpty_shouldClearDateWithoutFormatError() {
        Candidate existingCandidate = candidate(43L, 430L);
        existingCandidate.setDateOfBirth(LocalDate.of(2000, 1, 1));
        UpdateCandidateDTO request = new UpdateCandidateDTO();
        request.setDateOfBirth("");
        when(candidateRepository.findById(43L)).thenReturn(Optional.of(existingCandidate));
        lenient().when(candidateRepository.save(existingCandidate)).thenReturn(existingCandidate);

        assertDoesNotThrow(() -> candidateService.update(43L, request));
        verify(candidateRepository).save(existingCandidate);
        assertNull(existingCandidate.getDateOfBirth());
    }

    @Test
    // Test Case ID: TC-CS-044 - empty id list should return empty result.
    @DisplayName("TC-CS-044 - getByIds() returns empty list when ids are empty")
    void tc_cs_044_getByIds_whenIdsEmpty_returnsEmptyList() {
        when(candidateRepository.findAllById(List.of())).thenReturn(List.of());

        assertTrue(candidateService.getByIds(List.of()).isEmpty());
    }

    @Test
    // Test Case ID: TC-CS-045 - missing ids are ignored by repository result.
    @DisplayName("TC-CS-045 - getByIds() returns only existing candidates when some ids are missing")
    void tc_cs_045_getByIds_whenSomeIdsMissing_returnsExistingCandidatesOnly() {
        when(candidateRepository.findAllById(List.of(45L, 999L))).thenReturn(List.of(candidate(45L, 450L)));

        List<CandidateDetailResponseDTO> result = candidateService.getByIds(List.of(45L, 999L));

        assertEquals(1, result.size());
        assertEquals(45L, result.get(0).getId());
    }

    @Test
    // Test Case ID: TC-CS-046 - business rule from application upload when CV is optional.
    @DisplayName("TC-CS-046 - createCandidateFromApplication() should allow missing cvUrl and save null resumeUrl")
    void tc_cs_046_createCandidateFromApplication_whenCvUrlNull_shouldSaveCandidateWithNullResumeUrl() throws Exception {
        UploadCVDTO request = new UploadCVDTO();
        request.setName("No CV Candidate");
        request.setEmail("nocv@example.com");
        request.setPhone("0900000046");
        request.setJobPositionId(46L);
        request.setCvUrl(null);
        when(candidateRepository.existsByEmailAndJobPositionId("nocv@example.com", 46L)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Candidate result = candidateService.createCandidateFromApplication(request);

        assertNull(result.getResumeUrl());
        verify(candidateRepository).save(argThat(saved -> saved.getResumeUrl() == null));
    }

    @Test
    // Test Case ID: TC-CS-047 - EXPECTED FAIL/Bug exposed: service currently does not return a clear validation exception for null status.
    @DisplayName("TC-CS-047 - EXPECTED FAIL - updateCandidateStatus() should reject null status")
    void tc_cs_047_updateCandidateStatus_whenStatusNull_shouldThrowIllegalArgumentExceptionAndNotSave() {
        when(candidateRepository.findById(47L)).thenReturn(Optional.of(candidate(47L, 470L)));

        assertThrows(IllegalArgumentException.class,
                () -> candidateService.updateCandidateStatus(47L, null, null, 4700L));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    // Test Case ID: TC-CS-048 - EXPECTED FAIL/Bug exposed: service currently saves even when status is unchanged.
    @DisplayName("TC-CS-048 - EXPECTED FAIL - updateCandidateStatus() should not save when new status equals current status")
    void tc_cs_048_updateCandidateStatus_whenStatusIsUnchanged_shouldReturnCurrentStateWithoutSaving() throws IdInvalidException {
        Candidate existingCandidate = candidate(48L, 480L);
        existingCandidate.setStatus(CandidateStatus.INTERVIEW);
        when(candidateRepository.findById(48L)).thenReturn(Optional.of(existingCandidate));
        lenient().when(candidateRepository.save(existingCandidate)).thenReturn(existingCandidate);

        CandidateDetailResponseDTO result = candidateService.updateCandidateStatus(48L, "INTERVIEW", null, 4800L);

        assertEquals(CandidateStatus.INTERVIEW, result.getStatus());
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    // Test Case ID: TC-CS-049 - invalid date filters should fall back to null date query.
    @DisplayName("TC-CS-049 - getAllWithFilters() ignores invalid date filters and still queries repository")
    void tc_cs_049_getAllWithFilters_whenDateFiltersInvalid_shouldQueryWithNullDates() {
        Pageable pageable = PageRequest.of(0, 10);
        when(candidateRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable))).thenReturn(new PageImpl<>(List.of(candidate(49L, 490L)), pageable, 1));

        PaginationDTO result = candidateService.getAllWithFilters(null, null, null, "bad-start", "bad-end", null,
                null, pageable, "token");

        assertEquals(1L, result.getMeta().getTotal());
        verify(candidateRepository).findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable));
    }

    @Test
    // Test Case ID: TC-CS-050 - department lookup should return null when job-service response is non-success.
    @DisplayName("TC-CS-050 - getDepartmentIdByCandidateId() returns null when job-service response is non-success")
    void tc_cs_050_getDepartmentIdByCandidateId_whenJobServiceReturnsNonSuccess_shouldReturnNull() {
        when(candidateRepository.findById(50L)).thenReturn(Optional.of(candidate(50L, 500L)));
        when(jobService.getJobPositionByIdSimple(500L, "token"))
                .thenReturn(ResponseEntity.badRequest().body(jobNode("Bad job", 5000L)));

        Long result = candidateService.getDepartmentIdByCandidateId(50L, "token");

        assertNull(result);
        verify(candidateRepository).findById(50L);
        verify(jobService).getJobPositionByIdSimple(500L, "token");
    }

    @Test
    // Test Case ID: TC-CS-051 - upcoming schedule object with data field should be unpacked.
    @DisplayName("TC-CS-051 - getCandidateDetailById() reads upcoming schedules from response data field")
    void tc_cs_051_getCandidateDetailById_whenScheduleResponseHasDataField_shouldSetUpcomingSchedules()
            throws Exception {
        Candidate existingCandidate = candidate(51L, null);
        when(candidateRepository.findById(51L)).thenReturn(Optional.of(existingCandidate));
        when(reviewCandidateService.getByCandidateId(51L, "token")).thenReturn(new ArrayList<>());
        ObjectNode scheduleResponse = objectMapper.createObjectNode();
        ArrayNode data = objectMapper.createArrayNode();
        data.addObject().put("id", 5101L);
        scheduleResponse.set("data", data);
        when(communicationService.getUpcomingSchedulesForCandidate(51L, "token"))
                .thenReturn(ResponseEntity.ok(scheduleResponse));

        CandidateDetailResponseDTO result = candidateService.getCandidateDetailById(51L, "token");

        assertEquals(1, result.getUpcomingSchedules().size());
        verify(communicationService).getUpcomingSchedulesForCandidate(51L, "token");
    }

}
