package com.example.communications_service.repository;

import com.example.communications_service.model.ScheduleParticipant;
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
}

