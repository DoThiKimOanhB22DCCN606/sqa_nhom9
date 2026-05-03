package com.example.workflow_service.exception;

//Exception dùng chung
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
