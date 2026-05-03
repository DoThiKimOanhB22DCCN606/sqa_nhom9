package com.example.user_service.dto.position;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePositionDTO {
    private String name;
    private String level;
    private Integer hierarchyOrder;
    private Boolean isActive;
}
