package com.example.job_service.dto.user;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String department;
    private String position;
    private String role;
    private Boolean active;
}
