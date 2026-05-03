package com.example.schedule_service.repository;

import com.example.schedule_service.model.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

       Page<Schedule> findByStatus(String status, Pageable pageable);

       Page<Schedule> findByMeetingType(String meetingType, Pageable pageable);

       Page<Schedule> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

       List<Schedule> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Sort sort);

       List<Schedule> findAll(Sort sort);

       /**
        * Tìm các schedules trùng lịch với khoảng thời gian đã cho
        * Hai schedules trùng lịch nếu: newStart < existingEnd AND newEnd >
        * existingStart
        * Loại trừ schedule có id = excludeScheduleId (dùng khi update)
        * Loại trừ các lịch đã CANCELLED hoặc DONE (đã hoàn thành)
        */
       @Query("SELECT s FROM Schedule s WHERE s.status != 'CANCELLED' " +
                     "AND s.status != 'DONE' " +
                     "AND s.startTime < :endTime AND s.endTime > :startTime " +
                     "AND (:excludeScheduleId IS NULL OR s.id != :excludeScheduleId)")
       List<Schedule> findOverlappingSchedules(@Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime,
                     @Param("excludeScheduleId") Long excludeScheduleId);

       /**
        * Tìm các schedules có status = 'SCHEDULED' và đã đến giờ bắt đầu
        */
       @Query("SELECT s FROM Schedule s WHERE s.status = 'SCHEDULED' " +
                     "AND s.startTime <= :currentTime")
       List<Schedule> findSchedulesToStart(@Param("currentTime") LocalDateTime currentTime);

       /**
        * Tìm các schedules có status = 'IN_PROGRESS' và đã đến giờ kết thúc
        */
       @Query("SELECT s FROM Schedule s WHERE s.status = 'SCHEDULED' " +
                     "AND s.endTime <= :currentTime")
       List<Schedule> findSchedulesToComplete(@Param("currentTime") LocalDateTime currentTime);

       /**
        * Tìm các schedules cần gửi reminder
        * Điều kiện:
        * - status = 'SCHEDULED' (chưa bắt đầu)
        * - reminderTime không null
        * - reminderSent = false (chưa gửi reminder)
        * - startTime > currentTime (chưa bắt đầu)
        * 
        * Note: Logic tính toán reminder time sẽ được xử lý trong service
        */
       @Query("SELECT s FROM Schedule s WHERE s.status = 'SCHEDULED' " +
                     "AND s.reminderTime IS NOT NULL " +
                     "AND (s.reminderSent IS NULL OR s.reminderSent = false) " +
                     "AND s.startTime > :currentTime")
       List<Schedule> findSchedulesForReminder(@Param("currentTime") LocalDateTime currentTime);
}
