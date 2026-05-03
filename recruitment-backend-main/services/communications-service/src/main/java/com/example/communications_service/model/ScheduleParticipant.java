package com.example.communications_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_participant")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_type")
    private String participantType; // (e.g., "USER", "CANDIDATE")

    @Column(name = "response_status")
    private String responseStatus; // [cite: 70, 71] (e.g., "ACCEPTED", "DECLINED", "PENDING")

    @Column(name = "participant_id")
    private Long participantId; // ID của Employee (khi participantType = "USER") hoặc Candidate (khi
                                // participantType = "CANDIDATE")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @JsonIgnore
    private Schedule schedule;
}