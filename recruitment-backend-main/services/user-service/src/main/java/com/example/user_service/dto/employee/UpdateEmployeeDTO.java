package com.example.user_service.dto.employee;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeDTO {
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String address;
    private String nationality;
    private LocalDate dateOfBirth;
    private String idNumber;
    private Long departmentId;
    private Long positionId;
    private String status;
}
