package com.example.candidate_service.dto.candidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadCVDTO {
    @NotBlank(message = "Họ tên không được để trống")
    private String name;
    @NotBlank(message = "Email không được để trống")
    private String email;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    @NotNull(message = "Vị trí không được để trống")
    private Long jobPositionId;
    @NotBlank(message = "CV URL không được để trống")
    private String cvUrl;
    private String notes;
}
