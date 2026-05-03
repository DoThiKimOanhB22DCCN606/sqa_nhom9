package com.example.schedule_service.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableParticipantDTO {
    private Long id;
    private String name;
    private String departmentName;
}
