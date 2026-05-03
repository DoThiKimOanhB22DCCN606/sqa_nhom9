package com.example.user_service.dto.employee;

import java.time.LocalDate;

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
public class CreateEmployeeDTO {
    @NotBlank(message = "Tên không được để trống")
    private String name;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    @NotBlank(message = "Email không được để trống")
    private String email;
    @NotBlank(message = "Giới tính không được để trống")
    private String gender;
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;
    @NotBlank(message = "Quốc tịch không được để trống")
    private String nationality;
    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;
    @NotBlank(message = "Số CMND không được để trống")
    private String idNumber;
    @NotNull(message = "Phòng ban không được để trống")
    private Long departmentId;
    @NotNull(message = "Vị trí không được để trống")
    private Long positionId;
    private String status;
}
