package com.example.communications_service.repository;

import com.example.communications_service.model.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Hai schedules trùng lịch nếu: newStart < existingEnd AND newEnd > existingStart
     * Loại trừ schedule có id = excludeScheduleId (dùng khi update)
     */
    @Query("SELECT s FROM Schedule s WHERE s.status != 'CANCELLED' " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeScheduleId IS NULL OR s.id != :excludeScheduleId)")
    List<Schedule> findOverlappingSchedules(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("excludeScheduleId") Long excludeScheduleId);
}
