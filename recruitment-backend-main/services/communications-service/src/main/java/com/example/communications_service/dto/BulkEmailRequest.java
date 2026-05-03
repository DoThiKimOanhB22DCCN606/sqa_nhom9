package com.example.communications_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BulkEmailRequest {

    @NotEmpty(message = "Recipients list cannot be empty")
    private List<String> recipients;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message is required")
    private String message;

    private String templateName;

    private Map<String, Object> variables;

    private String priority = "NORMAL";
}
