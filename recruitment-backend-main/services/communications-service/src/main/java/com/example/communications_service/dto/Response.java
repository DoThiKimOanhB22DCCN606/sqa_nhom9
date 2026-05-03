package com.example.communications_service.dto;

import lombok.*;

@Getter
@Setter
public class Response<T> {
    private int statusCode;
    private String error;
    private Object message;
    private T data;

}
