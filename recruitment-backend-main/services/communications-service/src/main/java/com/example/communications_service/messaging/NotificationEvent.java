package com.example.communications_service.messaging;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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
