package com.example.candidate_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.http.ResponseEntity;

import com.example.candidate_service.dto.comment.CommentResponseDTO;
import com.example.candidate_service.dto.comment.CreateCommentDTO;
import com.example.candidate_service.dto.comment.UpdateCommentDTO;
import com.example.candidate_service.exception.IdInvalidException;
import com.example.candidate_service.model.Candidate;
import com.example.candidate_service.model.Comment;
import com.example.candidate_service.repository.CandidateRepository;
import com.example.candidate_service.repository.CommentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit test for CommentService.
 * CheckDB: repositories are mocked as persistence boundaries; create/update/delete
 * tests capture saved/deleted state. Rollback: no real database is touched.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    // Test Case ID: TC-CMS-001 - get comments and enrich only names returned by user service.
    @DisplayName("TC-CMS-001 - getByCandidateId() returns comments with employee name enrichment")
    void tc_cms_001_getByCandidateId_whenCandidateExists_enrichesEmployeeNames() throws IdInvalidException {
        Long candidateId = 1L;
        String token = "Bearer token";
        Comment first = comment(10L, 5L, "First", LocalDateTime.of(2026, 5, 1, 9, 0));
        Comment second = comment(11L, 6L, "Second", LocalDateTime.of(2026, 5, 1, 10, 0));
        ObjectNode names = objectMapper.createObjectNode();
        names.put("5", "Nguyen Van A");

        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        when(commentRepository.findByCandidate_Id(candidateId)).thenReturn(List.of(first, second));
        when(userService.getEmployeeNames(anyList(), eq(token))).thenReturn(ResponseEntity.ok(names));

        List<CommentResponseDTO> result = commentService.getByCandidateId(candidateId, token);

        assertEquals(2, result.size());
        assertEquals("Nguyen Van A", result.get(0).getEmployeeName());
        assertNull(result.get(1).getEmployeeName());
        assertEquals("First", result.get(0).getContent());

        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<List<Long>> idsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(userService).getEmployeeNames(idsCaptor.capture(), eq(token));
        assertTrue(idsCaptor.getValue().containsAll(List.of(5L, 6L)));
    }

    @Test
    // Test Case ID: TC-CMS-002 - fail fast when candidate does not exist.
    @DisplayName("TC-CMS-002 - getByCandidateId() throws when candidate is missing")
    void tc_cms_002_getByCandidateId_whenCandidateMissing_throwsException() {
        when(candidateRepository.existsById(999L)).thenReturn(false);

        assertThrows(IdInvalidException.class, () -> commentService.getByCandidateId(999L, "token"));
        verify(commentRepository, never()).findByCandidate_Id(anyLong());
        verify(userService, never()).getEmployeeNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-CMS-003 - empty comments should not call user service.
    @DisplayName("TC-CMS-003 - getByCandidateId() returns empty list without employee lookup")
    void tc_cms_003_getByCandidateId_whenNoComments_skipsEmployeeLookup() throws IdInvalidException {
        when(candidateRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByCandidate_Id(1L)).thenReturn(List.of());

        List<CommentResponseDTO> result = commentService.getByCandidateId(1L, "token");

        assertTrue(result.isEmpty());
        verify(userService, never()).getEmployeeNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-CMS-004 - getById missing branch.
    @DisplayName("TC-CMS-004 - getById() throws when comment is missing")
    void tc_cms_004_getById_whenCommentMissing_throwsException() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> commentService.getById(999L));
        verifyNoMoreInteractions(candidateRepository, userService);
    }

    @Test
    // Test Case ID: TC-CMS-005 - CheckDB create state for a valid comment.
    @DisplayName("TC-CMS-005 - create() saves comment for existing candidate")
    void tc_cms_005_create_whenCandidateExists_savesComment() throws IdInvalidException {
        Candidate candidate = candidate(1L);
        CreateCommentDTO dto = new CreateCommentDTO();
        dto.setCandidateId(1L);
        dto.setContent("New comment");

        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(100L);
            saved.setCreatedAt(LocalDateTime.of(2026, 5, 2, 8, 0));
            return saved;
        });

        CommentResponseDTO result = commentService.create(dto, 5L);

        assertEquals(100L, result.getId());
        assertEquals(5L, result.getEmployeeId());
        assertEquals("New comment", result.getContent());

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertEquals(candidate, commentCaptor.getValue().getCandidate());
        assertEquals(5L, commentCaptor.getValue().getEmployeeId());
    }

    @Test
    // Test Case ID: TC-CMS-006 - create fail path when candidate is missing.
    @DisplayName("TC-CMS-006 - create() throws when candidate is missing")
    void tc_cms_006_create_whenCandidateMissing_throwsExceptionAndDoesNotSave() {
        CreateCommentDTO dto = new CreateCommentDTO();
        dto.setCandidateId(404L);
        dto.setContent("Comment");
        when(candidateRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> commentService.create(dto, 5L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    // Test Case ID: TC-CMS-007 - null update content preserves old content.
    @DisplayName("TC-CMS-007 - update() preserves content when request content is null")
    void tc_cms_007_update_whenContentNull_preservesExistingContent() throws IdInvalidException {
        Comment existing = comment(1L, 5L, "Old", LocalDateTime.of(2026, 5, 2, 9, 0));
        UpdateCommentDTO dto = new UpdateCommentDTO();
        dto.setContent(null);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponseDTO result = commentService.update(1L, dto, 5L);

        assertEquals("Old", result.getContent());
        verify(commentRepository).save(existing);
    }

    @Test
    // Test Case ID: TC-CMS-008 - delete fail path when comment is missing.
    @DisplayName("TC-CMS-008 - delete() throws when comment is missing")
    void tc_cms_008_delete_whenCommentMissing_throwsExceptionAndDoesNotDelete() {
        when(commentRepository.existsById(999L)).thenReturn(false);

        assertThrows(IdInvalidException.class, () -> commentService.delete(999L));
        verify(commentRepository, never()).deleteById(anyLong());
    }

    @Test
    // Test Case ID: TC-CMS-009 - EXPECTED FAIL/Bug exposed: blank comment content is accepted.
    @DisplayName("TC-CMS-009 - EXPECTED FAIL - create() should reject blank content")
    void tc_cms_009_create_whenContentBlank_shouldThrowAndNotSave() {
        Candidate candidate = candidate(1L);
        CreateCommentDTO dto = new CreateCommentDTO();
        dto.setCandidateId(1L);
        dto.setContent("   ");
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(candidate));

        assertThrows(IdInvalidException.class, () -> commentService.create(dto, 5L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    // Test Case ID: TC-CMS-010 - EXPECTED FAIL/Bug exposed: update ignores comment ownership.
    @DisplayName("TC-CMS-010 - EXPECTED FAIL - update() should reject non-owner employee")
    void tc_cms_010_update_whenEmployeeIsNotOwner_shouldThrowAndNotSave() {
        Comment existing = comment(1L, 5L, "Old", LocalDateTime.of(2026, 5, 2, 9, 0));
        UpdateCommentDTO dto = new UpdateCommentDTO();
        dto.setContent("Hacked");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IdInvalidException.class, () -> commentService.update(1L, dto, 99L));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    // Test Case ID: TC-CMS-011 - EXPECTED FAIL/Bug exposed: delete does not validate ownership.
    @DisplayName("TC-CMS-011 - EXPECTED FAIL - delete() should require requester ownership")
    void tc_cms_011_delete_shouldHaveRequesterOwnershipCheck() {
        assertFalse(hasDeleteMethodWithRequesterEmployeeId(),
                "CommentService.delete() has no requester employeeId parameter, so ownership cannot be checked.");
    }

    @Test
    // Test Case ID: TC-CMS-012 - getById success path maps stored comment data to response DTO.
    @DisplayName("TC-CMS-012 - getById() returns mapped comment when comment exists")
    void tc_cms_012_getById_whenCommentExists_returnsMappedResponse() throws IdInvalidException {
        Comment existing = comment(12L, 7L, "Reviewed profile", LocalDateTime.of(2026, 5, 3, 14, 30));
        when(commentRepository.findById(12L)).thenReturn(Optional.of(existing));

        CommentResponseDTO result = commentService.getById(12L);

        assertEquals(12L, result.getId());
        assertEquals(7L, result.getEmployeeId());
        assertEquals("Reviewed profile", result.getContent());
        assertEquals(LocalDateTime.of(2026, 5, 3, 14, 30), result.getCreatedAt());
        assertNull(result.getEmployeeName());

        verify(commentRepository).findById(12L);
        verifyNoMoreInteractions(candidateRepository, userService);
    }

    @Test
    // Test Case ID: TC-CMS-013 - CheckDB update path must persist the new comment content.
    @DisplayName("TC-CMS-013 - update() saves new content when request content is provided")
    void tc_cms_013_update_whenContentProvided_persistsUpdatedContent() throws IdInvalidException {
        Comment existing = comment(13L, 8L, "Old comment", LocalDateTime.of(2026, 5, 3, 15, 0));
        UpdateCommentDTO dto = new UpdateCommentDTO();
        dto.setContent("Updated comment");

        when(commentRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponseDTO result = commentService.update(13L, dto, 8L);

        assertEquals("Updated comment", result.getContent());
        assertEquals(8L, result.getEmployeeId());

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertEquals(13L, commentCaptor.getValue().getId());
        assertEquals("Updated comment", commentCaptor.getValue().getContent());
        assertEquals(8L, commentCaptor.getValue().getEmployeeId());
    }

    @Test
    // Test Case ID: TC-CMS-014 - update missing branch must not write anything to persistence.
    @DisplayName("TC-CMS-014 - update() throws when comment is missing and does not save")
    void tc_cms_014_update_whenCommentMissing_throwsExceptionAndDoesNotSave() {
        UpdateCommentDTO dto = new UpdateCommentDTO();
        dto.setContent("Should not be saved");
        when(commentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> commentService.update(404L, dto, 8L));

        verify(commentRepository).findById(404L);
        verify(commentRepository, never()).save(any(Comment.class));
        verifyNoMoreInteractions(candidateRepository, userService);
    }

    @Test
    // Test Case ID: TC-CMS-015 - CheckDB delete path must call repository delete for existing comment.
    @DisplayName("TC-CMS-015 - delete() deletes comment when comment exists")
    void tc_cms_015_delete_whenCommentExists_deletesById() throws IdInvalidException {
        when(commentRepository.existsById(15L)).thenReturn(true);

        commentService.delete(15L);

        verify(commentRepository).existsById(15L);
        verify(commentRepository).deleteById(15L);
        verifyNoMoreInteractions(candidateRepository, userService);
    }

    @Test
    // Test Case ID: TC-CMS-016 - null employeeId should not be sent to user-service name lookup.
    @DisplayName("TC-CMS-016 - getByCandidateId() skips null employeeId during name lookup")
    void tc_cms_016_getByCandidateId_whenEmployeeIdNull_skipsNullEmployeeLookup() throws IdInvalidException {
        Long candidateId = 16L;
        String token = "Bearer token";
        Comment anonymousComment = comment(160L, null, "No employee assigned", LocalDateTime.of(2026, 5, 3, 16, 0));
        Comment namedComment = comment(161L, 9L, "Has employee", LocalDateTime.of(2026, 5, 3, 16, 5));
        ObjectNode names = objectMapper.createObjectNode();
        names.put("9", "Tran Thi B");

        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        when(commentRepository.findByCandidate_Id(candidateId)).thenReturn(List.of(anonymousComment, namedComment));
        when(userService.getEmployeeNames(anyList(), eq(token))).thenReturn(ResponseEntity.ok(names));

        List<CommentResponseDTO> result = commentService.getByCandidateId(candidateId, token);

        assertEquals(2, result.size());
        assertNull(result.get(0).getEmployeeId());
        assertNull(result.get(0).getEmployeeName());
        assertEquals("Tran Thi B", result.get(1).getEmployeeName());

        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<List<Long>> idsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(userService).getEmployeeNames(idsCaptor.capture(), eq(token));
        assertEquals(List.of(9L), idsCaptor.getValue());
    }

    private Comment comment(Long id, Long employeeId, String content, LocalDateTime createdAt) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setEmployeeId(employeeId);
        comment.setContent(content);
        comment.setCreatedAt(createdAt);
        return comment;
    }

    private Candidate candidate(Long id) {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        return candidate;
    }

    private boolean hasDeleteMethodWithRequesterEmployeeId() {
        try {
            CommentService.class.getMethod("delete", Long.class, Long.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
