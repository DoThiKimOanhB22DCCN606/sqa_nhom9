package com.example.user_service.dto.login;

import com.example.user_service.model.Department;
import com.example.user_service.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class ResponseLoginDTO {
    @JsonProperty("access_token")
    private String accessToken;
    private UserLogin user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLogin {
        private long userId;
        private long employeeId;
        private String email;
        private String name; // Lấy từ Employee
        private Role role;
        private Department department;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserToken {
        private long userId; // Lưu userId vào token
        private long employeeId; // Lưu employeeId vào token
        private String email;
        private String name;
        private String role;
        private Long departmentId;
        private String departmentCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGetAccount {
        private UserLogin user;
    }
}
