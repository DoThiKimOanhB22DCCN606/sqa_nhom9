package com.example.schedule_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import com.example.schedule_service.dto.PaginationDTO;
import com.example.schedule_service.dto.schedule.AvailableParticipantDTO;
import com.example.schedule_service.dto.schedule.CreateScheduleDTO;
import com.example.schedule_service.dto.schedule.ScheduleDetailDTO;
import com.example.schedule_service.messaging.NotificationProducer;
import com.example.schedule_service.model.Schedule;
import com.example.schedule_service.model.ScheduleParticipant;
import com.example.schedule_service.repository.ScheduleParticipantRepository;
import com.example.schedule_service.repository.ScheduleRepository;
import com.example.schedule_service.utils.enums.MeetingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit test for ScheduleService.
 * CheckDB: repository mocks represent database access; create/update/delete tests
 * assert returned objects plus repository save/delete/find interactions.
 * Rollback: no real database is used, so each test starts with fresh Mockito
 * state and leaves no persisted data.
 */
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleParticipantRepository scheduleParticipantRepository;

    @Mock
    private UserService userService;

    @Mock
    private CandidateService candidateService;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private ScheduleService scheduleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Schedule buildSchedule(Long id, String title) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setTitle(title);
        schedule.setFormat("ONLINE");
        schedule.setMeetingType(MeetingType.INTERVIEW);
        schedule.setStatus("SCHEDULED");
        schedule.setLocation("Zoom");
        schedule.setStartTime(LocalDateTime.of(2026, 5, 4, 10, 0));
        schedule.setEndTime(LocalDateTime.of(2026, 5, 4, 11, 0));
        schedule.setParticipants(new HashSet<>());
        schedule.setCreatedById(500L);
        return schedule;
    }

    @Test

    // Test Case ID: TC-SS-001 - createSchedule() should save schedule and send notifications to users

    @DisplayName("TC-SS-001 - createSchedule() should save schedule and send notifications to users")
    void tc_ss_001_createSchedule_whenInputIsValid_shouldSaveAndNotifyUsers() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Interview session");
        request.setDescription("Interview with candidate");
        request.setFormat("ONLINE");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setStatus("SCHEDULED");
        request.setLocation("Zoom");
        request.setStartTime(LocalDateTime.of(2026, 5, 5, 14, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 5, 15, 0));
        request.setCandidateId(100L);
        request.setUserIds(List.of(200L, 201L));

        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });

        Schedule result = scheduleService.createSchedule(request);

        assertEquals(1L, result.getId());
        assertEquals(3, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository, atLeastOnce()).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getAllValues().get(scheduleCaptor.getAllValues().size() - 1);
        assertEquals("Interview session", persisted.getTitle());
        assertEquals("SCHEDULED", persisted.getStatus());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "CANDIDATE".equals(p.getParticipantType()) && Long.valueOf(100L).equals(p.getParticipantId())));
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "USER".equals(p.getParticipantType()) && Long.valueOf(200L).equals(p.getParticipantId())));
        verify(notificationProducer, times(1)).sendNotificationToMultiple(eq(List.of(200L, 201L)), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-002 - updateSchedule() should rebuild participants and send update notification

    @DisplayName("TC-SS-002 - updateSchedule() should rebuild participants and send update notification")
    void tc_ss_002_updateSchedule_whenInputIsValid_shouldUpdateParticipantsAndNotify() {
        Schedule existing = buildSchedule(2L, "Original title");
        ScheduleParticipant existingParticipant = new ScheduleParticipant();
        existingParticipant.setId(50L);
        existingParticipant.setParticipantType("USER");
        existingParticipant.setParticipantId(300L);
        existingParticipant.setResponseStatus("PENDING");
        existingParticipant.setSchedule(existing);
        existing.getParticipants().add(existingParticipant);

        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Updated interview");
        request.setDescription("Updated description");
        request.setFormat("OFFLINE");
        request.setMeetingType(MeetingType.MEETING);
        request.setStatus("SCHEDULED");
        request.setLocation("Office");
        request.setStartTime(LocalDateTime.of(2026, 5, 6, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 6, 10, 0));
        request.setUserIds(List.of(400L));

        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schedule result = scheduleService.updateSchedule(2L, request);

        assertEquals("Updated interview", result.getTitle());
        assertEquals(1, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> p.getParticipantId().equals(400L)));
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getValue();
        assertEquals("Updated interview", persisted.getTitle());
        assertEquals("OFFLINE", persisted.getFormat());
        assertEquals("Office", persisted.getLocation());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "USER".equals(p.getParticipantType()) && Long.valueOf(400L).equals(p.getParticipantId())));
        assertTrue(persisted.getParticipants().stream().noneMatch(p -> Long.valueOf(300L).equals(p.getParticipantId())));
        verify(notificationProducer, times(1)).sendNotificationToMultiple(eq(List.of(400L)), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-003 - deleteSchedule() should delete schedule when exists

    @DisplayName("TC-SS-003 - deleteSchedule() should delete schedule when exists")
    void tc_ss_003_deleteSchedule_whenScheduleExists_shouldDelete() {
        when(scheduleRepository.existsById(3L)).thenReturn(true);

        scheduleService.deleteSchedule(3L);

        verify(scheduleRepository, times(1)).deleteById(3L);
    }

    @Test

    // Test Case ID: TC-SS-004 - deleteSchedule() should throw RuntimeException when schedule missing

    @DisplayName("TC-SS-004 - deleteSchedule() should throw RuntimeException when schedule missing")
    void tc_ss_004_deleteSchedule_whenScheduleMissing_shouldThrowRuntimeException() {
        when(scheduleRepository.existsById(4L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> scheduleService.deleteSchedule(4L));
        assertTrue(exception.getMessage().contains("4"));
    }

    @Test

    // Test Case ID: TC-SS-005 - getScheduleById() should return schedule when found

    @DisplayName("TC-SS-005 - getScheduleById() should return schedule when found")
    void tc_ss_005_getScheduleById_whenFound_shouldReturnSchedule() {
        Schedule schedule = buildSchedule(5L, "Session");
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        Schedule result = scheduleService.getScheduleById(5L);

        assertEquals(5L, result.getId());
        assertEquals("Session", result.getTitle());
        verify(scheduleRepository).findById(5L);
    }

    @Test

    // Test Case ID: TC-SS-006 - getScheduleById() should throw RuntimeException when not found

    @DisplayName("TC-SS-006 - getScheduleById() should throw RuntimeException when not found")
    void tc_ss_006_getScheduleById_whenNotFound_shouldThrowRuntimeException() {
        when(scheduleRepository.findById(6L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> scheduleService.getScheduleById(6L));
        assertTrue(exception.getMessage().contains("6"));
    }

    @Test

    // Test Case ID: TC-SS-007 - getScheduleWithParticipantNames() should build participant names for users and candidates

    @DisplayName("TC-SS-007 - getScheduleWithParticipantNames() should build participant names for users and candidates")
    void tc_ss_007_getScheduleWithParticipantNames_whenParticipantsExist_shouldReturnDetailDTOWithNames() {
        Schedule schedule = buildSchedule(7L, "Interview");
        ScheduleParticipant userParticipant = new ScheduleParticipant();
        userParticipant.setId(61L);
        userParticipant.setParticipantType("USER");
        userParticipant.setParticipantId(701L);
        userParticipant.setResponseStatus("PENDING");
        userParticipant.setSchedule(schedule);
        schedule.getParticipants().add(userParticipant);

        ScheduleParticipant candidateParticipant = new ScheduleParticipant();
        candidateParticipant.setId(62L);
        candidateParticipant.setParticipantType("CANDIDATE");
        candidateParticipant.setParticipantId(801L);
        candidateParticipant.setResponseStatus("PENDING");
        candidateParticipant.setSchedule(schedule);
        schedule.getParticipants().add(candidateParticipant);

        when(scheduleRepository.findById(7L)).thenReturn(Optional.of(schedule));

        ObjectNode userNames = objectMapper.createObjectNode();
        userNames.put("701", "Employee One");
        when(userService.getEmployeeNames(List.of(701L), "token")).thenReturn(ResponseEntity.ok(userNames));

        ObjectNode candidateNames = objectMapper.createObjectNode();
        candidateNames.put("801", "Candidate One");
        when(candidateService.getCandidateNames(List.of(801L), "token")).thenReturn(ResponseEntity.ok(candidateNames));

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(7L, "token");

        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> "Employee One".equals(p.getName())));
        assertTrue(result.getParticipants().stream().anyMatch(p -> "Candidate One".equals(p.getName())));
        verify(scheduleRepository).findById(7L);
    }

    @Test

    // Test Case ID: TC-SS-008 - createSchedule() should default status and skip notification when userIds are null

    @DisplayName("TC-SS-008 - createSchedule() should default status and skip notification when userIds are null")
    void tc_ss_008_createSchedule_whenStatusAndUsersMissing_shouldDefaultStatusAndSkipNotification() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("No users");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(808L);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            saved.setId(308L);
            return saved;
        });

        Schedule result = scheduleService.createSchedule(request);

        assertEquals("SCHEDULED", result.getStatus());
        assertEquals(1, result.getParticipants().size());
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository, atLeastOnce()).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getAllValues().get(scheduleCaptor.getAllValues().size() - 1);
        assertEquals("SCHEDULED", persisted.getStatus());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "CANDIDATE".equals(p.getParticipantType()) && Long.valueOf(808L).equals(p.getParticipantId())));
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-009 - updateSchedule() should throw RuntimeException when schedule is missing

    @DisplayName("TC-SS-009 - updateSchedule() should throw RuntimeException when schedule is missing")
    void tc_ss_009_updateSchedule_whenMissing_shouldThrowRuntimeException() {
        when(scheduleRepository.findById(309L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.updateSchedule(309L, new CreateScheduleDTO()));

        assertTrue(exception.getMessage().contains("309"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test

    // Test Case ID: TC-SS-010 - updateSchedule() should initialize participants when existing collection is null

    @DisplayName("TC-SS-010 - updateSchedule() should initialize participants when existing collection is null")
    void tc_ss_010_updateSchedule_whenParticipantsNull_shouldInitializeAndSaveParticipants() {
        Schedule existing = buildSchedule(310L, "Original");
        existing.setParticipants(null);
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Updated");
        request.setMeetingType(MeetingType.MEETING);
        request.setCandidateId(910L);
        request.setUserIds(List.of(911L));
        when(scheduleRepository.findById(310L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(existing)).thenReturn(existing);

        Schedule result = scheduleService.updateSchedule(310L, request);

        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        assertTrue(result.getParticipants().stream().anyMatch(p -> Long.valueOf(911L).equals(p.getParticipantId())));
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getValue();
        assertEquals("Updated", persisted.getTitle());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "CANDIDATE".equals(p.getParticipantType()) && Long.valueOf(910L).equals(p.getParticipantId())));
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "USER".equals(p.getParticipantType()) && Long.valueOf(911L).equals(p.getParticipantId())));
    }

    @Test

    // Test Case ID: TC-SS-011 - getScheduleWithParticipantNames() should use Unknown when remote name services fail

    @DisplayName("TC-SS-011 - getScheduleWithParticipantNames() should use Unknown when remote name services fail")
    void tc_ss_011_getScheduleWithParticipantNames_whenRemoteNamesFail_shouldUseUnknownNames() {
        Schedule schedule = buildSchedule(311L, "Unknown names");
        ScheduleParticipant user = new ScheduleParticipant();
        user.setId(1L);
        user.setParticipantType("USER");
        user.setParticipantId(1L);
        user.setSchedule(schedule);
        schedule.getParticipants().add(user);
        when(scheduleRepository.findById(311L)).thenReturn(Optional.of(schedule));
        when(userService.getEmployeeNames(List.of(1L), "token")).thenReturn(ResponseEntity.status(500).build());

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(311L, "token");

        assertEquals("Unknown", result.getParticipants().get(0).getName());
    }

    @Test

    // Test Case ID: TC-SS-012 - getAllSchedules() should query by specific date when date provided

    @DisplayName("TC-SS-012 - getAllSchedules() should query by specific date when date provided")
    void tc_ss_012_getAllSchedules_whenDateProvided_shouldQueryBetweenDayBounds() {
        Schedule schedule = buildSchedule(312L, "Date");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "asc",
                LocalDate.of(2026, 5, 4), null, null, null, null, null, null);

        assertEquals(1, ((List<?>) result.getResult()).size());
        assertEquals(1, result.getMeta().getPage());
        verify(scheduleRepository).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-013 - getAllSchedules() should query by month when year and month provided

    @DisplayName("TC-SS-013 - getAllSchedules() should query by month when year and month provided")
    void tc_ss_013_getAllSchedules_whenYearAndMonthProvided_shouldQueryBetweenMonthBounds() {
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginationDTO result = scheduleService.getAllSchedules(0, 0, "startTime", "desc",
                null, 2026, 5, null, null, null, null);

        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
    }

    @Test

    // Test Case ID: TC-SS-014 - getAllSchedules() should query by status when status provided

    @DisplayName("TC-SS-014 - getAllSchedules() should query by status when status provided")
    void tc_ss_014_getAllSchedules_whenStatusProvided_shouldQueryByStatus() {
        Schedule schedule = buildSchedule(314L, "Status");
        when(scheduleRepository.findByStatus(eq("DONE"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, "DONE", null, null, null);

        assertEquals(1, result.getMeta().getTotal());
        verify(scheduleRepository).findByStatus(eq("DONE"), any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-015 - getAllSchedules() should query by meetingType when meetingType provided

    @DisplayName("TC-SS-015 - getAllSchedules() should query by meetingType when meetingType provided")
    void tc_ss_015_getAllSchedules_whenMeetingTypeProvided_shouldQueryByMeetingType() {
        when(scheduleRepository.findByMeetingType(eq("INTERVIEW"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, null, "INTERVIEW", null, null);

        assertEquals(0, result.getMeta().getTotal());
        verify(scheduleRepository).findByMeetingType(eq("INTERVIEW"), any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-016 - getAllSchedules() should filter returned page by participant when participant filter is provided

    @DisplayName("TC-SS-016 - getAllSchedules() should filter returned page by participant when participant filter is provided")
    void tc_ss_016_getAllSchedules_whenParticipantFilterProvided_shouldKeepOnlyMatchingSchedules() {
        Schedule matching = buildSchedule(316L, "Matching");
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantType("USER");
        participant.setParticipantId(3160L);
        matching.getParticipants().add(participant);
        Schedule other = buildSchedule(317L, "Other");
        when(scheduleRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(matching, other), PageRequest.of(0, 10), 2));

        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, null, null, 3160L, "USER");

        assertEquals(1, ((List<?>) result.getResult()).size());
        assertEquals(1, result.getMeta().getTotal());
    }

    @Test

    // Test Case ID: TC-SS-017 - updateScheduleStatus() should update status and save schedule

    @DisplayName("TC-SS-017 - updateScheduleStatus() should update status and save schedule")
    void tc_ss_017_updateScheduleStatus_whenFound_shouldPersistNewStatus() {
        Schedule schedule = buildSchedule(317L, "Status");
        when(scheduleRepository.findById(317L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(schedule)).thenReturn(schedule);

        Schedule result = scheduleService.updateScheduleStatus(317L, "DONE");

        assertEquals("DONE", result.getStatus());
        verify(scheduleRepository).save(schedule);
    }

    @Test

    // Test Case ID: TC-SS-018 - updateScheduleStatus() should throw RuntimeException when schedule missing

    @DisplayName("TC-SS-018 - updateScheduleStatus() should throw RuntimeException when schedule missing")
    void tc_ss_018_updateScheduleStatus_whenMissing_shouldThrowRuntimeException() {
        when(scheduleRepository.findById(318L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> scheduleService.updateScheduleStatus(318L, "DONE"));
    }

    @Test

    // Test Case ID: TC-SS-019 - getSchedulesDetailed() should query by date range and filter status meetingType participant

    @DisplayName("TC-SS-019 - getSchedulesDetailed() should query by date range and filter status meetingType participant")
    void tc_ss_019_getSchedulesDetailed_whenAllFiltersProvided_shouldReturnMatchingDetailedSchedules() {
        Schedule matching = buildSchedule(319L, "Matching");
        matching.setStatus("SCHEDULED");
        matching.setMeetingType(MeetingType.INTERVIEW);
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantType("USER");
        participant.setParticipantId(3190L);
        matching.getParticipants().add(participant);
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(matching));
        ObjectNode names = objectMapper.createObjectNode();
        names.put("3190", "Interviewer");
        when(userService.getEmployeeNames(List.of(3190L), "token")).thenReturn(ResponseEntity.ok(names));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(null, null, null, null,
                "scheduled", "INTERVIEW", 3190L, "USER", "token", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertEquals(1, result.size());
        assertEquals("Interviewer", result.get(0).getParticipants().get(0).getName());
    }

    @Test

    // Test Case ID: TC-SS-020 - getSchedulesDetailed() should query all schedules when no date filter exists

    @DisplayName("TC-SS-020 - getSchedulesDetailed() should query all schedules when no date filter exists")
    void tc_ss_020_getSchedulesDetailed_whenNoDateFilter_shouldQueryAllSorted() {
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(List.of(buildSchedule(320L, "All")));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(null, null, null, null,
                null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
        verify(scheduleRepository).findAll(any(Sort.class));
    }

    @Test

    // Test Case ID: TC-SS-021 - getAvailableParticipants() should return empty when user service has no employees

    @DisplayName("TC-SS-021 - getAvailableParticipants() should return empty when user service has no employees")
    void tc_ss_021_getAvailableParticipants_whenNoEmployees_shouldReturnEmptyList() {
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of());

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");

        assertTrue(result.isEmpty());
        verify(scheduleRepository, never()).findOverlappingSchedules(any(), any(), any());
    }

    @Test

    // Test Case ID: TC-SS-022 - getAvailableParticipants() should remove busy employees and enrich available names

    @DisplayName("TC-SS-022 - getAvailableParticipants() should remove busy employees and enrich available names")
    void tc_ss_022_getAvailableParticipants_whenSomeEmployeesBusy_shouldReturnOnlyAvailableEmployees() {
        Schedule busy = buildSchedule(322L, "Busy");
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(1L, 2L));
        when(scheduleRepository.findOverlappingSchedules(any(), any(), eq(99L))).thenReturn(List.of(busy));
        when(scheduleParticipantRepository.findParticipantIdsByScheduleIds(List.of(322L))).thenReturn(List.of(1L));
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode employee = objectMapper.createObjectNode();
        employee.put("name", "Available User");
        employee.put("departmentName", "QA");
        body.set("2", employee);
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(2L), "token")).thenReturn(ResponseEntity.ok(body));

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), 99L, "token");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Available User", result.get(0).getName());
        assertEquals("QA", result.get(0).getDepartmentName());
    }

    @Test

    // Test Case ID: TC-SS-023 - getAvailableParticipants() should return empty when all employees are busy

    @DisplayName("TC-SS-023 - getAvailableParticipants() should return empty when all employees are busy")
    void tc_ss_023_getAvailableParticipants_whenAllEmployeesBusy_shouldReturnEmptyList() {
        Schedule busy = buildSchedule(323L, "Busy");
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(1L));
        when(scheduleRepository.findOverlappingSchedules(any(), any(), isNull())).thenReturn(List.of(busy));
        when(scheduleParticipantRepository.findParticipantIdsByScheduleIds(List.of(323L))).thenReturn(List.of(1L));

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");

        assertTrue(result.isEmpty());
    }

    @Test

    // Test Case ID: TC-SS-024 - getSchedulesForStatistics() should filter date range by status and meeting type

    @DisplayName("TC-SS-024 - getSchedulesForStatistics() should filter date range by status and meeting type")
    void tc_ss_024_getSchedulesForStatistics_whenDateStatusMeetingTypeProvided_shouldReturnMatchingStats() {
        Schedule match = buildSchedule(324L, "Match");
        match.setStatus("DONE");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(325L, "Other");
        other.setStatus("DONE");
        other.setMeetingType(MeetingType.MEETING);
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));

        var result = scheduleService.getSchedulesForStatistics(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                "DONE", "INTERVIEW");

        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getMeetingType());
    }

    @Test

    // Test Case ID: TC-SS-025 - getSchedulesForStatistics() should limit all schedules to first 10000 records

    @DisplayName("TC-SS-025 - getSchedulesForStatistics() should limit all schedules to first 10000 records")
    void tc_ss_025_getSchedulesForStatistics_whenMoreThanLimit_shouldReturnFirst10000() {
        List<Schedule> schedules = java.util.stream.IntStream.range(0, 10001)
                .mapToObj(i -> buildSchedule((long) i, "S" + i))
                .toList();
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(schedules);

        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);

        assertEquals(10000, result.size());
    }

    @Test

    // Test Case ID: TC-SS-026 - getCandidateIdsByInterviewer() should return repository candidate ids

    @DisplayName("TC-SS-026 - getCandidateIdsByInterviewer() should return repository candidate ids")
    void tc_ss_026_getCandidateIdsByInterviewer_shouldReturnRepositoryResult() {
        when(scheduleParticipantRepository.findCandidateIdsByInterviewer(326L)).thenReturn(List.of(1L, 2L));

        List<Long> result = scheduleService.getCandidateIdsByInterviewer(326L);

        assertEquals(List.of(1L, 2L), result);
    }

    @Test
    // Test Case ID: TC-SS-027 - EXPECTED FAIL/Bug exposed: service currently accepts endTime before startTime.
    @DisplayName("TC-SS-027 - EXPECTED FAIL - createSchedule() should reject endTime before startTime")
    void tc_ss_027_createSchedule_whenEndBeforeStart_shouldThrowAndNotSave() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Invalid interview time");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setStartTime(LocalDateTime.of(2026, 4, 5, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 5, 9, 30));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    // Test Case ID: TC-SS-028 - EXPECTED FAIL/Bug exposed: service currently allows duplicate candidate schedules.
    @DisplayName("TC-SS-028 - EXPECTED FAIL - createSchedule() should reject candidate who already has an interview schedule")
    void tc_ss_028_createSchedule_whenCandidateAlreadyHasSchedule_shouldThrowAndNotSave() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Duplicate candidate schedule");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(100L);
        request.setUserIds(List.of(200L));
        request.setStartTime(LocalDateTime.of(2026, 4, 5, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 5, 10, 0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    // Test Case ID: TC-SS-029 - EXPECTED FAIL/Bug exposed: service currently allows double-booked interviewer schedules.
    @DisplayName("TC-SS-029 - EXPECTED FAIL - createSchedule() should reject interviewer who is already busy")
    void tc_ss_029_createSchedule_whenInterviewerAlreadyBusy_shouldThrowAndNotSave() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Busy interviewer");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(101L);
        request.setUserIds(List.of(201L));
        request.setStartTime(LocalDateTime.of(2026, 4, 5, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 5, 10, 0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () -> scheduleService.createSchedule(request));
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    // Test Case ID: TC-SS-030 - EXPECTED FAIL/Bug exposed: service currently throws NullPointerException for null sortOrder.
    @DisplayName("TC-SS-030 - EXPECTED FAIL - getAllSchedules() should not crash when sortOrder is null")
    void tc_ss_030_getAllSchedules_whenSortOrderNull_shouldUseDefaultSort() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> scheduleService.getAllSchedules(
                1, 10, "startTime", null, null, null, null, null, null, null, null));
    }

    @Test
    // Test Case ID: TC-SS-031 - createSchedule() candidate false branch and default notification text.
    @DisplayName("TC-SS-031 - createSchedule() handles null candidate title startTime and location")
    void tc_ss_031_createSchedule_whenCandidateAndOptionalTextFieldsNull_shouldSaveAndNotifyWithFallbackText() {
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle(null);
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(null);
        request.setUserIds(List.of(331L));
        request.setStartTime(null);
        request.setLocation(null);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schedule result = scheduleService.createSchedule(request);

        assertTrue(result.getParticipants().stream().noneMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(331L)), anyString(), anyString(), any());
    }

    @Test
    // Test Case ID: TC-SS-032 - updateSchedule() userIds false branch should skip notification.
    @DisplayName("TC-SS-032 - updateSchedule() skips notification when userIds is null")
    void tc_ss_032_updateSchedule_whenUserIdsNull_shouldSaveWithoutNotification() {
        Schedule existing = buildSchedule(332L, "Existing");
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("No users");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setUserIds(null);
        when(scheduleRepository.findById(332L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(existing)).thenReturn(existing);

        Schedule result = scheduleService.updateSchedule(332L, request);

        assertEquals("No users", result.getTitle());
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
    }

    @Test
    // Test Case ID: TC-SS-033 - getScheduleWithParticipantNames() null participant branch.
    @DisplayName("TC-SS-033 - getScheduleWithParticipantNames() returns empty participants when schedule participants is null")
    void tc_ss_033_getScheduleWithParticipantNames_whenParticipantsNull_returnsEmptyParticipantList() {
        Schedule schedule = buildSchedule(333L, "No participants");
        schedule.setParticipants(null);
        when(scheduleRepository.findById(333L)).thenReturn(Optional.of(schedule));

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(333L, "token");

        assertTrue(result.getParticipants().isEmpty());
        verify(userService, never()).getEmployeeNames(anyList(), anyString());
        verify(candidateService, never()).getCandidateNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-SS-034 - getScheduleWithParticipantNames() remote non-2xx branches.
    @DisplayName("TC-SS-034 - getScheduleWithParticipantNames() uses Unknown when name services return non success")
    void tc_ss_034_getScheduleWithParticipantNames_whenNameServicesFail_shouldUseUnknownNames() {
        Schedule schedule = buildSchedule(334L, "Remote names fail");
        ScheduleParticipant user = new ScheduleParticipant();
        user.setParticipantType("USER");
        user.setParticipantId(3341L);
        ScheduleParticipant candidate = new ScheduleParticipant();
        candidate.setParticipantType("CANDIDATE");
        candidate.setParticipantId(3342L);
        schedule.getParticipants().add(user);
        schedule.getParticipants().add(candidate);
        when(scheduleRepository.findById(334L)).thenReturn(Optional.of(schedule));
        when(userService.getEmployeeNames(anyList(), eq("token"))).thenReturn(ResponseEntity.badRequest().build());
        when(candidateService.getCandidateNames(anyList(), eq("token"))).thenReturn(ResponseEntity.ok(null));

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(334L, "token");

        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().allMatch(p -> "Unknown".equals(p.getName())));
    }

    @Test
    // Test Case ID: TC-SS-035 - getAllSchedules() limit lower-bound and participant false branch.
    @DisplayName("TC-SS-035 - getAllSchedules() normalizes limit below one and ignores incomplete participant filter")
    void tc_ss_035_getAllSchedules_whenLimitBelowOneAndParticipantTypeMissing_shouldReturnAllPage() {
        Schedule schedule = buildSchedule(335L, "All");
        when(scheduleRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        PaginationDTO result = scheduleService.getAllSchedules(
                1, 0, "startTime", "desc", null, null, null, null, null, 335L, null);

        assertEquals(10, result.getMeta().getPageSize());
        assertEquals(1L, result.getMeta().getTotal());
    }

    @Test
    // Test Case ID: TC-SS-036 - getAllSchedules() participant filter no-match branch.
    @DisplayName("TC-SS-036 - getAllSchedules() participant filter returns empty page when no participant matches")
    void tc_ss_036_getAllSchedules_whenParticipantDoesNotMatch_shouldReturnEmptyPage() {
        Schedule schedule = buildSchedule(336L, "Participant mismatch");
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantId(1L);
        participant.setParticipantType("USER");
        schedule.getParticipants().add(participant);
        when(scheduleRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        PaginationDTO result = scheduleService.getAllSchedules(
                1, 10, "startTime", "desc", null, null, null, null, null, 999L, "USER");

        assertTrue(((List<?>) result.getResult()).isEmpty());
    }

    @Test
    // Test Case ID: TC-SS-037 - getSchedulesDetailed() should query by single day.
    @DisplayName("TC-SS-037 - getSchedulesDetailed() queries schedules by day")
    void tc_ss_037_getSchedulesDetailed_whenDayProvided_shouldQueryDayRange() {
        Schedule schedule = buildSchedule(337L, "Day");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                LocalDate.of(2026, 5, 4), null, null, null, null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-038 - getSchedulesDetailed() should query by week.
    @DisplayName("TC-SS-038 - getSchedulesDetailed() queries schedules by week and year")
    void tc_ss_038_getSchedulesDetailed_whenWeekAndYearProvided_shouldQueryWeekRange() {
        Schedule schedule = buildSchedule(338L, "Week");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, 20, null, 2026, null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-039 - getSchedulesDetailed() should query by month.
    @DisplayName("TC-SS-039 - getSchedulesDetailed() queries schedules by month and year")
    void tc_ss_039_getSchedulesDetailed_whenMonthAndYearProvided_shouldQueryMonthRange() {
        Schedule schedule = buildSchedule(339L, "Month");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, 5, 2026, null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-040 - getSchedulesDetailed() should query by year.
    @DisplayName("TC-SS-040 - getSchedulesDetailed() queries schedules by year")
    void tc_ss_040_getSchedulesDetailed_whenYearProvided_shouldQueryYearRange() {
        Schedule schedule = buildSchedule(340L, "Year");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, null, 2026, null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-041 - getSchedulesDetailed() resolves candidate participant names.
    @DisplayName("TC-SS-041 - getSchedulesDetailed() resolves candidate participant names")
    void tc_ss_041_getSchedulesDetailed_whenCandidateParticipantsExist_shouldResolveCandidateNames() {
        Schedule withCandidate = buildSchedule(341L, "Candidate");
        ScheduleParticipant candidate = new ScheduleParticipant();
        candidate.setParticipantType("CANDIDATE");
        candidate.setParticipantId(3410L);
        withCandidate.getParticipants().add(candidate);
        Schedule nullParticipants = buildSchedule(342L, "Null participants");
        nullParticipants.setParticipants(null);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("3410", "Candidate Name");
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(List.of(withCandidate, nullParticipants));
        when(candidateService.getCandidateNames(anyList(), eq("token"))).thenReturn(ResponseEntity.ok(body));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, null, null, null, null, null, null, "token", null, null);

        assertEquals(2, result.size());
        assertEquals("Candidate Name", result.get(0).getParticipants().get(0).getName());
    }

    @Test
    // Test Case ID: TC-SS-042 - getAvailableParticipants() no overlap branch and unknown fallback fields.
    @DisplayName("TC-SS-042 - getAvailableParticipants() returns available employees with Unknown fallback fields")
    void tc_ss_042_getAvailableParticipants_whenNoOverlapAndMissingNameFields_shouldUseUnknownFallbacks() {
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(420L));
        when(scheduleRepository.findOverlappingSchedules(any(), any(), isNull())).thenReturn(List.of());
        ObjectNode body = objectMapper.createObjectNode();
        body.set("420", objectMapper.createObjectNode());
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(420L), "token")).thenReturn(ResponseEntity.ok(body));

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");

        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).getName());
        assertEquals("Unknown", result.get(0).getDepartmentName());
    }

    @Test
    // Test Case ID: TC-SS-043 - statistics date range with status only.
    @DisplayName("TC-SS-043 - getSchedulesForStatistics() filters date range by status only")
    void tc_ss_043_getSchedulesForStatistics_whenDateRangeAndStatusOnly_shouldFilterByStatus() {
        Schedule match = buildSchedule(343L, "Done");
        match.setStatus("DONE");
        Schedule other = buildSchedule(344L, "Scheduled");
        other.setStatus("SCHEDULED");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));

        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "DONE", null);

        assertEquals(1, result.size());
        assertEquals("DONE", result.get(0).getStatus());
    }

    @Test
    // Test Case ID: TC-SS-044 - statistics date range with meeting type only.
    @DisplayName("TC-SS-044 - getSchedulesForStatistics() filters date range by meeting type only")
    void tc_ss_044_getSchedulesForStatistics_whenDateRangeAndMeetingTypeOnly_shouldFilterByMeetingType() {
        Schedule match = buildSchedule(345L, "Interview");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(346L, "Meeting");
        other.setMeetingType(MeetingType.MEETING);
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));

        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null, "INTERVIEW");

        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getMeetingType());
    }

    @Test
    // Test Case ID: TC-SS-045 - statistics date range with no secondary filters.
    @DisplayName("TC-SS-045 - getSchedulesForStatistics() returns date range schedules when no secondary filters exist")
    void tc_ss_045_getSchedulesForStatistics_whenDateRangeOnly_shouldReturnRangeSchedules() {
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(buildSchedule(347L, "Range")));

        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null, null);

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-046 - statistics status-only branch with meeting type post-filter.
    @DisplayName("TC-SS-046 - getSchedulesForStatistics() filters status query by meeting type")
    void tc_ss_046_getSchedulesForStatistics_whenStatusAndMeetingTypeWithoutDate_shouldFilterRepositoryPage() {
        Schedule match = buildSchedule(348L, "Status interview");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(349L, "Status meeting");
        other.setMeetingType(MeetingType.MEETING);
        when(scheduleRepository.findByStatus(eq("DONE"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(match, other), PageRequest.of(0, 10), 2));

        var result = scheduleService.getSchedulesForStatistics(null, null, "DONE", "INTERVIEW");

        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getMeetingType());
    }

    @Test
    // Test Case ID: TC-SS-047 - statistics meeting type only branch.
    @DisplayName("TC-SS-047 - getSchedulesForStatistics() queries by meeting type only")
    void tc_ss_047_getSchedulesForStatistics_whenMeetingTypeOnly_shouldUseMeetingTypeRepository() {
        when(scheduleRepository.findByMeetingType(eq("INTERVIEW"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(buildSchedule(350L, "Meeting type")), PageRequest.of(0, 10), 1));

        var result = scheduleService.getSchedulesForStatistics(null, null, null, "INTERVIEW");

        assertEquals(1, result.size());
    }

    @Test
    // Test Case ID: TC-SS-048 - statistics should map null meeting type to null.
    @DisplayName("TC-SS-048 - getSchedulesForStatistics() maps null meetingType to null in DTO")
    void tc_ss_048_getSchedulesForStatistics_whenScheduleMeetingTypeNull_shouldReturnNullMeetingType() {
        Schedule schedule = buildSchedule(351L, "No meeting type");
        schedule.setMeetingType(null);
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(List.of(schedule));

        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals(null, result.get(0).getMeetingType());
    }

    @Test
    // Test Case ID: TC-SS-049 - getScheduleWithParticipantNames() remote success with null body branch.
    @DisplayName("TC-SS-049 - getScheduleWithParticipantNames() uses Unknown when user-service body is null")
    void tc_ss_049_getScheduleWithParticipantNames_whenUserServiceBodyIsNull_shouldUseUnknownName() {
        Schedule schedule = buildSchedule(352L, "Null body names");
        ScheduleParticipant userParticipant = new ScheduleParticipant();
        userParticipant.setParticipantType("USER");
        userParticipant.setParticipantId(3520L);
        userParticipant.setResponseStatus("PENDING");
        userParticipant.setSchedule(schedule);
        schedule.getParticipants().add(userParticipant);

        when(scheduleRepository.findById(352L)).thenReturn(Optional.of(schedule));
        when(userService.getEmployeeNames(List.of(3520L), "token")).thenReturn(ResponseEntity.ok(null));

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(352L, "token");

        assertEquals(1, result.getParticipants().size());
        assertEquals("Unknown", result.getParticipants().get(0).getName());
        verify(scheduleRepository).findById(352L);
        verify(userService).getEmployeeNames(List.of(3520L), "token");
        verify(candidateService, never()).getCandidateNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-SS-050 - getAvailableParticipants() remote name response false branch.
    @DisplayName("TC-SS-050 - getAvailableParticipants() uses Unknown when name-service body is null")
    void tc_ss_050_getAvailableParticipants_whenNameServiceBodyIsNull_shouldReturnUnknownFields() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 10, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 10, 10, 0);
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(5001L));
        when(scheduleRepository.findOverlappingSchedules(startTime, endTime, null)).thenReturn(List.of());
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(5001L), "token"))
                .thenReturn(ResponseEntity.ok(null));

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(startTime, endTime, null,
                "token");

        assertEquals(1, result.size());
        assertEquals(5001L, result.get(0).getId());
        assertEquals("Unknown", result.get(0).getName());
        assertEquals("Unknown", result.get(0).getDepartmentName());
        verify(scheduleRepository).findOverlappingSchedules(startTime, endTime, null);
        verify(userService).getEmployeeNamesAndDepartmentNames(List.of(5001L), "token");
    }

    @Test
    // Test Case ID: TC-SS-051 - statistics all branch should truncate records over 10000.
    @DisplayName("TC-SS-051 - getSchedulesForStatistics() truncates all schedules to first 10000 records")
    void tc_ss_051_getSchedulesForStatistics_whenAllSchedulesExceedLimit_shouldReturnFirst10000Only() {
        List<Schedule> schedules = new java.util.ArrayList<>();
        for (long id = 1; id <= 10001; id++) {
            schedules.add(buildSchedule(id, "Schedule " + id));
        }
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(schedules);

        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);

        assertEquals(10000, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10000L, result.get(9999).getId());
        verify(scheduleRepository).findAll(any(Sort.class));
    }

}
