package com.example.job_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.example.job_service.dto.PaginationDTO;
import com.example.job_service.dto.jobposition.CreateJobPositionDTO;
import com.example.job_service.dto.jobposition.JobPositionResponseDTO;
import com.example.job_service.dto.jobposition.UpdateJobPositionDTO;
import com.example.job_service.exception.IdInvalidException;
import com.example.job_service.model.JobPosition;
import com.example.job_service.model.RecruitmentRequest;
import com.example.job_service.repository.JobPositionRepository;
import com.example.job_service.utils.enums.JobPositionStatus;
import com.example.job_service.utils.enums.RecruitmentRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)

class JobPositionServiceTest {

    @Mock
    private JobPositionRepository jobPositionRepository;

    @Mock
    private RecruitmentRequestService recruitmentRequestService;

    @Mock
    private UserClient userService;

    @Mock
    private CandidateClient candidateClient;

    @InjectMocks
    private JobPositionService jobPositionService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RecruitmentRequest buildRecruitmentRequest(Long id) {
        RecruitmentRequest rr = new RecruitmentRequest();
        rr.setId(id);
        rr.setTitle("Java Developer");
        rr.setQuantity(5);
        rr.setSalaryMin(new BigDecimal("15000000"));
        rr.setSalaryMax(new BigDecimal("25000000"));
        rr.setDepartmentId(1L);
        rr.setStatus(RecruitmentRequestStatus.APPROVED);
        return rr;
    }

    private CreateJobPositionDTO buildCreateDto() {
        CreateJobPositionDTO dto = new CreateJobPositionDTO();
        dto.setTitle("Java Developer");
        dto.setDescription("Description");
        dto.setRequirements("Requirements");
        dto.setBenefits("Benefits");
        dto.setEmploymentType("Full-time");
        dto.setExperienceLevel("Mid");
        dto.setLocation("Hanoi");
        dto.setQuantity(5);
        dto.setDeadline(LocalDate.now().plusMonths(1));
        dto.setRecruitmentRequestId(1L);
        return dto;
    }

    private JobPosition buildJobPosition(Long id, JobPositionStatus status) {
        JobPosition jp = new JobPosition();
        jp.setId(id);
        jp.setTitle("Java Developer");
        jp.setDescription("Description");
        jp.setRequirements("Requirements");
        jp.setBenefits("Benefits");
        jp.setSalaryMin(new BigDecimal("15000000"));
        jp.setSalaryMax(new BigDecimal("25000000"));
        jp.setEmploymentType("Full-time");
        jp.setExperienceLevel("Mid");
        jp.setLocation("Hanoi");
        jp.setRemote(false);
        jp.setQuantity(5);
        jp.setDeadline(LocalDate.now().plusMonths(1));
        jp.setStatus(status);
        jp.setRecruitmentRequest(buildRecruitmentRequest(1L));
        return jp;
    }

    // ==================== create ====================

    @Test
    void create_Success() throws Exception {
        RecruitmentRequest rr = buildRecruitmentRequest(1L);
        CreateJobPositionDTO dto = buildCreateDto();
        when(recruitmentRequestService.findById(1L)).thenReturn(rr);
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> {
            JobPosition position = inv.getArgument(0);
            position.setId(1L);
            return position;
        });
        when(recruitmentRequestService.changeStatus(1L, RecruitmentRequestStatus.COMPLETED)).thenReturn(true);

        JobPosition result = jobPositionService.create(dto);

