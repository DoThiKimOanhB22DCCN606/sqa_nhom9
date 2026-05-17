package com.example.schedule_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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
import org.mockito.Mockito;
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
//tạo nhanh một đối tượng Schedule có sẵn các giá trị mặc định chuẩn (như phỏng vấn Online qua Zoom vào một khung giờ cố định)
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

    // Rollback: Đảm bảo sau khi thực hiện test, DB (ảo) quay về trạng thái data như TRƯỚC khi test.
    @AfterEach
    void tearDown() {
        // Reset lại toàn bộ hành vi và lịch sử gọi (interactions) của các mock object
        Mockito.reset(scheduleRepository, userService, candidateService, notificationProducer);
    }
    
    @Test

    // Test Case ID: TC-SS-001 - createSchedule() should save schedule and send notifications to users

    @DisplayName("TC-SS-001 - createSchedule() should save schedule and send notifications to users")
    void tc_ss_001_createSchedule_whenInputIsValid_shouldSaveAndNotifyUsers() {
        //CreateScheduleDTO request giả lập client gửi lên
        //điền vào một cái Form (DTO) gửi lên mạng để tạo lịch. Form này ghi rõ: Có 1 Ứng viên và 2 Nhân viên.
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

        //Giả lập Database (Mocking) vì không có DB thật
        //Này Service, cứ gọi hàm save đi, tao sẽ tóm lấy cái lịch mày gửi (getArgument(0)), tự tay gán cho nó cái Id = 1L rồi trả ngược lại cho mày, coi như đã lưu vào DB thành công!
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });

        //Thực thi hàm thật
        Schedule result = scheduleService.createSchedule(request);
        //Xác nhận xem kết quả trả ra có chứa cái ID bằng 1L mà DB giả đã cấp phát hay không.
        assertEquals(1L, result.getId());
        assertEquals(3, result.getParticipants().size());
        //Kiểm tra trong danh sách người tham gia có tồn tại loại "CANDIDATE" hay không.
        assertTrue(result.getParticipants().stream().anyMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        //ArgumentCaptor đóng vai trò như một cái Camera an ninh. Nó rình sẵn ở cửa ngõ tầng Database. 
        //Khi Service gọi lệnh lưu, nó sẽ "chụp" và "tóm" lấy cái thực thể Schedule thật sự đang chuẩn bị chui vào DB (lưu vào biến persisted) để Tester lôi ra kiểm tra xem các thông tin bên trong có bị sai lệch không.
        verify(scheduleRepository, atLeastOnce()).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getAllValues().get(scheduleCaptor.getAllValues().size() - 1);
        assertEquals("Interview session", persisted.getTitle());
        assertEquals("SCHEDULED", persisted.getStatus());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "CANDIDATE".equals(p.getParticipantType()) && Long.valueOf(100L).equals(p.getParticipantId())));
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "USER".equals(p.getParticipantType()) && Long.valueOf(200L).equals(p.getParticipantId())));
        //Mày phải gọi hàm sendNotificationToMultiple gửi tới đúng 2 ông 200, 201 này chính xác 1 lần
        verify(notificationProducer, times(1)).sendNotificationToMultiple(eq(List.of(200L, 201L)), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-002 - updateSchedule() should rebuild participants and send update notification
    @DisplayName("TC-SS-002 - updateSchedule() should rebuild participants and send update notification")
    void tc_ss_002_updateSchedule_whenInputIsValid_shouldUpdateParticipantsAndNotify() {
        // Chuẩn bị dữ liệu lịch cũ (existing) đang nằm trong DB
        // Lịch cũ có ID = 2L, hiện đang có 1 người phỏng vấn (USER) mang ID = 300L
        Schedule existing = buildSchedule(2L, "Original title");
        ScheduleParticipant existingParticipant = new ScheduleParticipant();
        existingParticipant.setId(50L);
        existingParticipant.setParticipantType("USER");
        existingParticipant.setParticipantId(300L);
        existingParticipant.setResponseStatus("PENDING");
        existingParticipant.setSchedule(existing);
        existing.getParticipants().add(existingParticipant);
        // CreateScheduleDTO request giả lập client gửi lên để Cập nhật
        // Form này yêu cầu đổi tên lịch, đổi địa điểm và thay người phỏng vấn mới là ID = 400L.
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
        
        // Giả lập Database (Mocking)
        // Này DB giả, nếu service hỏi tìm lịch ID = 2L, mày trả về cái lịch cũ (existing) cho tao.
        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(existing));
        // Còn khi gọi hàm lưu, cứ trả về nguyên vẹn đối tượng truyền vào.
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Thực thi hàm thật
        Schedule result = scheduleService.updateSchedule(2L, request);

        // Xác nhận kết quả đầu ra
        assertEquals("Updated interview", result.getTitle());
        assertEquals(1, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> p.getParticipantId().equals(400L)));
        // Dùng ArgumentCaptor tịch thu thực thể trước khi lưu vào DB để tra khảo
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getValue();
        assertEquals("Updated interview", persisted.getTitle());
        assertEquals("OFFLINE", persisted.getFormat());
        assertEquals("Office", persisted.getLocation());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "USER".equals(p.getParticipantType()) && Long.valueOf(400L).equals(p.getParticipantId())));
        assertTrue(persisted.getParticipants().stream().noneMatch(p -> Long.valueOf(300L).equals(p.getParticipantId())));
        // Hệ thống phải gọi hàm gửi thông báo đến đúng 1 người mới là ông 400L.
        verify(notificationProducer, times(1)).sendNotificationToMultiple(eq(List.of(400L)), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-003 - deleteSchedule() should delete schedule when exists

    @DisplayName("TC-SS-003 - deleteSchedule() should delete schedule when exists")
    void tc_ss_003_deleteSchedule_whenScheduleExists_shouldDelete() {
        // Giả lập DB báo cáo rằng lịch mang ID = 3L CÓ tồn tại.
        when(scheduleRepository.existsById(3L)).thenReturn(true);

        scheduleService.deleteSchedule(3L);
        // Xác nhận xem Service có ra lệnh cho DB gọi hàm deleteById(3L) chính xác 1 lần không.
        verify(scheduleRepository, times(1)).deleteById(3L);
    }

    @Test

    // Test Case ID: TC-SS-004 - deleteSchedule() should throw RuntimeException when schedule missing

    @DisplayName("TC-SS-004 - deleteSchedule() should throw RuntimeException when schedule missing")
    void tc_ss_004_deleteSchedule_whenScheduleMissing_shouldThrowRuntimeException() {
        // Giả lập DB báo cáo rằng lịch mang ID = 4L KHÔNG tồn tại.
        when(scheduleRepository.existsById(4L)).thenReturn(false);
        // Thực thi hàm thật và Xác nhận hệ thống phải ném ra lỗi RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> scheduleService.deleteSchedule(4L));
        // Kiểm tra xem trong câu thông báo lỗi có chứa cái ID bị sai (4L) để user biết không.
        assertTrue(exception.getMessage().contains("4"));

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        // Hệ thống phải kiểm tra sự tồn tại trong DB và TUYỆT ĐỐI không gọi lệnh deleteById khi ID không hợp lệ.
        verify(scheduleRepository, times(1)).existsById(4L);
        verify(scheduleRepository, never()).deleteById(any());
    }

    @Test

    // Test Case ID: TC-SS-005 - getScheduleById() should return schedule when found

    @DisplayName("TC-SS-005 - getScheduleById() should return schedule when found")
    void tc_ss_005_getScheduleById_whenFound_shouldReturnSchedule() {
        // Chuẩn bị dữ liệu mẫu
        Schedule schedule = buildSchedule(5L, "Session");
        // Giả lập DB trả về đúng object mẫu khi được yêu cầu tìm ID 5L.
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));

        Schedule result = scheduleService.getScheduleById(5L);
        // Xác nhận thông tin trả về khớp với data đã giả lập
        assertEquals(5L, result.getId());
        assertEquals("Session", result.getTitle());
        // Xác minh Service có thực sự tương tác với DB giả lập hay không.
        verify(scheduleRepository).findById(5L);
    }

    @Test

    // Test Case ID: TC-SS-006 - getScheduleById() should throw RuntimeException when not found

    @DisplayName("TC-SS-006 - getScheduleById() should throw RuntimeException when not found")
    void tc_ss_006_getScheduleById_whenNotFound_shouldThrowRuntimeException() {
        // Giả lập DB trả về trống rỗng khi tìm lịch số 6L.
        when(scheduleRepository.findById(6L)).thenReturn(Optional.empty());
        // Thực thi và Xác nhận hàm phải văng lỗi, đồng thời chứa ID số 6 trong câu báo lỗi.
        RuntimeException exception = assertThrows(RuntimeException.class, () -> scheduleService.getScheduleById(6L));
        assertTrue(exception.getMessage().contains("6"));

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findById(6L);
    }

    @Test

    // Test Case ID: TC-SS-007 - getScheduleWithParticipantNames() should build participant names for users and candidates

    @DisplayName("TC-SS-007 - getScheduleWithParticipantNames() should build participant names for users and candidates")
    void tc_ss_007_getScheduleWithParticipantNames_whenParticipantsExist_shouldReturnDetailDTOWithNames() {
        // Chuẩn bị lịch mẫu ID = 7L, chứa sẵn ID của Nhân viên (701L) và Ứng viên (801L).
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

        // Giả lập DB trả về lịch mẫu này.
        when(scheduleRepository.findById(7L)).thenReturn(Optional.of(schedule));
        // Giả lập API gọi sang UserService: Ép nó trả về JSON { "701": "Employee One" }
        ObjectNode userNames = objectMapper.createObjectNode();
        userNames.put("701", "Employee One");
        when(userService.getEmployeeNames(List.of(701L), "token")).thenReturn(ResponseEntity.ok(userNames));
        // Giả lập API gọi sang CandidateService: Ép nó trả về JSON { "801": "Candidate One" }
        ObjectNode candidateNames = objectMapper.createObjectNode();
        candidateNames.put("801", "Candidate One");
        when(candidateService.getCandidateNames(List.of(801L), "token")).thenReturn(ResponseEntity.ok(candidateNames));

        // Thực thi hàm thật
        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(7L, "token");
        // Xác nhận logic code đã lấy được cục JSON giả lập và gán tên thành công vào DTO trả về.
        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> "Employee One".equals(p.getName())));
        assertTrue(result.getParticipants().stream().anyMatch(p -> "Candidate One".equals(p.getName())));
        verify(scheduleRepository).findById(7L);
    }

    @Test

    // Test Case ID: TC-SS-008 - createSchedule() should default status and skip notification when userIds are null

    @DisplayName("TC-SS-008 - createSchedule() should default status and skip notification when userIds are null")
    void tc_ss_008_createSchedule_whenStatusAndUsersMissing_shouldDefaultStatusAndSkipNotification() {
        // Form gửi lên bị thiết sót dữ liệu: Không có Status, Không có User (người phỏng vấn)
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("No users");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(808L);
        // Giả lập DB lưu vào sẽ cấp phát ID là 308L
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            saved.setId(308L);
            return saved;
        });

        Schedule result = scheduleService.createSchedule(request);

        // Xác nhận code hệ thống tự điền Status mặc định là "SCHEDULED".
        assertEquals("SCHEDULED", result.getStatus());
        assertEquals(1, result.getParticipants().size());
        // Xác minh hệ thống đã lưu xuống DB
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository, atLeastOnce()).save(scheduleCaptor.capture());
        Schedule persisted = scheduleCaptor.getAllValues().get(scheduleCaptor.getAllValues().size() - 1);
        assertEquals("SCHEDULED", persisted.getStatus());
        assertTrue(persisted.getParticipants().stream()
                .anyMatch(p -> "CANDIDATE".equals(p.getParticipantType()) && Long.valueOf(808L).equals(p.getParticipantId())));
        // Cực kỳ quan trọng: Vì userIds truyền lên = null, hệ thống tuyệt đối KHÔNG ĐƯỢC gọi hàm gửi thông báo (never).
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
    }

    @Test

    // Test Case ID: TC-SS-009 - updateSchedule() should throw RuntimeException when schedule is missing

    @DisplayName("TC-SS-009 - updateSchedule() should throw RuntimeException when schedule is missing")
    void tc_ss_009_updateSchedule_whenMissing_shouldThrowRuntimeException() {
        // Giả lập DB không tìm thấy lịch cần update (ID = 309L)
        when(scheduleRepository.findById(309L)).thenReturn(Optional.empty());
        // Thực thi & Xác nhận văng lỗi
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.updateSchedule(309L, new CreateScheduleDTO()));

        assertTrue(exception.getMessage().contains("309"));
        // Xác nhận hệ thống đã chặn thành công, tuyệt đối không được gọi lệnh lưu DB (never).
        verify(scheduleRepository, never()).save(any());
    }

    @Test

    // Test Case ID: TC-SS-010 - updateSchedule() should initialize participants when existing collection is null
