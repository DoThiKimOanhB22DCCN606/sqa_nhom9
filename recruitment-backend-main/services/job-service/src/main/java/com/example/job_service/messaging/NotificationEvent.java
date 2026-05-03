package com.example.job_service.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long recipientId;
    private List<Long> recipientIds;
    private Long departmentId;
    private Long positionId;
    private Boolean includeAllEmployees;
    private String title;
    private String message;
    private String authToken;
}
