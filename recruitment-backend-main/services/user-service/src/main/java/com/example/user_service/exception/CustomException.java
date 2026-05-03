package com.example.user_service.exception;

//Exception dùng chung
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