        assertNotNull(result);
        assertEquals(JobPositionStatus.DRAFT, result.getStatus());
        verify(recruitmentRequestService).changeStatus(1L, RecruitmentRequestStatus.COMPLETED);
    }

    @Test
    void create_WithCustomSalary() throws Exception {
        RecruitmentRequest rr = buildRecruitmentRequest(1L);
        CreateJobPositionDTO dto = buildCreateDto();
        dto.setSalaryMin(new BigDecimal("10000000"));
        dto.setSalaryMax(new BigDecimal("20000000"));

        when(recruitmentRequestService.findById(1L)).thenReturn(rr);
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.create(dto);

        assertEquals(new BigDecimal("10000000"), result.getSalaryMin());
        assertEquals(new BigDecimal("20000000"), result.getSalaryMax());
    }

    @Test
    void create_WithNullSalary_UseRecruitmentRequestSalary() throws Exception {
        RecruitmentRequest rr = buildRecruitmentRequest(1L);
        CreateJobPositionDTO dto = buildCreateDto();
        dto.setSalaryMin(null);
        dto.setSalaryMax(null);

        when(recruitmentRequestService.findById(1L)).thenReturn(rr);
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.create(dto);

        assertEquals(new BigDecimal("15000000"), result.getSalaryMin());
        assertEquals(new BigDecimal("25000000"), result.getSalaryMax());
    }

    @Test
    void create_WithNullIsRemote_DefaultFalse() throws Exception {
        RecruitmentRequest rr = buildRecruitmentRequest(1L);
        CreateJobPositionDTO dto = buildCreateDto();
        dto.setIsRemote(null);

        when(recruitmentRequestService.findById(1L)).thenReturn(rr);
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.create(dto);

        assertFalse(result.isRemote());
    }

    @Test
    void create_WithIsRemoteTrue() throws Exception {
        RecruitmentRequest rr = buildRecruitmentRequest(1L);
        CreateJobPositionDTO dto = buildCreateDto();
        dto.setIsRemote(true);

        when(recruitmentRequestService.findById(1L)).thenReturn(rr);
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.create(dto);

        assertTrue(result.isRemote());
    }

    @Test
    void create_RecruitmentRequestNotFound_ThrowException() throws Exception {
        when(recruitmentRequestService.findById(9999L)).thenThrow(new IdInvalidException("Not found"));
        CreateJobPositionDTO dto = buildCreateDto();
        dto.setRecruitmentRequestId(9999L);

        assertThrows(IdInvalidException.class, () -> jobPositionService.create(dto));
    }

    // ==================== findById ====================

    @Test
    void findById_Success() throws Exception {
        JobPosition jp = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(jp));

        JobPosition result = jobPositionService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.findById(9999L));
    }

    // ==================== getByIdSimple ====================

    @Test
    void getByIdSimple_Success() throws Exception {
        JobPosition jp = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(jp));

        JobPosition result = jobPositionService.getByIdSimple(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    // ==================== getByIdWithDepartmentName ====================

    @Test
    void getByIdWithDepartmentName_Success() throws Exception {
        JobPosition jp = buildJobPosition(1L, JobPositionStatus.PUBLISHED);
        JsonNode deptNode = objectMapper.createObjectNode().put("name", "IT Department");

        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(jp));
        when(userService.getDepartmentById(1L, "token")).thenReturn(ResponseEntity.ok(deptNode));
        when(candidateClient.countCandidatesByJobPositionId(1L, "token")).thenReturn(5);

        JobPositionResponseDTO result = jobPositionService.getByIdWithDepartmentName(1L, "token");

        assertNotNull(result);
        assertEquals("IT Department", result.getDepartmentName());
        assertEquals(5, result.getApplicationCount());
    }

    // ==================== getByIdsWithDepartmentName ====================

    @Test
    void getByIdsWithDepartmentName_Success() {
        List<JobPosition> positions = List.of(
                buildJobPosition(1L, JobPositionStatus.PUBLISHED),
                buildJobPosition(2L, JobPositionStatus.DRAFT)
        );

        when(jobPositionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(positions);
        when(userService.getDepartmentsByIds(List.of(1L), "token")).thenReturn(Map.of(1L, "IT Department"));

        List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(List.of(1L, 2L), "token");

        assertEquals(2, result.size());
        assertEquals("IT Department", result.get(0).getDepartmentName());
    }

    @Test
    void getByIdsWithDepartmentName_EmptyList() {
        List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(List.of(), "token");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdsWithDepartmentName_NullList() {
        List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(null, "token");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdsWithDepartmentName_EmptyPositions() {
        when(jobPositionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of());

        List<JobPositionResponseDTO> result = jobPositionService.getByIdsWithDepartmentName(List.of(1L, 2L), "token");

        assertTrue(result.isEmpty());
    }

    // ==================== getByIdWithPublished ====================

    @Test
    void getByIdWithPublished_Success() throws Exception {
        JobPosition jp = buildJobPosition(1L, JobPositionStatus.PUBLISHED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(jp));

        JobPosition result = jobPositionService.getByIdWithPublished(1L);

        assertNotNull(result);
        assertEquals(JobPositionStatus.PUBLISHED, result.getStatus());
    }

    @Test
    void getByIdWithPublished_NotPublished_ThrowException() {
        JobPosition jp = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(jp));

        assertThrows(IdInvalidException.class, () -> jobPositionService.getByIdWithPublished(1L));
    }

    // ==================== findAllWithFiltersSimple ====================

    @Test
    void findAllWithFiltersSimple_WithIds() {
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        when(jobPositionRepository.findByIdIn(List.of(1L))).thenReturn(positions);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, "1");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithMultipleIds() {
        List<JobPosition> positions = List.of(
                buildJobPosition(1L, JobPositionStatus.PUBLISHED),
                buildJobPosition(2L, JobPositionStatus.DRAFT)
        );
        when(jobPositionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(positions);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, "1,2");

        assertEquals(2, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithIdsAndSpaces() {
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        when(jobPositionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(positions);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, " 1 , 2 ");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithEmptyIds() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)));
        when(jobPositionRepository.findByFilters(null, null, null, null, pageable)).thenReturn(page);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, "");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithInvalidIds() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)));
        when(jobPositionRepository.findByFilters(null, null, null, null, pageable)).thenReturn(page);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, "abc,def");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithEmptyIdList() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)));
        when(jobPositionRepository.findByFilters(null, null, null, null, pageable)).thenReturn(page);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(null, null, null, null, " , , ");

        assertEquals(1, result.size());
    }

    @Test
    void findAllWithFiltersSimple_WithFilters() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)));
        when(jobPositionRepository.findByFilters(1L, JobPositionStatus.PUBLISHED, true, "Java", pageable)).thenReturn(page);

        List<JobPosition> result = jobPositionService.findAllWithFiltersSimple(1L, JobPositionStatus.PUBLISHED, true, "Java", null);

        assertEquals(1, result.size());
    }

    // ==================== findAllWithFiltersSimplePaged ====================

    @Test
    void findAllWithFiltersSimplePaged_WithIds() {
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        Pageable pageable = PageRequest.of(0, 10);
        when(jobPositionRepository.findByIdIn(List.of(1L))).thenReturn(positions);

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "1", pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test
    void findAllWithFiltersSimplePaged_WithIdsAndPagination() {
        List<JobPosition> positions = List.of(
                buildJobPosition(1L, JobPositionStatus.PUBLISHED),
                buildJobPosition(2L, JobPositionStatus.DRAFT),
                buildJobPosition(3L, JobPositionStatus.CLOSED)
        );
        Pageable pageable = PageRequest.of(1, 2);
        when(jobPositionRepository.findByIdIn(List.of(1L, 2L, 3L))).thenReturn(positions);

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "1,2,3", pageable);

        assertNotNull(result);
        assertEquals(3, result.getMeta().getTotal());
    }

    @Test
    void findAllWithFiltersSimplePaged_WithIdsOutOfRange() {
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        Pageable pageable = PageRequest.of(5, 10);
        when(jobPositionRepository.findByIdIn(List.of(1L))).thenReturn(positions);

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "1", pageable);

        assertNotNull(result);
        assertEquals(0, ((List<?>) result.getResult()).size());
    }

    @Test
    void findAllWithFiltersSimplePaged_WithEmptyIds() {
        Pageable pageable = PageRequest.of(0, 10);
        // Khi parse " , , " sẽ tạo ra empty list và trả về Page.empty

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, " , , ", pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
    }

    @Test
    void findAllWithFiltersSimplePaged_WithInvalidIds() {
        Pageable pageable = PageRequest.of(0, 10);
        // Khi parse "abc" sẽ throw exception và code catch để trả về Page.empty

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(null, null, null, null, "abc", pageable);

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotal());
    }


    @Test
    void findAllWithFiltersSimplePaged_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)), pageable, 1);
        when(jobPositionRepository.findByFilters(1L, JobPositionStatus.PUBLISHED, true, "Java", pageable)).thenReturn(page);

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplePaged(1L, JobPositionStatus.PUBLISHED, true, "Java", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    // ==================== findAllWithFilters ====================

    @Test
    void findAllWithFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        Page<JobPosition> page = new PageImpl<>(positions, pageable, 1);

        when(jobPositionRepository.findByFilters(null, JobPositionStatus.PUBLISHED, true, "Java", pageable)).thenReturn(page);
        when(userService.getDepartmentsByIds(List.of(1L), "token")).thenReturn(Map.of(1L, "IT Department"));
        when(candidateClient.countCandidatesByJobPositionIds(List.of(1L), "token")).thenReturn(Map.of(1L, 5));

        PaginationDTO result = jobPositionService.findAllWithFilters(null, JobPositionStatus.PUBLISHED, true, "Java", pageable, "token");

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test
    void findAllWithFilters_DepartmentId1_ConvertToNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)), pageable, 1);

        when(jobPositionRepository.findByFilters(null, null, null, null, pageable)).thenReturn(page);
        when(userService.getDepartmentsByIds(any(), any())).thenReturn(Map.of(1L, "IT"));
        when(candidateClient.countCandidatesByJobPositionIds(any(), any())).thenReturn(Map.of(1L, 0));

        PaginationDTO result = jobPositionService.findAllWithFilters(1L, null, null, null, pageable, "token");

        verify(jobPositionRepository).findByFilters(null, null, null, null, pageable);
    }

    @Test
    void findAllWithFilters_DepartmentIdNot1() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPosition> page = new PageImpl<>(List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED)), pageable, 1);

        when(jobPositionRepository.findByFilters(2L, null, null, null, pageable)).thenReturn(page);
        when(userService.getDepartmentsByIds(any(), any())).thenReturn(Map.of(1L, "IT"));
        when(candidateClient.countCandidatesByJobPositionIds(any(), any())).thenReturn(Map.of(1L, 0));

        PaginationDTO result = jobPositionService.findAllWithFilters(2L, null, null, null, pageable, "token");

        verify(jobPositionRepository).findByFilters(2L, null, null, null, pageable);
    }

    // ==================== findAllWithFiltersSimplified ====================

    @Test
    void findAllWithFiltersSimplified_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<JobPosition> positions = List.of(buildJobPosition(1L, JobPositionStatus.PUBLISHED));
        Page<JobPosition> page = new PageImpl<>(positions, pageable, 1);

        when(jobPositionRepository.findByFilters(1L, JobPositionStatus.PUBLISHED, true, "Java", pageable)).thenReturn(page);
        when(userService.getDepartmentsByIds(List.of(1L), "token")).thenReturn(Map.of(1L, "IT Department"));
        when(candidateClient.countCandidatesByJobPositionIds(List.of(1L), "token")).thenReturn(Map.of(1L, 3));

        PaginationDTO result = jobPositionService.findAllWithFiltersSimplified(1L, JobPositionStatus.PUBLISHED, 1L, true, "Java", pageable, "token");

        assertNotNull(result);
        assertEquals(1, result.getMeta().getTotal());
    }

    // ==================== update ====================

    @Test
    void update_AllFields() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        UpdateJobPositionDTO dto = new UpdateJobPositionDTO();
        dto.setTitle("Senior Java Developer");
        dto.setDescription("New Description");
        dto.setRequirements("New Requirements");
        dto.setBenefits("New Benefits");
        dto.setSalaryMin(new BigDecimal("20000000"));
        dto.setSalaryMax(new BigDecimal("30000000"));
        dto.setEmploymentType("Contract");
        dto.setExperienceLevel("Senior");
        dto.setLocation("Ho Chi Minh");
        dto.setIsRemote(true);
        dto.setQuantity(10);
        dto.setDeadline(LocalDate.now().plusMonths(2));
        dto.setYearsOfExperience("5+");

        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.update(1L, dto);

        assertEquals("Senior Java Developer", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals("New Requirements", result.getRequirements());
        assertEquals("New Benefits", result.getBenefits());
        assertEquals(new BigDecimal("20000000"), result.getSalaryMin());
        assertEquals(new BigDecimal("30000000"), result.getSalaryMax());
        assertEquals("Contract", result.getEmploymentType());
        assertEquals("Senior", result.getExperienceLevel());
        assertEquals("Ho Chi Minh", result.getLocation());
        assertTrue(result.isRemote());
        assertEquals(10, result.getQuantity());
        assertEquals("5+", result.getYearsOfExperience());
    }

    @Test
    void update_NoFields() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        UpdateJobPositionDTO dto = new UpdateJobPositionDTO();

        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.update(1L, dto);

        assertEquals("Java Developer", result.getTitle());
    }

    @Test
    void update_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.update(9999L, new UpdateJobPositionDTO()));
    }

    // ==================== delete ====================

    @Test
    void delete_Success() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertTrue(jobPositionService.delete(1L));
        verify(jobPositionRepository).delete(existing);
    }

    @Test
    void delete_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.delete(9999L));
    }

    // ==================== publish ====================

    @Test
    void publish_Success() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.publish(1L);

        assertEquals(JobPositionStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
    }

    @Test
    void publish_AlreadyPublished_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.PUBLISHED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.publish(1L));
    }

    @Test
    void publish_Closed_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.CLOSED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.publish(1L));
    }

    @Test
    void publish_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.publish(9999L));
    }

    // ==================== close ====================

    @Test
    void close_Success() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.PUBLISHED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.close(1L);

        assertEquals(JobPositionStatus.CLOSED, result.getStatus());
    }

    @Test
    void close_NotPublished_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.close(1L));
    }

    @Test
    void close_AlreadyClosed_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.CLOSED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.close(1L));
    }

    @Test
    void close_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.close(9999L));
    }

    // ==================== reopen ====================

    @Test
    void reopen_Success() throws Exception {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.CLOSED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobPositionRepository.save(any(JobPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPosition result = jobPositionService.reopen(1L);

        assertEquals(JobPositionStatus.PUBLISHED, result.getStatus());
    }

    @Test
    void reopen_NotClosed_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.PUBLISHED);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.reopen(1L));
    }

    @Test
    void reopen_Draft_ThrowException() {
        JobPosition existing = buildJobPosition(1L, JobPositionStatus.DRAFT);
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> jobPositionService.reopen(1L));
    }

    @Test
    void reopen_NotFound_ThrowException() {
        when(jobPositionRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> jobPositionService.reopen(9999L));
    }
}

