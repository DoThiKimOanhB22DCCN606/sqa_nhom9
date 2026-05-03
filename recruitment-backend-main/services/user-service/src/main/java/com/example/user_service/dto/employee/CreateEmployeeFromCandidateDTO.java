package com.example.user_service.dto.employee;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeFromCandidateDTO {
    /**
     * Candidate ID (bắt buộc)
     */
    @NotNull(message = "Candidate ID là bắt buộc")
    private Long candidateId;

    /**
     * Thông tin candidate từ candidate-service
     */
    private String name;
    private String email;
    private String phone;
    private String dateOfBirth; // String format từ candidate
    private String gender;
    private String nationality;
    private String idNumber;
    private String address;
    private String avatarUrl;

    /**
     * Thông tin bổ sung cần thiết để tạo employee
     */
    @NotNull(message = "Phòng ban là bắt buộc")
    private Long departmentId;

    @NotNull(message = "Vị trí là bắt buộc")
    private Long positionId;

    /**
     * Trạng thái nhân viên (mặc định là PROBATION khi chuyển từ candidate)
     */
    private String status; // Mặc định "PROBATION"
}
