package com.example.user_service.dto.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePositionDTO {
    @NotBlank(message = "Tên không được để trống")
    private String name;
    @NotBlank(message = "Cấp bậc không được để trống")
    private String level;
    @NotNull(message = "Thứ tự cấp bậc không được để trống")
    private Integer hierarchyOrder;

    private Boolean isActive;
}
