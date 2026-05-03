package com.example.communications_service.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleParticipantDTO {
    private Long id;
    private String participantType;
    private String responseStatus;
    private Long participantId;
    private String name;
}
