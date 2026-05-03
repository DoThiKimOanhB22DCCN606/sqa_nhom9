package com.example.schedule_service.repository;

import com.example.schedule_service.model.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {

        @Query("SELECT DISTINCT sp.participantId FROM ScheduleParticipant sp " +
                        "WHERE sp.participantType = 'USER' " +
                        "AND sp.schedule.id IN :scheduleIds")
        List<Long> findParticipantIdsByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

        /**
         * Lấy danh sách candidateIds từ các schedules mà một employee đã tham gia phỏng
         * vấn
         * Tìm các schedules có:
         * - meetingType = INTERVIEW
         * - có participant với participantType = USER và participantId = employeeId
         * Sau đó lấy tất cả các participant có participantType = CANDIDATE từ các
         * schedules đó
         */
        @Query("SELECT DISTINCT sp2.participantId FROM ScheduleParticipant sp1 " +
                        "JOIN ScheduleParticipant sp2 ON sp1.schedule.id = sp2.schedule.id " +
                        "WHERE sp1.participantType = 'USER' " +
                        "AND sp1.participantId = :employeeId " +
                        "AND sp2.participantType = 'CANDIDATE' " +
                        "AND sp1.schedule.meetingType = 'INTERVIEW'")
        List<Long> findCandidateIdsByInterviewer(@Param("employeeId") Long employeeId);
}
