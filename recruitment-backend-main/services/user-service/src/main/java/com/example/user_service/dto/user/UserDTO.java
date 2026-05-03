package com.example.user_service.dto.user;

import java.time.LocalDateTime;

import com.example.user_service.model.Employee;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String email;
    private String password;
    private boolean isActive;
    private String createBy;
    private String updateBy;
    private String refreshToken;
    private Long roleId;
    private Employee employee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
