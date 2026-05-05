package com.example.schedule_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.schedule_service.messaging.NotificationProducer;
import com.example.schedule_service.model.Schedule;
import com.example.schedule_service.model.ScheduleParticipant;
import com.example.schedule_service.repository.ScheduleRepository;
import com.example.schedule_service.utils.enums.MeetingType;

/**
 * Unit test for ScheduleStatusUpdateService.
 * CheckDB: ScheduleRepository is mocked as the DB boundary; status/reminder
 * tests assert changed entity state and saveAll calls.
 * Rollback: no real database is touched, so Mockito resets all in-memory test
 * data after each method.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleStatusUpdateServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private ScheduleStatusUpdateService scheduleStatusUpdateService;

    private Schedule buildSchedule(Long id, String title) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setTitle(title);
        schedule.setStatus("SCHEDULED");
        schedule.setMeetingType(MeetingType.INTERVIEW);
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setEndTime(LocalDateTime.now().plusMinutes(60));
        schedule.setLocation("Room A");
        schedule.setReminderTime(10);
        schedule.setReminderSent(false);
        schedule.setParticipants(new HashSet<>());
        return schedule;
    }

    private ScheduleParticipant participant(String type, Long participantId) {
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantType(type);
        participant.setParticipantId(participantId);
        participant.setResponseStatus("PENDING");
        return participant;
    }

    @Test
    // Test Case ID: TC-SSUS-001 - CheckDB status update true branch.
    @DisplayName("TC-SSUS-001 - updateScheduleStatuses() marks schedules as DONE and saves them")
    void tc_ssus_001_updateScheduleStatuses_whenSchedulesToCompleteExist_marksDoneAndSaves() {
        Schedule schedule = buildSchedule(1L, "Interview");
        when(scheduleRepository.findSchedulesToComplete(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.updateScheduleStatuses();

        assertEquals("DONE", schedule.getStatus());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-002 - status update false branch.
    @DisplayName("TC-SSUS-002 - updateScheduleStatuses() does not save when no schedules need update")
    void tc_ssus_002_updateScheduleStatuses_whenNoSchedulesToComplete_doesNotSave() {
        when(scheduleRepository.findSchedulesToComplete(any(LocalDateTime.class))).thenReturn(List.of());

        scheduleStatusUpdateService.updateScheduleStatuses();

        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-003 - reminder empty repository branch.
    @DisplayName("TC-SSUS-003 - sendReminderNotifications() returns when repository finds no reminders")
    void tc_ssus_003_sendReminderNotifications_whenNoPotentialReminders_doesNothing() {
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of());

        scheduleStatusUpdateService.sendReminderNotifications();

        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-004 - reminder guard branch for missing reminderTime.
    @DisplayName("TC-SSUS-004 - sendReminderNotifications() skips schedules with null reminderTime")
    void tc_ssus_004_sendReminderNotifications_whenReminderTimeNull_skipsSchedule() {
        Schedule schedule = buildSchedule(4L, "No reminder time");
        schedule.setReminderTime(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-005 - reminder guard branch for missing startTime.
    @DisplayName("TC-SSUS-005 - sendReminderNotifications() skips schedules with null startTime")
    void tc_ssus_005_sendReminderNotifications_whenStartTimeNull_skipsSchedule() {
        Schedule schedule = buildSchedule(5L, "No start time");
        schedule.setStartTime(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-006 - not-yet-due branch.
    @DisplayName("TC-SSUS-006 - sendReminderNotifications() skips schedules outside reminder window")
    void tc_ssus_006_sendReminderNotifications_whenReminderNotDue_doesNotSend() {
        Schedule schedule = buildSchedule(6L, "Future reminder");
        schedule.setStartTime(LocalDateTime.now().plusHours(3));
        schedule.setReminderTime(10);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-007 - CheckDB reminder success path with USER participants.
    @DisplayName("TC-SSUS-007 - sendReminderNotifications() sends reminder to USER participants and saves state")
    void tc_ssus_007_sendReminderNotifications_whenDueWithUserParticipants_sendsAndMarksReminderSent() {
        Schedule schedule = buildSchedule(7L, "Due reminder");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 701L));
        schedule.getParticipants().add(participant("CANDIDATE", 801L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertTrue(schedule.getReminderSent());
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(701L)), anyString(), anyString(), isNull());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-008 - no participant branch still marks reminder as sent.
    @DisplayName("TC-SSUS-008 - sendReminderNotifications() marks sent without notification when no USER participants exist")
    void tc_ssus_008_sendReminderNotifications_whenNoUserParticipants_marksSentWithoutSending() {
        Schedule schedule = buildSchedule(8L, "No users");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("CANDIDATE", 801L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertTrue(schedule.getReminderSent());
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-009 - null participants branch still marks reminder as sent.
    @DisplayName("TC-SSUS-009 - sendReminderNotifications() marks sent when participants collection is null")
    void tc_ssus_009_sendReminderNotifications_whenParticipantsNull_marksSentWithoutSending() {
        Schedule schedule = buildSchedule(9L, "Null participants");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.setParticipants(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertTrue(schedule.getReminderSent());
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-010 - notification fail path is caught by service.
    @DisplayName("TC-SSUS-010 - sendReminderNotifications() catches notification failure and still saves processed schedules")
    void tc_ssus_010_sendReminderNotifications_whenProducerThrows_doesNotCrashAndSaves() {
        Schedule schedule = buildSchedule(10L, "Producer fails");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1001L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));
        doThrow(new RuntimeException("kafka down")).when(notificationProducer)
                .sendNotificationToMultiple(anyList(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> scheduleStatusUpdateService.sendReminderNotifications());

        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-011 - EXPECTED FAIL/Bug exposed: started schedules are not moved to IN_PROGRESS.
    @DisplayName("TC-SSUS-011 - EXPECTED FAIL - updateScheduleStatuses() should move started SCHEDULED schedules to IN_PROGRESS")
    void tc_ssus_011_updateScheduleStatuses_whenScheduleHasStarted_shouldMarkInProgressAndSave() {
        Schedule schedule = buildSchedule(11L, "Started interview");
        schedule.setStatus("SCHEDULED");
        schedule.setStartTime(LocalDateTime.now().minusMinutes(5));
        schedule.setEndTime(LocalDateTime.now().plusMinutes(30));

        scheduleStatusUpdateService.updateScheduleStatuses();

        assertEquals("IN_PROGRESS", schedule.getStatus());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-012 - USER participant with null id should not receive reminder.
    @DisplayName("TC-SSUS-012 - sendReminderNotifications() ignores USER participants with null id")
    void tc_ssus_012_sendReminderNotifications_whenUserParticipantIdNull_marksSentWithoutSending() {
        Schedule schedule = buildSchedule(12L, "Null participant id");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", null));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertTrue(schedule.getReminderSent());
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-013 - lower reminder window boundary should still send.
    @DisplayName("TC-SSUS-013 - sendReminderNotifications() sends reminder at lower boundary minus one minute")
    void tc_ssus_013_sendReminderNotifications_whenReminderTimeIsOneMinutePast_shouldStillSend() {
        Schedule schedule = buildSchedule(13L, "Boundary reminder");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(9));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1301L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        assertTrue(schedule.getReminderSent());
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(1301L)), anyString(), anyString(), isNull());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-014 - optional reminder text fields false branches.
    @DisplayName("TC-SSUS-014 - sendReminderNotifications() builds reminder message without null title or location text")
    void tc_ssus_014_sendReminderNotifications_whenOptionalTextFieldsNull_shouldNotAppendNullText() {
        Schedule schedule = buildSchedule(14L, "Optional text");
        schedule.setTitle(null);
        schedule.setLocation(null);
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1401L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        scheduleStatusUpdateService.sendReminderNotifications();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(1401L)), anyString(),
                messageCaptor.capture(), isNull());
        assertTrue(schedule.getReminderSent());
        assertFalse(messageCaptor.getValue().contains("null"));
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

}