//nhằm đảm bảo hệ thống tự khởi tạo lại vùng nhớ và không bị sập lỗi hệ thống (NullPointerException) khi gặp dữ liệu cũ bị hỏng dữ liệu (bị null).
    @DisplayName("TC-SS-010 - updateSchedule() should initialize participants when existing collection is null")
    void tc_ss_010_updateSchedule_whenParticipantsNull_shouldInitializeAndSaveParticipants() {
        // Chuẩn bị dữ liệu lịch cũ nhưng cố tình gán danh sách participants = null (Lỗi dữ liệu ngầm)
        Schedule existing = buildSchedule(310L, "Original");
        existing.setParticipants(null);
        // Form gửi lên yêu cầu update người phỏng vấn và ứng viên mới
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Updated");
        request.setMeetingType(MeetingType.MEETING);
        request.setCandidateId(910L);
        request.setUserIds(List.of(911L));
        // Giả lập DB
        when(scheduleRepository.findById(310L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(existing)).thenReturn(existing);

        Schedule result = scheduleService.updateSchedule(310L, request);
        // Xác nhận Code không bị văng NullPointerException mà tự thông minh tạo ra danh sách mới và nhét 2 ông mới vào
        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().stream().anyMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        assertTrue(result.getParticipants().stream().anyMatch(p -> Long.valueOf(911L).equals(p.getParticipantId())));
        //capture xem dữ liệu tống xuống DB chuẩn chưa
        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        verify(scheduleRepository, times(1)).findById(310L);
        verify(scheduleRepository, times(1)).save(scheduleCaptor.capture());
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
        // Tạo một cái lịch giả với ID = 311L
        Schedule schedule = buildSchedule(311L, "Unknown names");
        ScheduleParticipant user = new ScheduleParticipant();
        user.setId(1L);
        user.setParticipantType("USER");
        user.setParticipantId(1L);
        user.setSchedule(schedule);
        schedule.getParticipants().add(user);
        // Giả lập Database: Khi Service đi tìm lịch số 311, vứt cho nó cái lịch giả vừa tạo ở trên
        when(scheduleRepository.findById(311L)).thenReturn(Optional.of(schedule));
        // Giả lập External Service (API lấy tên nhân sự bị sập):
        // Này userService, khi người ta hỏi tên của ông có ID là 1, mày cứ trả về lỗi 500 (Server Error) cho tao để test hệ thống xử lý lỗi.
        when(userService.getEmployeeNames(List.of(1L), "token")).thenReturn(ResponseEntity.status(500).build());

        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(311L, "token");
        // Khi API bị lỗi, hệ thống không được phép "chết" (crash), mà phải tự động gán tên người tham gia là "Unknown".
        assertEquals("Unknown", result.getParticipants().get(0).getName());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findById(311L);
    }

    @Test

    // Test Case ID: TC-SS-012 - getAllSchedules() should query by specific date when date provided

    @DisplayName("TC-SS-012 - getAllSchedules() should query by specific date when date provided")
    void tc_ss_012_getAllSchedules_whenDateProvided_shouldQueryBetweenDayBounds() {
        Schedule schedule = buildSchedule(312L, "Date");
        // Giả lập DB trả về 1 trang (Page) chứa 1 cái lịch duy nhất khi tìm kiếm trong khoảng thời gian
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));
        // Thực thi hàm lấy danh sách lịch, truyền vào cụ thể ngày 04/05/2026
        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "asc",
                LocalDate.of(2026, 5, 4), null, null, null, null, null, null);
        // Kiểm tra xem Service có bọc đúng list lịch vào kết quả trả về không
        assertEquals(1, ((List<?>) result.getResult()).size());
        assertEquals(1, result.getMeta().getPage());
        // Cảnh sát "verify" xác nhận: Mày có thật sự gọi hàm findByStartTimeBetween của Repository để query theo khoảng thời gian trong ngày hay không?
        verify(scheduleRepository).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class));
    }


    @Test
    // Test Case ID: TC-SS-013 - getAllSchedules() should query by month when year and month provided
    @DisplayName("TC-SS-013 - getAllSchedules() should query by month when year and month provided")
    void tc_ss_013_getAllSchedules_whenYearAndMonthProvided_shouldQueryBetweenMonthBounds() {
        // Giả lập Database (Mocking): Bất kể Service truyền vào khoảng thời gian hay kiểu phân trang nào (any),
        // cứ vứt trả lại một trang trống (List rỗng, tổng số = 0) để test kịch bản "Không tìm thấy lịch nào trong khoảng thời gian này".
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // Truyền vào Tháng 5, Năm 2026
        PaginationDTO result = scheduleService.getAllSchedules(0, 0, "startTime", "desc",
                null, 2026, 5, null, null, null, null);

        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());

        // Khai báo 2 cái bẫy để tóm lấy thời gian Bắt đầu và Kết thúc
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // Bắt buộc Service phải gọi hàm này, và "tóm" luôn 2 cái biến thời gian nó truyền vào
        verify(scheduleRepository).findByStartTimeBetween(
                startCaptor.capture(), 
                endCaptor.capture(), 
                any(org.springframework.data.domain.Pageable.class)
        );

        // Lấy 2 biến vừa tóm được ra để khám xét: 
        LocalDateTime queryStart = startCaptor.getValue();
        LocalDateTime queryEnd = endCaptor.getValue();

        // Xác nhận xem Service có khôn tới mức tự tính được: Ngày bắt đầu phải là 1/5/2026
        assertEquals(2026, queryStart.getYear());
        assertEquals(5, queryStart.getMonthValue());
        assertEquals(1, queryStart.getDayOfMonth());

        // Xác nhận xem ngày kết thúc có đúng là nằm trong tháng 5 hay không (vd: 31/5/2026)
        assertEquals(2026, queryEnd.getYear());
        assertEquals(5, queryEnd.getMonthValue());
    }

    @Test

    // Test Case ID: TC-SS-014 - getAllSchedules() should query by status when status provided

    @DisplayName("TC-SS-014 - getAllSchedules() should query by status when status provided")
    void tc_ss_014_getAllSchedules_whenStatusProvided_shouldQueryByStatus() {
        Schedule schedule = buildSchedule(314L, "Status");
        // Mocking: Trả về cái lịch này nếu bị truy vấn với status = "DONE"
        when(scheduleRepository.findByStatus(eq("DONE"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));
        // Truyền thẳng status "DONE" vào parameter thứ 8 để test tính năng lọc
        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, "DONE", null, null, null);
        // Đảm bảo tổng số lượng tìm thấy là 1
        assertEquals(1, result.getMeta().getTotal());
        // Xác minh xem Service có ném đúng từ khóa "DONE" xuống DB để tìm hay không, hay nó lại ném nhầm từ khác!
        verify(scheduleRepository).findByStatus(eq("DONE"), any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-015 - getAllSchedules() should query by meetingType when meetingType provided

    @DisplayName("TC-SS-015 - getAllSchedules() should query by meetingType when meetingType provided")
    void tc_ss_015_getAllSchedules_whenMeetingTypeProvided_shouldQueryByMeetingType() {
        // Tương tự lọc theo trạng thái, nhưng áp dụng cho loại lịch (INTERVIEW)
        when(scheduleRepository.findByMeetingType(eq("INTERVIEW"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, null, "INTERVIEW", null, null);
        // Trả về rỗng do Mocking ở trên set list rỗng (Total = 0)
        assertEquals(0, result.getMeta().getTotal());
        // Xác minh xem Service có ném đúng từ khóa "INTERVIEW" xuống DB để tìm hay không, hay nó lại ném nhầm từ khác!
        verify(scheduleRepository).findByMeetingType(eq("INTERVIEW"), any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-016 - getAllSchedules() should filter returned page by participant when participant filter is provided

    @DisplayName("TC-SS-016 - getAllSchedules() should filter returned page by participant when participant filter is provided")
    void tc_ss_016_getAllSchedules_whenParticipantFilterProvided_shouldKeepOnlyMatchingSchedules() {
        // Tạo ra 2 cái lịch:
        // - Lịch 1 ("matching"): Có ông USER 3160L tham gia
        Schedule matching = buildSchedule(316L, "Matching");
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantType("USER");
        participant.setParticipantId(3160L);
        matching.getParticipants().add(participant);

        // - Lịch 2 ("other"): Ông USER 3160L KHÔNG tham gia
        Schedule other = buildSchedule(317L, "Other");
        // DB cứ thế trả về hết cả 2 lịch (chưa lọc)
        when(scheduleRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(matching, other), PageRequest.of(0, 10), 2));
        // Gọi hàm, bắt Service tự lấy rây ra mà lọc bằng code Java: Chỉ lấy lịch của ông 3160L
        PaginationDTO result = scheduleService.getAllSchedules(1, 10, "startTime", "desc",
                null, null, null, null, null, 3160L, "USER");
        // Dù DB trả ra 2, nhưng Service lọc xong phải cón đúng 1 lịch của ông 3160L
        assertEquals(1, ((List<?>) result.getResult()).size());
        assertEquals(1, result.getMeta().getTotal());
        // Xác nhận rõ ràng: Service đã gọi đúng hàm `findAll` (cào tất cả dữ liệu từ DB lên) rồi mới mang về tự lọc trên RAM.
        // Đảm bảo logic code không chạy lung tung sang các hàm tìm kiếm query khác.
        verify(scheduleRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test

    // Test Case ID: TC-SS-017 - updateScheduleStatus() should update status and save schedule

    @DisplayName("TC-SS-017 - updateScheduleStatus() should update status and save schedule")
    void tc_ss_017_updateScheduleStatus_whenFound_shouldPersistNewStatus() {
        Schedule schedule = buildSchedule(317L, "Status");
        // Tìm thấy lịch trong DB
        when(scheduleRepository.findById(317L)).thenReturn(Optional.of(schedule));
        // Cứ gọi hàm save thì sẽ tự động trả lại đúng object đó
        when(scheduleRepository.save(schedule)).thenReturn(schedule);
        // Đổi trạng thái lịch sang "DONE"
        Schedule result = scheduleService.updateScheduleStatus(317L, "DONE");
        // Phải chắc chắn trạng thái đã được chuyển sang DONE
        assertEquals("DONE", result.getStatus());
        // Cảnh sát "verify": Bắt buộc phải có thao tác lưu (save) cập nhật xuống DB, nếu không thì mất dữ liệu!
        verify(scheduleRepository).save(schedule);
    }

    @Test

    // Test Case ID: TC-SS-018 - updateScheduleStatus() should throw RuntimeException when schedule missing

    @DisplayName("TC-SS-018 - updateScheduleStatus() should throw RuntimeException when schedule missing")
    void tc_ss_018_updateScheduleStatus_whenMissing_shouldThrowRuntimeException() {
        // Ép DB giả vờ KHÔNG TÌM THẤY LỊCH (Optional.empty())
        when(scheduleRepository.findById(318L)).thenReturn(Optional.empty());
        // Nếu lịch không có thật mà vẫn đòi update, Service phải "gắt" lên và ném lỗi RuntimeException.
        // Dùng assertThrows để chặn và kiểm chứng việc ném lỗi này. Nếu không ném ra lỗi => Test fail.
        assertThrows(RuntimeException.class, () -> scheduleService.updateScheduleStatus(318L, "DONE"));
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        // Đảm bảo hệ thống kiểm tra sự tồn tại và TUYỆT ĐỐI không gọi lệnh lưu DB khi thực thể trống.
        verify(scheduleRepository, times(1)).findById(318L);
        verify(scheduleRepository, never()).save(any());
    }

    @Test

    // Test Case ID: TC-SS-019 - getSchedulesDetailed() should query by date range and filter status meetingType participant

    @DisplayName("TC-SS-019 - getSchedulesDetailed() should query by date range and filter status meetingType participant")
    void tc_ss_019_getSchedulesDetailed_whenAllFiltersProvided_shouldReturnMatchingDetailedSchedules() {
        // Trường hợp test full-giáp: Cung cấp mọi bộ lọc có thể có (Ngày, status, meetingType, người tham gia)
        Schedule matching = buildSchedule(319L, "Matching");
        matching.setStatus("SCHEDULED");
        matching.setMeetingType(MeetingType.INTERVIEW);
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantType("USER");
        participant.setParticipantId(3190L);
        matching.getParticipants().add(participant);

        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(matching));
        // Giả lập External Service lấy tên thành công (trả ra JSON báo ông 3190 tên là "Interviewer")
        ObjectNode names = objectMapper.createObjectNode();
        names.put("3190", "Interviewer");
        when(userService.getEmployeeNames(List.of(3190L), "token")).thenReturn(ResponseEntity.ok(names));
        // Thực thi với cả lố filter
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(null, null, null, null,
                "scheduled", "INTERVIEW", 3190L, "USER", "token", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        // Trả ra đúng 1 người
        assertEquals(1, result.size());
        // Phải verify xem API móc nối tên có làm việc tốt không, có nhét đúng tên "Interviewer" vào DTO trả ra hay không
        assertEquals("Interviewer", result.get(0).getParticipants().get(0).getName());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test

    // Test Case ID: TC-SS-020 - getSchedulesDetailed() should query all schedules when no date filter exists

    @DisplayName("TC-SS-020 - getSchedulesDetailed() should query all schedules when no date filter exists")
    void tc_ss_020_getSchedulesDetailed_whenNoDateFilter_shouldQueryAllSorted() {
        // Không truyền filter gì sất, yêu cầu Service cào mẻ lưới lớn lấy bằng hết lịch trong DB (findAll)
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(List.of(buildSchedule(320L, "All")));

        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(null, null, null, null,
                null, null, null, null, "token", null, null);

        assertEquals(1, result.size());
        // Cảnh sát "verify": Xác nhận là mày đã xài hàm findAll (chứ không phải hàm find theo range ngày).
        verify(scheduleRepository).findAll(any(Sort.class));
    }

    @Test

    // Test Case ID: TC-SS-021 - getAvailableParticipants() should return empty when user service has no employees

    @DisplayName("TC-SS-021 - getAvailableParticipants() should return empty when user service has no employees")
    void tc_ss_021_getAvailableParticipants_whenNoEmployees_shouldReturnEmptyList() {
        // Giả lập API công ty bị "mất sạch dữ liệu nhân viên", mảng ID trả về rỗng tuếch
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of());
        // Đi tìm người rảnh
        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");
        //Công ty không có ai thì lấy đâu ra ai rảnh? Trả về rỗng là cái chắc!
        assertTrue(result.isEmpty());
        // Tối ưu hóa: Khi đã biết công ty chả có ai, Service tuyệt đối KHÔNG ĐƯỢC query DB tìm lịch chồng chéo làm gì cho tốn tài nguyên. (never)
        verify(scheduleRepository, never()).findOverlappingSchedules(any(), any(), any());
    }

    @Test

    // Test Case ID: TC-SS-022 - getAvailableParticipants() should remove busy employees and enrich available names

    @DisplayName("TC-SS-022 - getAvailableParticipants() should remove busy employees and enrich available names")
    void tc_ss_022_getAvailableParticipants_whenSomeEmployeesBusy_shouldReturnOnlyAvailableEmployees() {
        Schedule busy = buildSchedule(322L, "Busy");
        // Công ty có 2 nhân viên: ID 1 và ID 2
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(1L, 2L));
        // Phát hiện có 1 cái lịch bận đang diễn ra trong thời gian này
        when(scheduleRepository.findOverlappingSchedules(any(), any(), eq(99L))).thenReturn(List.of(busy));
        // Ông nhân viên số 1 là người tham gia cái lịch bận đó
        when(scheduleParticipantRepository.findParticipantIdsByScheduleIds(List.of(322L))).thenReturn(List.of(1L));
        // Giả lập API thông tin cho người còn lại rảnh (Ông ID 2)
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode employee = objectMapper.createObjectNode();
        employee.put("name", "Available User");
        employee.put("departmentName", "QA");
        body.set("2", employee);
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(2L), "token")).thenReturn(ResponseEntity.ok(body));
        // Lấy danh sách người rảnh
        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), 99L, "token");
        // Ông 1 bận rồi thì loại ra, list chỉ còn ông số 2
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Available User", result.get(0).getName());
        assertEquals("QA", result.get(0).getDepartmentName());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findOverlappingSchedules(any(), any(), eq(99L));
        verify(scheduleParticipantRepository, times(1)).findParticipantIdsByScheduleIds(List.of(322L));
    }

    @Test

    // Test Case ID: TC-SS-023 - getAvailableParticipants() should return empty when all employees are busy

    @DisplayName("TC-SS-023 - getAvailableParticipants() should return empty when all employees are busy")
    void tc_ss_023_getAvailableParticipants_whenAllEmployeesBusy_shouldReturnEmptyList() {
        Schedule busy = buildSchedule(323L, "Busy");
        // Công ty chỉ có 1 nhân viên duy nhất (ID = 1L)
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(1L));
        when(scheduleRepository.findOverlappingSchedules(any(), any(), isNull())).thenReturn(List.of(busy));
        // Đen thay, nhân viên số 1 này lại đang dính lịch phỏng vấn (Busy)
        when(scheduleParticipantRepository.findParticipantIdsByScheduleIds(List.of(323L))).thenReturn(List.of(1L));

        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");
        // Loại ông 1 ra thì list sạch bách, kết quả trả về phải rỗng
        assertTrue(result.isEmpty());
    }

    @Test

    // Test Case ID: TC-SS-024 - getSchedulesForStatistics() should filter date range by status and meeting type

    @DisplayName("TC-SS-024 - getSchedulesForStatistics() should filter date range by status and meeting type")
    void tc_ss_024_getSchedulesForStatistics_whenDateStatusMeetingTypeProvided_shouldReturnMatchingStats() {
        // Tạo 2 lịch: Lịch Match (Phỏng vấn - INTERVIEW) và Lịch Other (Họp thường - MEETING)
        Schedule match = buildSchedule(324L, "Match");
        match.setStatus("DONE");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(325L, "Other");
        other.setStatus("DONE");
        other.setMeetingType(MeetingType.MEETING);
        // Cả 2 đều trả về nếu xét nguyên khoảng thời gian
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));
        // Nhưng ở tầng Service, yêu cầu xuất thống kê chỉ lấy các lịch DONE thuộc loại INTERVIEW
        var result = scheduleService.getSchedulesForStatistics(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                "DONE", "INTERVIEW");
        // Kết quả bị bế đi lọc, nên chỉ còn sót lại duy nhất 1 thằng (thằng Match)
        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getMeetingType());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test

    // Test Case ID: TC-SS-025 - getSchedulesForStatistics() should limit all schedules to first 10000 records

    @DisplayName("TC-SS-025 - getSchedulesForStatistics() should limit all schedules to first 10000 records")
    void tc_ss_025_getSchedulesForStatistics_whenMoreThanLimit_shouldReturnFirst10000() {
        // Dùng Stream API chế tạo nhanh 10.001 cái lịch trong Database để test giới hạn chịu tải
        List<Schedule> schedules = java.util.stream.IntStream.range(0, 10001)
                .mapToObj(i -> buildSchedule((long) i, "S" + i))
                .toList();
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(schedules);
        // Xuất thống kê, lấy tất cả không truyền filter gì
        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);
        // Chặn spam dữ liệu: Đảm bảo Service khôn khéo chặn đứng (truncate), chỉ cho xuất ra tối đa 10.000 dòng đầu để server khỏi treo.
        assertEquals(10000, result.size());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findAll(any(Sort.class));
    }

    @Test

    // Test Case ID: TC-SS-026 - getCandidateIdsByInterviewer() should return repository candidate ids

    @DisplayName("TC-SS-026 - getCandidateIdsByInterviewer() should return repository candidate ids")
    void tc_ss_026_getCandidateIdsByInterviewer_shouldReturnRepositoryResult() {
        // Giả lập: Ông phỏng vấn viên 326L đã từng phỏng vấn 2 ứng viên là 1L và 2L
        when(scheduleParticipantRepository.findCandidateIdsByInterviewer(326L)).thenReturn(List.of(1L, 2L));

        List<Long> result = scheduleService.getCandidateIdsByInterviewer(326L);
        // Đảm bảo Service truyền dữ liệu nguyên vẹn từ DB lên Controller
        assertEquals(List.of(1L, 2L), result);

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleParticipantRepository, times(1)).findCandidateIdsByInterviewer(326L);
    }

    @Test
    // Test Case ID: TC-SS-027 - EXPECTED FAIL/Bug exposed: service currently accepts endTime before startTime.
    @DisplayName("TC-SS-027 - EXPECTED FAIL - createSchedule() should reject endTime before startTime")
    void tc_ss_027_createSchedule_whenEndBeforeStart_shouldThrowAndNotSave() {
        // Test case đi bắt BUG nguy hiểm: Logic ngược đời!
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Invalid interview time");
        request.setMeetingType(MeetingType.INTERVIEW);
        // Thời gian bắt đầu: 10 giờ. Kết thúc lúc: 9 giờ rưỡi.
        request.setStartTime(LocalDateTime.of(2026, 4, 5, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 4, 5, 9, 30));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Phải đập ngay ngoại lệ (IllegalArgumentException), ngăn không cho tạo.
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createSchedule(request));
        // Quan trọng nhất: Không bao giờ được phép dính dáng tới DB! (never)
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("TC-SS-028 - createSchedule() should throw IllegalStateException when candidate is double-booked")
    void tc_ss_028_createSchedule_whenCandidateAlreadyHasSchedule_shouldThrowAndNotSave() {
        // Arrange: Khởi tạo dữ liệu request hợp lệ (Mock Data)
        LocalDateTime mockStartTime = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);
        LocalDateTime mockEndTime = mockStartTime.plusHours(1);
        // Arrange: Mocking hành vi của tầng Repository
        // Giả lập trạng thái ứng viên (CandidateId: 100L) đã có lịch trùng lặp trong hệ thống
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Interview setup for busy candidate");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(100L); 
        request.setUserIds(List.of(200L));
        request.setStartTime(mockStartTime);
        request.setEndTime(mockEndTime);
        
        // 2. Mocking Behavior: Giả lập luồng lưu trữ của Repository
        // Đảm bảo entity Schedule được gán ID = 1L sau khi thực thi phương thức save() để mô phỏng Database cấp phát khóa chính.
        Schedule existingSchedule = new Schedule();
        existingSchedule.setId(999L);
                when(scheduleRepository.findOverlappingSchedules(
                any(LocalDateTime.class), 
                any(LocalDateTime.class), 
                eq(100L)
        )).thenReturn(List.of(existingSchedule));

        // Act & Assert: Thực thi logic xếp lịch và kiểm chứng quy tắc nghiệp vụ (Business Rule)
        assertThrows(IllegalStateException.class, () -> scheduleService.createSchedule(request),
                "Lỗi Nghiệp Vụ: Hệ thống không được phép xếp lịch khi ứng viên đã có lịch trùng giờ!");

        // Verify: Kiểm chứng hiệu ứng phụ (Side-effects)
        // Đảm bảo tính toàn vẹn dữ liệu: Không thực hiện giao dịch lưu (save) khi Validation thất bại
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("TC-SS-029 - createSchedule() should throw IllegalStateException when interviewer is double-booked")
    void tc_ss_029_createSchedule_whenInterviewerAlreadyBusy_shouldThrowAndNotSave() {
        
        // Arrange: Khởi tạo dữ liệu request hợp lệ với thời gian tương lai
        LocalDateTime mockStartTime = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);
        
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("Busy interviewer scenario");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(101L);
        request.setUserIds(List.of(201L)); // Mục tiêu test: Phỏng vấn viên 201L
        request.setStartTime(mockStartTime);
        request.setEndTime(mockStartTime.plusHours(1));
        
        // Arrange: Mocking hành vi của tầng Repository
        // Bắt buộc Database phải nôn ra một cái lịch bận khi Service hỏi thăm ông 201L
        Schedule existingSchedule = new Schedule();
        existingSchedule.setId(888L);
        when(scheduleRepository.findOverlappingSchedules(
                any(LocalDateTime.class), 
                any(LocalDateTime.class), 
                eq(201L) // Gài bẫy đúng ID của phỏng vấn viên
        )).thenReturn(List.of(existingSchedule));

        // Act & Assert: Thực thi và kiểm chứng quy tắc nghiệp vụ
        assertThrows(IllegalStateException.class, () -> scheduleService.createSchedule(request),
                "Expected createSchedule() to throw IllegalStateException due to interviewer double-booking");

        // Verify: Đảm bảo tính toàn vẹn dữ liệu
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    // Test Case ID: TC-SS-030 - getAllSchedules() should apply default sorting direction when sortOrder is null
    @DisplayName("TC-SS-030 - getAllSchedules() should apply default sorting direction when sortOrder is null")
    void tc_ss_030_getAllSchedules_whenSortOrderNull_shouldUseDefaultSort() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Đảm bảo tầng Repository trả về một đối tượng Page rỗng hợp lệ để luồng Service tiếp tục chạy mà không bị sập.
        when(scheduleRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Truyền cố ý giá trị sortOrder = null để kích hoạt nhánh xử lý Default Fallback của hệ thống.
        PaginationDTO result = scheduleService.getAllSchedules(
                1, 10, "startTime", null, null, null, null, null, null, null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        org.junit.jupiter.api.Assertions.assertNotNull(result, "Hệ thống không được phép trả về null khi sortOrder là null");

        //Xác minh cơ sở dữ liệu có thay đổi/truy cập đúng theo yêu cầu hay không.
        // Sử dụng ArgumentCaptor để can thiệp và kiểm tra đối tượng Pageable trước khi nó được gửi xuống DB.
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor = 
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        
        verify(scheduleRepository, times(1)).findAll(pageableCaptor.capture());
        
        org.springframework.data.domain.Pageable capturedPageable = pageableCaptor.getValue();
        
        // Assert Core Logic: Xác nhận Dev đã code logic tự động gắn Default Sort (Ví dụ: DESCENDING) khi nhận giá trị null.
        org.junit.jupiter.api.Assertions.assertTrue(
                capturedPageable.getSort().getOrderFor("startTime").isDescending(),
                "Lỗi Nghiệp Vụ: Hệ thống phải tự động gán mặc định là sắp xếp Giảm Dần (DESC) khi không truyền sortOrder"
        );
    }

    @Test
    // Test Case ID: TC-SS-031 - createSchedule() candidate false branch and default notification text.
    @DisplayName("TC-SS-031 - createSchedule() handles null candidate title startTime and location")
    void tc_ss_031_createSchedule_whenCandidateAndOptionalTextFieldsNull_shouldSaveAndNotifyWithFallbackText() {
        // Arrange: Chuẩn bị dữ liệu đầu vào với các trường tùy chọn bị null
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle(null);
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setCandidateId(null);
        request.setUserIds(List.of(331L));
        request.setStartTime(null);
        request.setLocation(null);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Act: Gọi hàm tạo lịch trình
        Schedule result = scheduleService.createSchedule(request);
        // Assert: Xác minh logic và kết quả trả về
        assertTrue(result.getParticipants().stream().noneMatch(p -> "CANDIDATE".equals(p.getParticipantType())));
        verify(notificationProducer).sendNotificationToMultiple(eq(List.of(331L)), anyString(), anyString(), any());
        // CheckDB: BẮT BUỘC phải verify xem repository có thực sự gọi lệnh save() xuống DB để insert data không
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    // Test Case ID: TC-SS-032 - updateSchedule() userIds false branch should skip notification.
    @DisplayName("TC-SS-032 - updateSchedule() skips notification when userIds is null")
    void tc_ss_032_updateSchedule_whenUserIdsNull_shouldSaveWithoutNotification() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập lịch trình đã tồn tại dưới DB. Đẩy lên một DTO không có danh sách userIds để kiểm tra việc chặn Notification.
        Schedule existing = buildSchedule(332L, "Existing");
        CreateScheduleDTO request = new CreateScheduleDTO();
        request.setTitle("No users");
        request.setMeetingType(MeetingType.INTERVIEW);
        request.setUserIds(null);
        
        when(scheduleRepository.findById(332L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(existing)).thenReturn(existing);

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        Schedule result = scheduleService.updateSchedule(332L, request);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals("No users", result.getTitle(), "Lỗi: Title không được cập nhật đúng");
        
        // Xác minh ngoại vi và cơ sở dữ liệu: Hệ thống phải lưu data nhưng TUYỆT ĐỐI KHÔNG bắn notification.
        verify(notificationProducer, never()).sendNotificationToMultiple(anyList(), anyString(), anyString(), any());
        verify(scheduleRepository, times(1)).save(existing);
    }

    @Test
    // Test Case ID: TC-SS-033 - getScheduleWithParticipantNames() null participant branch.
    @DisplayName("TC-SS-033 - getScheduleWithParticipantNames() returns empty participants when schedule participants is null")
    void tc_ss_033_getScheduleWithParticipantNames_whenParticipantsNull_returnsEmptyParticipantList() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập entity Schedule gặp lỗi/thiếu data mảng participants (bằng null) từ DB.
        Schedule schedule = buildSchedule(333L, "No participants");
        schedule.setParticipants(null);
        when(scheduleRepository.findById(333L)).thenReturn(Optional.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(333L, "token");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Đảm bảo hệ thống tự động fallback về danh sách rỗng (Empty List) thay vì ném ra NullPointerException.
        assertTrue(result.getParticipants().isEmpty(), "Lỗi: Phải trả về danh sách participant rỗng thay vì lỗi");
        
        // Xác minh ngoại vi: Do không có người tham gia, hệ thống không được phép gọi qua các service trung tâm lấy tên.
        verify(userService, never()).getEmployeeNames(anyList(), anyString());
        verify(candidateService, never()).getCandidateNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-SS-034 - getScheduleWithParticipantNames() remote non-2xx branches.
    @DisplayName("TC-SS-034 - getScheduleWithParticipantNames() uses Unknown when name services return non success")
    void tc_ss_034_getScheduleWithParticipantNames_whenNameServicesFail_shouldUseUnknownNames() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập trạng thái Remote Services (User, Candidate) bị lỗi (Trả về BadRequest hoặc Null Body).
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
        // Lỗi 400 Bad Request từ UserService và trả về Ok nhưng body rỗng từ CandidateService.
        when(userService.getEmployeeNames(anyList(), eq("token"))).thenReturn(ResponseEntity.badRequest().build());
        when(candidateService.getCandidateNames(anyList(), eq("token"))).thenReturn(ResponseEntity.ok(null));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(334L, "token");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(2, result.getParticipants().size());
        // Nghiệp vụ: Phải fallback tên hiển thị thành "Unknown" để UI không bị vỡ.
        assertTrue(result.getParticipants().stream().allMatch(p -> "Unknown".equals(p.getName())), 
                "Lỗi Nghiệp Vụ: Không hiển thị 'Unknown' khi Remote Service phản hồi lỗi");
        
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findById(334L);
    }

    @Test
    // Test Case ID: TC-SS-035 - getAllSchedules() limit lower-bound and participant false branch.
    @DisplayName("TC-SS-035 - getAllSchedules() normalizes limit below one and ignores incomplete participant filter")
    void tc_ss_035_getAllSchedules_whenLimitBelowOneAndParticipantTypeMissing_shouldReturnAllPage() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(335L, "All");
        when(scheduleRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Cố ý truyền limit = 0 (dưới mức cho phép) và thiếu participantType.
        PaginationDTO result = scheduleService.getAllSchedules(
                1, 0, "startTime", "desc", null, null, null, null, null, 335L, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Hệ thống phải tự động chuẩn hoá limit (vd: lên 10) để bảo vệ DB.
        assertEquals(10, result.getMeta().getPageSize(), "Lỗi: Hệ thống không tự động chuẩn hoá limit sai quy định");
        assertEquals(1L, result.getMeta().getTotal());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    // Test Case ID: TC-SS-036 - getAllSchedules() participant filter no-match branch.
    @DisplayName("TC-SS-036 - getAllSchedules() participant filter returns empty page when no participant matches")
    void tc_ss_036_getAllSchedules_whenParticipantDoesNotMatch_shouldReturnEmptyPage() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(336L, "Participant mismatch");
        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setParticipantId(1L);
        participant.setParticipantType("USER");
        schedule.getParticipants().add(participant);
        
        when(scheduleRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(schedule), PageRequest.of(0, 10), 1));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Tìm kiếm với Participant ID = 999L (không tồn tại trong mock data).
        PaginationDTO result = scheduleService.getAllSchedules(
                1, 10, "startTime", "desc", null, null, null, null, null, 999L, "USER");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Phải trả về list rỗng do bộ lọc tại tầng Service đã loại bỏ kết quả không khớp.
        assertTrue(((List<?>) result.getResult()).isEmpty(), "Lỗi: Service không lọc đúng participant mismatch");
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    // Test Case ID: TC-SS-037 - getSchedulesDetailed() should query by single day.
    @DisplayName("TC-SS-037 - getSchedulesDetailed() queries schedules by day")
    void tc_ss_037_getSchedulesDetailed_whenDayProvided_shouldQueryDayRange() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(337L, "Day");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt logic truy vấn theo Ngày cụ thể.
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                LocalDate.of(2026, 5, 4), null, null, null, null, null, null, null, "token", null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-038 - getSchedulesDetailed() should query by week.
    @DisplayName("TC-SS-038 - getSchedulesDetailed() queries schedules by week and year")
    void tc_ss_038_getSchedulesDetailed_whenWeekAndYearProvided_shouldQueryWeekRange() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(338L, "Week");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt logic truy vấn theo Tuần và Năm.
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, 20, null, 2026, null, null, null, null, "token", null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-039 - getSchedulesDetailed() should query by month.
    @DisplayName("TC-SS-039 - getSchedulesDetailed() queries schedules by month and year")
    void tc_ss_039_getSchedulesDetailed_whenMonthAndYearProvided_shouldQueryMonthRange() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(339L, "Month");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt logic truy vấn theo Tháng và Năm.
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, 5, 2026, null, null, null, null, "token", null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-040 - getSchedulesDetailed() should query by year.
    @DisplayName("TC-SS-040 - getSchedulesDetailed() queries schedules by year")
    void tc_ss_040_getSchedulesDetailed_whenYearProvided_shouldQueryYearRange() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(340L, "Year");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Kích hoạt logic truy vấn chỉ theo Năm.
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, null, 2026, null, null, null, null, "token", null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-041 - getSchedulesDetailed() resolves candidate participant names.
    @DisplayName("TC-SS-041 - getSchedulesDetailed() resolves candidate participant names")
    void tc_ss_041_getSchedulesDetailed_whenCandidateParticipantsExist_shouldResolveCandidateNames() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập lịch trình chứa các participant là Candidate. Đảm bảo CandidateService trả về ánh xạ tên đúng.
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

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        List<ScheduleDetailDTO> result = scheduleService.getSchedulesDetailed(
                null, null, null, null, null, null, null, null, "token", null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(2, result.size());
        assertEquals("Candidate Name", result.get(0).getParticipants().get(0).getName(), 
                "Lỗi Nghiệp Vụ: Map sai tên Candidate từ Remote Service");
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-042 - getAvailableParticipants() no overlap branch and unknown fallback fields.
    @DisplayName("TC-SS-042 - getAvailableParticipants() returns available employees with Unknown fallback fields")
    void tc_ss_042_getAvailableParticipants_whenNoOverlapAndMissingNameFields_shouldUseUnknownFallbacks() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập hệ thống User rỗng/thiếu thông tin, xác nhận xử lý an toàn khi merge dữ liệu.
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(420L));
        when(scheduleRepository.findOverlappingSchedules(any(), any(), isNull())).thenReturn(List.of());
        
        ObjectNode body = objectMapper.createObjectNode();
        body.set("420", objectMapper.createObjectNode()); // Trả về object nhưng không có thông tin Tên/Phòng ban
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(420L), "token")).thenReturn(ResponseEntity.ok(body));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, "token");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).getName(), "Phải fallback thành Unknown khi Remote không trả về name");
        assertEquals("Unknown", result.get(0).getDepartmentName(), "Phải fallback thành Unknown khi không có phòng ban");
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findOverlappingSchedules(any(), any(), isNull());
    }

    @Test
    // Test Case ID: TC-SS-043 - statistics date range with status only.
    @DisplayName("TC-SS-043 - getSchedulesForStatistics() filters date range by status only")
    void tc_ss_043_getSchedulesForStatistics_whenDateRangeAndStatusOnly_shouldFilterByStatus() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule match = buildSchedule(343L, "Done");
        match.setStatus("DONE");
        Schedule other = buildSchedule(344L, "Scheduled");
        other.setStatus("SCHEDULED");
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Truy vấn Statistic trong khoảng thời gian, kèm theo bộ lọc trạng thái "DONE".
        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "DONE", null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Logic code phải tự lọc bỏ record "SCHEDULED".
        assertEquals(1, result.size(), "Lỗi: Không lọc đúng Status");
        assertEquals("DONE", result.get(0).getStatus());

        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-044 - statistics date range with meeting type only.
    @DisplayName("TC-SS-044 - getSchedulesForStatistics() filters date range by meeting type only")
    void tc_ss_044_getSchedulesForStatistics_whenDateRangeAndMeetingTypeOnly_shouldFilterByMeetingType() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule match = buildSchedule(345L, "Interview");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(346L, "Meeting");
        other.setMeetingType(MeetingType.MEETING);
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(match, other));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null, "INTERVIEW");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size(), "Lỗi: Không lọc đúng Meeting Type");
        assertEquals("INTERVIEW", result.get(0).getMeetingType());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-045 - statistics date range with no secondary filters.
    @DisplayName("TC-SS-045 - getSchedulesForStatistics() returns date range schedules when no secondary filters exist")
    void tc_ss_045_getSchedulesForStatistics_whenDateRangeOnly_shouldReturnRangeSchedules() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        when(scheduleRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(buildSchedule(347L, "Range")));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        var result = scheduleService.getSchedulesForStatistics(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-046 - statistics status-only branch with meeting type post-filter.
    @DisplayName("TC-SS-046 - getSchedulesForStatistics() filters status query by meeting type")
    void tc_ss_046_getSchedulesForStatistics_whenStatusAndMeetingTypeWithoutDate_shouldFilterRepositoryPage() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule match = buildSchedule(348L, "Status interview");
        match.setMeetingType(MeetingType.INTERVIEW);
        Schedule other = buildSchedule(349L, "Status meeting");
        other.setMeetingType(MeetingType.MEETING);
        
        when(scheduleRepository.findByStatus(eq("DONE"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(match, other), PageRequest.of(0, 10), 2));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        // Truyền cả hai tham số Status và MeetingType nhưng bỏ qua Date.
        var result = scheduleService.getSchedulesForStatistics(null, null, "DONE", "INTERVIEW");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Đảm bảo Post-filter trong memory hoạt động đúng để lọc MeetingType từ cục dữ liệu Status.
        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getMeetingType());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByStatus(eq("DONE"), any(PageRequest.class));
    }

    @Test
    // Test Case ID: TC-SS-047 - statistics meeting type only branch.
    @DisplayName("TC-SS-047 - getSchedulesForStatistics() queries by meeting type only")
    void tc_ss_047_getSchedulesForStatistics_whenMeetingTypeOnly_shouldUseMeetingTypeRepository() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        when(scheduleRepository.findByMeetingType(eq("INTERVIEW"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(buildSchedule(350L, "Meeting type")), PageRequest.of(0, 10), 1));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        var result = scheduleService.getSchedulesForStatistics(null, null, null, "INTERVIEW");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findByMeetingType(eq("INTERVIEW"), any(PageRequest.class));
    }

    @Test
    // Test Case ID: TC-SS-048 - statistics should map null meeting type to null.
    @DisplayName("TC-SS-048 - getSchedulesForStatistics() maps null meetingType to null in DTO")
    void tc_ss_048_getSchedulesForStatistics_whenScheduleMeetingTypeNull_shouldReturnNullMeetingType() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Giả lập Dữ liệu legacy hoặc lỗi DB dẫn tới meetingType = null.
        Schedule schedule = buildSchedule(351L, "No meeting type");
        schedule.setMeetingType(null);
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(List.of(schedule));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Trình Mapper không được Crash khi enum null.
        assertEquals(1, result.size());
        assertEquals(null, result.get(0).getMeetingType(), "Lỗi: Mapper không xử lý được null Enum");
        // CheckDB: Xác minh cơ sở dữ liệu có truy cập đúng theo yêu cầu hay không.
        verify(scheduleRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    // Test Case ID: TC-SS-049 - getScheduleWithParticipantNames() remote success with null body branch.
    @DisplayName("TC-SS-049 - getScheduleWithParticipantNames() uses Unknown when user-service body is null")
    void tc_ss_049_getScheduleWithParticipantNames_whenUserServiceBodyIsNull_shouldUseUnknownName() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        Schedule schedule = buildSchedule(352L, "Null body names");
        ScheduleParticipant userParticipant = new ScheduleParticipant();
        userParticipant.setParticipantType("USER");
        userParticipant.setParticipantId(3520L);
        userParticipant.setResponseStatus("PENDING");
        userParticipant.setSchedule(schedule);
        schedule.getParticipants().add(userParticipant);

        when(scheduleRepository.findById(352L)).thenReturn(Optional.of(schedule));
        // Giả lập API trả HTTP 200 OK nhưng Body bị null (trường hợp cực đoan mạng).
        when(userService.getEmployeeNames(List.of(3520L), "token")).thenReturn(ResponseEntity.ok(null));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        ScheduleDetailDTO result = scheduleService.getScheduleWithParticipantNames(352L, "token");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.getParticipants().size());
        assertEquals("Unknown", result.getParticipants().get(0).getName(), "Bắt buộc fallback Unknown khi Response Body rỗng");
        
        // Xác minh ngoại vi truy cập đúng service theo yêu cầu.
        verify(scheduleRepository).findById(352L);
        verify(userService).getEmployeeNames(List.of(3520L), "token");
        verify(candidateService, never()).getCandidateNames(anyList(), anyString());
    }

    @Test
    // Test Case ID: TC-SS-050 - getAvailableParticipants() remote name response false branch.
    @DisplayName("TC-SS-050 - getAvailableParticipants() uses Unknown when name-service body is null")
    void tc_ss_050_getAvailableParticipants_whenNameServiceBodyIsNull_shouldReturnUnknownFields() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 10, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 10, 10, 0);
        when(userService.getAllEmployeeIds("token")).thenReturn(List.of(5001L));
        when(scheduleRepository.findOverlappingSchedules(startTime, endTime, null)).thenReturn(List.of());
        when(userService.getEmployeeNamesAndDepartmentNames(List.of(5001L), "token"))
                .thenReturn(ResponseEntity.ok(null));

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        List<AvailableParticipantDTO> result = scheduleService.getAvailableParticipants(startTime, endTime, null, "token");

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        assertEquals(1, result.size());
        assertEquals(5001L, result.get(0).getId());
        assertEquals("Unknown", result.get(0).getName());
        assertEquals("Unknown", result.get(0).getDepartmentName());
        
        // Xác minh cơ sở dữ liệu: verify DB method được gọi chính xác tham số thời gian.
        verify(scheduleRepository).findOverlappingSchedules(startTime, endTime, null);
        verify(userService).getEmployeeNamesAndDepartmentNames(List.of(5001L), "token");
    }

    @Test
    // Test Case ID: TC-SS-051 - statistics all branch should truncate records over 10000.
    @DisplayName("TC-SS-051 - getSchedulesForStatistics() truncates all schedules to first 10000 records")
    void tc_ss_051_getSchedulesForStatistics_whenAllSchedulesExceedLimit_shouldReturnFirst10000Only() {
        
        // Arrange: Thiết lập giả lập Database (Mocking)
        // Tạo khối lượng dữ liệu vượt ngưỡng tải an toàn (>10000 records) để thử nghiệm giới hạn bộ nhớ (Memory Limit).
        List<Schedule> schedules = new java.util.ArrayList<>();
        for (long id = 1; id <= 10001; id++) {
            schedules.add(buildSchedule(id, "Schedule " + id));
        }
        when(scheduleRepository.findAll(any(Sort.class))).thenReturn(schedules);

        // Act: Thực thi phương thức cần kiểm thử (Test Execution)
        var result = scheduleService.getSchedulesForStatistics(null, null, null, null);

        // Assert: Xác minh tính toàn vẹn của dữ liệu trả về (Data Integrity)
        // Đảm bảo tầng Service đã bảo vệ hệ thống bằng cách drop phần tử thứ 10001.
        assertEquals(10000, result.size(), "Lỗi: Rò rỉ dữ liệu, giới hạn API Report không được vượt quá 10000 dòng");
        assertEquals(1L, result.get(0).getId());
        assertEquals(10000L, result.get(9999).getId());
        /*
        verify: Xác thực phương thức được kích hoạt đúng 1 lần (times(1)) trên đối tượng Mock (scheduleRepository).
        findAll: Phương thức truy xuất toàn bộ bản ghi (Fetch All Records) từ Database.
        any(Sort.class): Trình khớp tham số (Argument Matcher), chấp nhận mọi cơ chế sắp xếp (Sorting Criteria).
        Mục đích kỹ thuật: kiểm tra xem code thực tế có thực sự mò xuống DB để lấy dữ liệu lên và có sắp xếp hay không.
         */
        verify(scheduleRepository).findAll(any(Sort.class));
    }
}
