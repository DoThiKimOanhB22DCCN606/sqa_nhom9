package com.example.schedule_service.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Cấu hình Repository trả về danh sách chứa lịch hẹn hợp lệ cần được hoàn thành.
        Schedule schedule = buildSchedule(1L, "Interview");
        when(scheduleRepository.findSchedulesToComplete(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt tiến trình tự động quét và cập nhật trạng thái lịch hẹn.
        scheduleStatusUpdateService.updateScheduleStatuses();

        // Assert: Xác minh tính toàn vẹn của trạng thái thực thể (Data Integrity)
        // Khẳng định trạng thái lịch hẹn bắt buộc phải chuyển sang dịch thái kết thúc "DONE".
        assertEquals("DONE", schedule.getStatus());
        
        // CheckDB: Xác minh cơ sở dữ liệu có cập nhật thay đổi đúng theo yêu cầu hay không
        // Kiểm tra xem thực thể có trạng thái mới đã được gọi lưu xuống DB qua hàm saveAll hay chưa.
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-002 - status update false branch.
    @DisplayName("TC-SSUS-002 - updateScheduleStatuses() does not save when no schedules need update")
    void tc_ssus_002_updateScheduleStatuses_whenNoSchedulesToComplete_doesNotSave() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Mô phỏng kịch bản hệ sinh thái sạch: Không có bất kỳ lịch hẹn nào cần xử lý hoàn thành (Danh sách trống).
        when(scheduleRepository.findSchedulesToComplete(any(LocalDateTime.class))).thenReturn(List.of());

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Khởi chạy tiến trình cập nhật trạng thái tự động.
        scheduleStatusUpdateService.updateScheduleStatuses();

        // Assert & CheckDB: Xác minh hệ thống tối ưu hóa tài nguyên, không gọi xuống DB
        // Đảm bảo tuyệt đối không có thao tác ghi đè hoặc gọi hàm saveAll xuống cơ sở dữ liệu khi không phát sinh thay đổi.
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-003 - reminder empty repository branch.
    @DisplayName("TC-SSUS-003 - sendReminderNotifications() returns when repository finds no reminders")
    void tc_ssus_003_sendReminderNotifications_whenNoPotentialReminders_doesNothing() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập kịch bản hệ thống tại thời điểm hiện tại không quét được lịch hẹn nào cần gửi nhắc nhở.
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of());

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Triển khai quét gửi thông báo nhắc nhở tự động.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert: Xác minh hệ thống phòng vệ hành vi gửi tin nhắn rác
        // Khẳng định không có bất kỳ tin nhắn nào được bắn sang hệ thống Message Broker phụ trách Notification.
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        
        // CheckDB: Đảm bảo tính toàn vẹn dữ liệu gốc
        // Xác nhận không phát sinh lệnh saveAll cập nhật thừa thãi xuống DB.
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-004 - reminder guard branch for missing reminderTime.
    @DisplayName("TC-SSUS-004 - sendReminderNotifications() skips schedules with null reminderTime")
    void tc_ssus_004_sendReminderNotifications_whenReminderTimeNull_skipsSchedule() {
        
        // Arrange: Thiết lập kịch bản dữ liệu khuyết thiếu biên (Guard Clause Boundary)
        // Khởi tạo một lịch hẹn bị mất cấu hình thời gian nhắc nhở (reminderTime = null).
        Schedule schedule = buildSchedule(4L, "No reminder time");
        schedule.setReminderTime(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt tiến trình quét gửi thông báo nhắc nhở.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert: Xác minh tính ổn định, tránh xử lý dữ liệu lỗi của hệ thống
        // Khẳng định cờ nhắc nhở reminderSent không được phép chuyển sang trạng thái true.
        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        
        // CheckDB & Messaging: Đảm bảo bỏ qua đối tượng lỗi hoàn toàn
        // Chắc chắn rằng không gửi notification và không thực hiện đồng bộ lưu dữ liệu bẩn xuống DB.
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-005 - reminder guard branch for missing startTime.
    @DisplayName("TC-SSUS-005 - sendReminderNotifications() skips schedules with null startTime")
    void tc_ssus_005_sendReminderNotifications_whenStartTimeNull_skipsSchedule() {
        
        // Arrange: Thiết lập kịch bản dữ liệu lỗi nghiêm trọng
        // Khởi tạo lịch hẹn thiếu hoàn toàn thời gian bắt đầu (startTime = null).
        Schedule schedule = buildSchedule(5L, "No start time");
        schedule.setStartTime(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Chạy tiến trình xử lý thông báo.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert & CheckDB: Khẳng định cơ chế phòng vệ (Robustness) của mã nguồn hoạt động tốt
        // Xác nhận cờ trạng thái gửi nhắc nhở không đổi và DB hoàn toàn được bảo vệ khỏi hành vi cập nhật bản ghi lỗi.
        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-006 - not-yet-due branch.
    @DisplayName("TC-SSUS-006 - sendReminderNotifications() skips schedules outside reminder window")
    void tc_ssus_006_sendReminderNotifications_whenReminderNotDue_doesNotSend() {
        
        // Arrange: Thiết lập dữ liệu kiểm thử nằm ngoài khoảng thời gian hiệu lực
        // Đặt lịch hẹn diễn ra ở tương lai xa (3 tiếng sau), vượt xa cấu hình thời gian nhắc nhở (10 phút).
        Schedule schedule = buildSchedule(6L, "Future reminder");
        schedule.setStartTime(LocalDateTime.now().plusHours(3));
        schedule.setReminderTime(10);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Chạy tiến trình kiểm tra nhắc nhở định kỳ.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert & CheckDB: Xác minh nghiệp vụ lọc thời gian chính xác
        // Hệ thống bắt buộc phải bỏ qua lịch hẹn chưa đến giờ: Không đổi trạng thái cờ, không phát tin, không ghi DB.
        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    // Test Case ID: TC-SSUS-007 - CheckDB reminder success path with USER participants.
    @DisplayName("TC-SSUS-007 - sendReminderNotifications() sends reminder to USER participants and saves state")
    void tc_ssus_007_sendReminderNotifications_whenDueWithUserParticipants_sendsAndMarksReminderSent() {
        
        // Arrange: Thiết lập kịch bản nghiệp vụ thành công tiêu chuẩn (Happy Path)
        // Thiết lập lịch hẹn đến hạn nhắc nhở, chứa cả thành viên thuộc hệ thống (USER) và ứng viên ngoài (CANDIDATE).
        Schedule schedule = buildSchedule(7L, "Due reminder");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 701L));
        schedule.getParticipants().add(participant("CANDIDATE", 801L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt phát thông báo nhắc nhở.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert Core Logic: Xác nhận cập nhật trạng thái thực thể thành công
        // Trạng thái nhắc nhở của thực thể bắt buộc phải chuyển sang true để tránh gửi lặp ở chu kỳ sau.
        assertTrue(schedule.getReminderSent());
        
        // Messaging Assert: Xác minh nghiệp vụ lọc đối tượng gửi tin nhắn
        // Hệ thống phải lọc chính xác và chỉ gửi tin nhắn cho USER (ID: 701L), tuyệt đối bỏ qua ứng viên (CANDIDATE).
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(701L)), anyString(), anyString(), isNull());
        
        // CheckDB: Xác minh dữ liệu trạng thái mới được đồng bộ xuống DB thành công
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-008 - no participant branch still marks reminder as sent.
    @DisplayName("TC-SSUS-008 - sendReminderNotifications() marks sent without notification when no USER participants exist")
    void tc_ssus_008_sendReminderNotifications_whenNoUserParticipants_marksSentWithoutSending() {
        
        // Arrange: Thiết lập dữ liệu kiểm thử biên về mặt phân quyền đối tượng
        // Lịch hẹn đến hạn nhưng danh sách tham gia khuyết thiếu, hoàn toàn không có ai thuộc nhóm "USER", chỉ có "CANDIDATE".
        Schedule schedule = buildSchedule(8L, "No users");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("CANDIDATE", 801L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Chạy tiến trình xử lý thông báo.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert Core Logic: Đảm bảo tiến trình bao quát toàn diện vòng đời (Lifecycle)
        // Thực thể vẫn phải đánh dấu cờ reminderSent = true để giải phóng phiên xử lý, tránh nghẽn hàng đợi quét.
        assertTrue(schedule.getReminderSent());
        
        // Messaging Assert: Đảm bảo không phát tán thông tin sai đối tượng cấu hình
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        
        // CheckDB: Ghi nhận trạng thái hoàn tất xử lý xuống DB
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-009 - null participants branch still marks reminder as sent.
    @DisplayName("TC-SSUS-009 - sendReminderNotifications() marks sent when participants collection is null")
    void tc_ssus_009_sendReminderNotifications_whenParticipantsNull_marksSentWithoutSending() {
        
        // Arrange: Thiết lập kịch bản biên phá hủy cấu trúc dữ liệu tập hợp
        // Đối tượng lịch hẹn đến hạn quét nhưng danh sách người tham gia bị null hoàn toàn thay vì rỗng.
        Schedule schedule = buildSchedule(9L, "Null participants");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.setParticipants(null);
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Chạy ứng dụng quét nhắc nhở tự động.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert: Xác minh khả năng chống chịu lỗi (Null-Pointer Safety)
        // Hệ thống không được phép crash và phải tự giải phóng bản ghi bằng việc nâng cờ trạng thái xử lý nhắc nhở thành true.
        assertTrue(schedule.getReminderSent());
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        
        // CheckDB: Kiểm tra lệnh đồng bộ hóa lưu xuống DB
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-010 - notification fail path is caught by service.
    @DisplayName("TC-SSUS-010 - sendReminderNotifications() catches notification failure and still saves processed schedules")
    void tc_ssus_010_sendReminderNotifications_whenProducerThrows_doesNotCrashAndSaves() {
        
        // Arrange: Giả lập kịch bản lỗi ngoại vi/hạ tầng mạng (Fault Tolerance Testing)
        // Tạo lịch hẹn hợp lệ, cấu hình ép Message Broker ném ra lỗi RuntimeException (Ví dụ: sự cố sập Kafka).
        Schedule schedule = buildSchedule(10L, "Producer fails");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1001L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));
        doThrow(new RuntimeException("kafka down")).when(notificationProducer)
                .sendNotificationToMultiple(anyList(), anyString(), anyString(), any());

        // Act & Assert (Fault Tolerance): Khẳng định lỗi ngoại vi không được làm tê liệt hệ thống chính
        // Ứng dụng bắt buộc phải bắt biệt lệ (Try-catch nội bộ) nhằm đảm bảo tiến trình tổng thể không bị chết đứng đột ngột.
        assertDoesNotThrow(() -> scheduleStatusUpdateService.sendReminderNotifications());

        // Assert State: Kiểm tra trạng thái nghiệp vụ khi gặp sự cố gửi thông điệp
        // Cờ nhắc nhở không được phép đổi sang true vì đối tượng đích thực tế chưa thể tiếp nhận tin nhắn thành công.
        assertFalse(Boolean.TRUE.equals(schedule.getReminderSent()));
        
        // CheckDB: Xác minh cơ chế cô lập lỗi
        // Hệ thống vẫn phải gọi hàm lưu trạng thái hiện tại xuống DB để bảo toàn dữ liệu cho các luồng xử lý song song khác.
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-011 - EXPECTED FAIL/Bug exposed: started schedules are not moved to IN_PROGRESS.
    @DisplayName("TC-SSUS-011 - EXPECTED FAIL - updateScheduleStatuses() should move started SCHEDULED schedules to IN_PROGRESS")
    void tc_ssus_011_updateScheduleStatuses_whenScheduleHasStarted_shouldMarkInProgressAndSave() {
        
        // Arrange: Thiết lập dữ liệu phơi bày lỗ hổng logic nghiệp vụ (Bug Detection)
        // Tạo lịch hẹn có thời gian bắt đầu nằm ở quá khứ (-5 phút) và chưa kết thúc, trạng thái gốc vẫn là SCHEDULED.
        Schedule schedule = buildSchedule(11L, "Started interview");
        schedule.setStatus("SCHEDULED");
        schedule.setStartTime(LocalDateTime.now().minusMinutes(5));
        schedule.setEndTime(LocalDateTime.now().plusMinutes(30));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Gọi tiến trình tự động quét cập nhật trạng thái lịch hẹn theo thời gian thực.
        scheduleStatusUpdateService.updateScheduleStatuses();

        // Assert Core Logic (Kỳ vọng Fail do Code Dev thiếu Logic):
        // Khẳng định đúng chuẩn nghiệp vụ: Lịch đã chạy bắt buộc phải chuyển trạng thái sang dạng đang diễn ra "IN_PROGRESS".
        assertEquals("IN_PROGRESS", schedule.getStatus());
        
        // CheckDB: Đảm bảo cập nhật trạng thái mới xuống cơ sở dữ liệu
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-012 - USER participant with null id should not receive reminder.
    @DisplayName("TC-SSUS-012 - sendReminderNotifications() ignores USER participants with null id")
    void tc_ssus_012_sendReminderNotifications_whenUserParticipantIdNull_marksSentWithoutSending() {
        
        // Arrange: Thiết lập kịch bản dữ liệu bẩn do lỗi ràng buộc ứng dụng
        // Lịch hẹn hợp lệ nhưng đối tượng USER bên trong lại bị khuyết thiếu mã định danh duy nhất (Id = null).
        Schedule schedule = buildSchedule(12L, "Null participant id");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", null));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt chạy hàm nghiệp vụ nhắc nhở.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert Core Logic: Xác minh luồng chạy khép kín an toàn
        // Cờ nhắc nhở vẫn phải chuyển sang trạng thái true để đóng phiên làm việc cho thực thể này.
        assertTrue(schedule.getReminderSent());
        
        // Messaging Assert: Chặn đứng hành vi lỗi khi đẩy tin nhắn sang Broker
        // Hệ thống tuyệt đối không được phát đi thông báo rác khi danh sách đích không có ID người nhận hợp lệ.
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        
        // CheckDB: Ghi nhận đồng bộ dữ liệu sau xử lý vệ sinh an toàn dữ liệu
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-013 - lower reminder window boundary should still send.
    @DisplayName("TC-SSUS-013 - sendReminderNotifications() sends reminder at lower boundary minus one minute")
    void tc_ssus_013_sendReminderNotifications_whenReminderTimeIsOneMinutePast_shouldStillSend() {
        
        // Arrange: Thiết lập kiểm thử giá trị biên thời gian khắt khe (Boundary Value Analysis)
        // Đặt lịch hẹn có khoảng cách thời gian thực tế so với hiện tại bị hụt mất 1 phút (9 phút) so với cấu hình nhắc nhở (10 phút).
        Schedule schedule = buildSchedule(13L, "Boundary reminder");
        schedule.setStartTime(LocalDateTime.now().plusMinutes(9));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1301L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Chạy tiến trình xử lý thông báo nhắc nhở tự động.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert Core Logic: Xác thực tính đúng đắn khi xử lý vùng biên
        // Hệ thống thông minh phải nhận diện được đối tượng nằm sát biên dưới và đánh dấu hoàn thành gửi tin thành công.
        assertTrue(schedule.getReminderSent());
        
        // Messaging & CheckDB Assert: Xác minh hành vi gửi tin nhắn thành công và lưu DB dữ liệu thay đổi
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(1301L)), anyString(), anyString(), isNull());
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

    @Test
    // Test Case ID: TC-SSUS-014 - optional reminder text fields false branches.
    @DisplayName("TC-SSUS-014 - sendReminderNotifications() builds reminder message without null title or location text")
    void tc_ssus_014_sendReminderNotifications_whenOptionalTextFieldsNull_shouldNotAppendNullText() {
        
        // Arrange: Thiết lập kịch bản biên chuỗi ký tự (String Concatenation Integrity)
        // Khởi tạo lịch hẹn có các trường thông tin hiển thị tùy chọn (Title, Location) bị null nhằm ép hệ thống xử lý ghép chuỗi trống.
        Schedule schedule = buildSchedule(14L, "Optional text");
        schedule.setTitle(null);
        schedule.setLocation(null);
        schedule.setStartTime(LocalDateTime.now().plusMinutes(10));
        schedule.setReminderTime(10);
        schedule.getParticipants().add(participant("USER", 1401L));
        when(scheduleRepository.findSchedulesForReminder(any(LocalDateTime.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Thực thi tiến trình sinh nội dung tin nhắn và phát thông báo nhắc nhở.
        scheduleStatusUpdateService.sendReminderNotifications();

        // Assert (Message Extraction): Sử dụng kĩ thuật đánh chặn dữ liệu (ArgumentCaptor)
        // Khởi tạo Captor để capture và trích xuất chuỗi nội dung tin nhắn thực tế mà Service đã xây dựng.
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(1401L)), anyString(),
                messageCaptor.capture(), isNull());
                
        // Assert Core Logic: Xác minh chất lượng hiển thị chuỗi thông tin (UX/Data Cleanliness)
        // 1. Cờ trạng thái xử lý nhắc nhở bắt buộc phải bật lên hoàn thành (true).
        assertTrue(schedule.getReminderSent());
        // 2. Nội dung tin nhắn gửi đi tuyệt đối không được chứa chữ "null" (lỗi hiển thị thô chuỗi thô của Dev).
        assertFalse(messageCaptor.getValue().contains("null"));
        
        // CheckDB: Xác minh bản ghi thay đổi trạng thái được lưu xuống cơ sở dữ liệu đúng yêu cầu
        verify(scheduleRepository).saveAll(List.of(schedule));
    }

}
