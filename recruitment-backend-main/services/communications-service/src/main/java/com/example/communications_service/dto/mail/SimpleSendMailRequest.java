package com.example.communications_service.dto.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleSendMailRequest {
    private String toEmail;
    private String subject;
    private String content;
}

