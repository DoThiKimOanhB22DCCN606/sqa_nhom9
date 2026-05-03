package com.example.user_service.dto.department;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDepartmentDTO {
    @Size(max = 50, message = "Mã phòng ban không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "Mã phòng ban chỉ được chứa chữ in hoa, số và dấu gạch dưới")
    private String code;

    private String name;

    private String description;

    private Boolean isActive;
}
