package com.example.workflow_service.exception;

//ban dau extends Exception
public class IdInvalidException extends RuntimeException {
    public IdInvalidException(String message) {
        super(message);
    }
}
