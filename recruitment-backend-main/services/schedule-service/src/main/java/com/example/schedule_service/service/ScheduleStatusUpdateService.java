package com.example.schedule_service.service;

import com.example.schedule_service.messaging.NotificationProducer;
import com.example.schedule_service.model.Schedule;
import com.example.schedule_service.model.ScheduleParticipant;
import com.example.schedule_service.repository.ScheduleRepository;
import com.example.schedule_service.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service tự động cập nhật status của schedule dựa trên thời gian
 * và gửi reminder notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleStatusUpdateService {

    private final ScheduleRepository scheduleRepository;
    private final NotificationProducer notificationProducer;

    /**
     * Tự động cập nhật status của schedules
     * Chạy mỗi phút (60000 milliseconds)
     * 
     * Logic:
     * - SCHEDULED -> IN_PROGRESS: Khi startTime <= hiện tại
     * - IN_PROGRESS -> DONE: Khi endTime <= hiện tại
     */
    @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút
    @Transactional
    public void updateScheduleStatuses() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Đang kiểm tra và cập nhật status schedules tại: {}", now);

        // // 1. Cập nhật SCHEDULED -> IN_PROGRESS
        // List<Schedule> schedulesToStart =
        // scheduleRepository.findSchedulesToStart(now);
        // if (!schedulesToStart.isEmpty()) {
        // log.info("Tìm thấy {} schedule(s) cần chuyển từ SCHEDULED sang IN_PROGRESS",
        // schedulesToStart.size());
        // for (Schedule schedule : schedulesToStart) {
        // schedule.setStatus("IN_PROGRESS");
        // log.debug("Đã cập nhật schedule ID {} từ SCHEDULED sang IN_PROGRESS",
        // schedule.getId());
        // }
        // scheduleRepository.saveAll(schedulesToStart);
        // }

        // 2. Cập nhật IN_PROGRESS -> DONE
        List<Schedule> schedulesToComplete = scheduleRepository.findSchedulesToComplete(now);
        if (!schedulesToComplete.isEmpty()) {
            log.info("Tìm thấy {} schedule(s) cần chuyển từ SCHEDULED sang DONE", schedulesToComplete.size());
            for (Schedule schedule : schedulesToComplete) {
                schedule.setStatus("DONE");
                log.debug("Đã cập nhật schedule ID {} từ SCHEDULED sang DONE", schedule.getId());
            }
            scheduleRepository.saveAll(schedulesToComplete);
        }

        if (schedulesToComplete.isEmpty()) {
            log.debug("Không có schedule nào cần cập nhật status");
        }
    }

    /**
     * Tự động gửi reminder notifications cho schedules
     * Chạy mỗi phút (60000 milliseconds)
     * 
     * Logic:
     * - Tìm schedules có status = 'SCHEDULED', có reminderTime, chưa gửi reminder
     * - Kiểm tra xem đã đến thời điểm reminder chưa (startTime - reminderTime <=
     * now)
     * - Gửi notification cho tất cả participants (chỉ USER type)
     * - Đánh dấu reminderSent = true
     */
    @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút
    @Transactional
    public void sendReminderNotifications() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Đang kiểm tra và gửi reminder notifications tại: {}", now);

        // Tìm các schedules có thể cần gửi reminder
        List<Schedule> potentialReminders = scheduleRepository.findSchedulesForReminder(now);

        if (potentialReminders.isEmpty()) {
            log.debug("Không có schedule nào cần gửi reminder");
            return;
        }

        List<Schedule> schedulesToRemind = new ArrayList<>();
        for (Schedule schedule : potentialReminders) {
            if (schedule.getReminderTime() == null || schedule.getStartTime() == null) {
                continue;
            }

            // Tính thời điểm reminder: startTime - reminderTime (phút)
            LocalDateTime reminderTime = schedule.getStartTime().minusMinutes(schedule.getReminderTime());

            // Kiểm tra xem đã đến thời điểm reminder chưa (trong khoảng ±1 phút để tránh bỏ
            // sót)
            long minutesUntilReminder = ChronoUnit.MINUTES.between(now, reminderTime);

            // Gửi reminder nếu đã đến thời điểm (trong khoảng -1 đến +1 phút)
            if (minutesUntilReminder >= -1 && minutesUntilReminder <= 1) {
                schedulesToRemind.add(schedule);
            }
        }

        if (schedulesToRemind.isEmpty()) {
            log.debug("Không có schedule nào đến thời điểm reminder");
            return;
        }

        log.info("Tìm thấy {} schedule(s) cần gửi reminder", schedulesToRemind.size());

        // Lấy token để gửi notification (có thể null nếu không có user context)
        String token = SecurityUtil.getCurrentUserJWT().orElse(null);

        for (Schedule schedule : schedulesToRemind) {
            try {
                // Lấy danh sách participant IDs (chỉ USER type)
                List<Long> participantIds = new ArrayList<>();
                if (schedule.getParticipants() != null) {
                    for (ScheduleParticipant participant : schedule.getParticipants()) {
                        if ("USER".equalsIgnoreCase(participant.getParticipantType())
                                && participant.getParticipantId() != null) {
                            participantIds.add(participant.getParticipantId());
                        }
                    }
                }

                if (participantIds.isEmpty()) {
                    log.debug("Schedule ID {} không có participants để gửi reminder", schedule.getId());
                    schedule.setReminderSent(true);
                    continue;
                }

                // Tạo message reminder
                String title = "Nhắc nhở lịch hẹn";
                StringBuilder message = new StringBuilder();
                message.append("Bạn có lịch hẹn: ");

                if (schedule.getTitle() != null) {
                    message.append(schedule.getTitle());
                }

                if (schedule.getStartTime() != null) {
                    message.append(" vào ").append(schedule.getStartTime());
                }

                if (schedule.getLocation() != null) {
                    message.append(" tại ").append(schedule.getLocation());
                }

                if (schedule.getReminderTime() != null) {
                    message.append(" (còn ").append(schedule.getReminderTime()).append(" phút nữa)");
                }

                // Gửi notification
                notificationProducer.sendNotificationToMultiple(
                        participantIds,
                        title,
                        message.toString(),
                        token);

                // Đánh dấu đã gửi reminder
                schedule.setReminderSent(true);

                log.info("Đã gửi reminder cho schedule ID {} đến {} participants",
                        schedule.getId(), participantIds.size());

            } catch (Exception e) {
                log.error("Lỗi khi gửi reminder cho schedule ID {}: {}", schedule.getId(), e.getMessage(), e);
            }
        }

        // Lưu tất cả schedules đã gửi reminder
        scheduleRepository.saveAll(schedulesToRemind);
    }
}
